package br.com.fiap.fastfood.producao;

import br.com.fiap.fastfood.producao.api.ProducaoController;
import br.com.fiap.fastfood.producao.application.ProducaoService;
import br.com.fiap.fastfood.producao.domain.EventoProducao;
import br.com.fiap.fastfood.producao.domain.ProducaoPedido;
import br.com.fiap.fastfood.producao.domain.StatusProducao;
import br.com.fiap.fastfood.producao.infra.ProducaoPedidoRepository;
import br.com.fiap.fastfood.producao.integration.PedidoClient;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ProducaoUnitTest {

    // =======================
    // HTTP SERVER REAL (JDK)
    // =======================
    private static HttpServer pedidoServer;
    private static String baseUrl;

    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();

    private static void handleRequest(HttpExchange exchange) throws IOException {
        lastMethod.set(exchange.getRequestMethod());
        lastPath.set(exchange.getRequestURI().getPath());
        lastBody.set(readBody(exchange.getRequestBody()));

        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private static String readBody(InputStream is) throws IOException {
        byte[] bytes = is.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @BeforeAll
    static void setupAll() throws Exception {
        pedidoServer = HttpServer.create(new InetSocketAddress(0), 0); // porta aleatória
        pedidoServer.createContext("/", ProducaoUnitTest::handleRequest);
        pedidoServer.setExecutor(Executors.newSingleThreadExecutor());
        pedidoServer.start();

        int port = pedidoServer.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    static void tearDownAll() {
        if (pedidoServer != null) pedidoServer.stop(0);
    }

    @BeforeEach
    void resetCaptures() {
        lastMethod.set(null);
        lastPath.set(null);
        lastBody.set(null);
    }

    // ==========================================
    // REPOSITÓRIO EM MEMÓRIA VIA PROXY (SEM MONGO)
    // ==========================================
    static class RepoStore {
        final Map<Long, ProducaoPedido> byPedidoId = new HashMap<>();
    }

    private static ProducaoPedidoRepository newRepoProxy(RepoStore store) {
        return (ProducaoPedidoRepository) Proxy.newProxyInstance(
                ProducaoPedidoRepository.class.getClassLoader(),
                new Class<?>[]{ProducaoPedidoRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();

                    // Métodos realmente usados pelo ProducaoService:
                    if (name.equals("findByPedidoId")) {
                        Long pedidoId = (Long) args[0];
                        return Optional.ofNullable(store.byPedidoId.get(pedidoId));
                    }

                    if (name.equals("save")) {
                        ProducaoPedido entity = (ProducaoPedido) args[0];
                        store.byPedidoId.put(entity.getPedidoId(), entity);
                        return entity;
                    }

                    // Alguns runtimes chamam isso:
                    if (name.equals("toString")) return "RepoProxy";
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];

                    // Qualquer outro método do MongoRepository NÃO deve ser usado no teste
                    throw new UnsupportedOperationException("Método não suportado no teste: " + method);
                }
        );
    }

    // =================
    // TESTES DO SERVICE
    // =================
    @Test
    void iniciar_deveCriarOuReusarProducao_eEnviarStatusEmPreparacaoParaPedidoService() {
        RepoStore store = new RepoStore();
        ProducaoPedidoRepository repo = newRepoProxy(store);

        PedidoClient pedidoClient = new PedidoClient(baseUrl);
        ProducaoService service = new ProducaoService(repo, pedidoClient);

        Long pedidoId = 10L;

        ProducaoPedido result = service.iniciar(pedidoId);

        assertNotNull(result);
        assertEquals(pedidoId, result.getPedidoId());
        assertEquals(StatusProducao.EM_PREPARACAO, result.getStatusAtual());
        assertTrue(result.getHistorico().size() >= 2); // RECEBIDO + EM_PREPARACAO

        // Confirma chamada HTTP feita pelo PedidoClient
        assertEquals("PUT", lastMethod.get());
        assertEquals("/pedidos/" + pedidoId + "/status", lastPath.get());

        // body é JSON do record AtualizarStatusPedidoRequest(status)
        assertNotNull(lastBody.get());
        assertTrue(lastBody.get().contains("EM_PREPARACAO"));
    }

    @Test
    void atualizarStatus_quandoNaoExiste_deveLancarExcecao() {
        RepoStore store = new RepoStore();
        ProducaoPedidoRepository repo = newRepoProxy(store);

        PedidoClient pedidoClient = new PedidoClient(baseUrl);
        ProducaoService service = new ProducaoService(repo, pedidoClient);

        Long pedidoId = 99L;

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.atualizarStatus(pedidoId, StatusProducao.EM_PREPARACAO, "x")
        );

        assertTrue(ex.getMessage().contains("Use /iniciar primeiro"));
    }

    @Test
    void atualizarStatus_paraPronto_deveSalvarEChamarPedidoServiceComPRONTO() {
        RepoStore store = new RepoStore();
        ProducaoPedidoRepository repo = newRepoProxy(store);

        PedidoClient pedidoClient = new PedidoClient(baseUrl);
        ProducaoService service = new ProducaoService(repo, pedidoClient);

        Long pedidoId = 20L;

        // primeiro inicia (vai para EM_PREPARACAO e chama pedido-service)
        service.iniciar(pedidoId);

        // zera captura pra focar na chamada do PRONTO
        resetCaptures();

        ProducaoPedido pronto = service.atualizarStatus(pedidoId, StatusProducao.PRONTO, "finalizado");

        assertEquals(StatusProducao.PRONTO, pronto.getStatusAtual());
        assertTrue(pronto.getHistorico().stream().anyMatch(e -> e.getStatus() == StatusProducao.PRONTO));

        // Confirma que avisou pedido-service com PRONTO
        assertEquals("PUT", lastMethod.get());
        assertEquals("/pedidos/" + pedidoId + "/status", lastPath.get());
        assertNotNull(lastBody.get());
        assertTrue(lastBody.get().contains("PRONTO"));
    }

    @Test
    void consultar_quandoNaoExiste_deveLancarExcecao() {
        RepoStore store = new RepoStore();
        ProducaoPedidoRepository repo = newRepoProxy(store);

        PedidoClient pedidoClient = new PedidoClient(baseUrl);
        ProducaoService service = new ProducaoService(repo, pedidoClient);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.consultar(1L)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("não encontrado")
                || ex.getMessage().toLowerCase().contains("nao encontrado"));
    }

    // =====================
    // TESTE DO CONTROLLER (SEM WEB)
    // =====================
    @Test
    void controller_deveDefinirObservacaoPadrao_quandoVazia() throws Exception {
        RepoStore store = new RepoStore();
        ProducaoPedidoRepository repo = newRepoProxy(store);

        PedidoClient pedidoClient = new PedidoClient(baseUrl);
        ProducaoService service = new ProducaoService(repo, pedidoClient);
        ProducaoController controller = new ProducaoController(service);

        Long pedidoId = 30L;
        controller.iniciar(pedidoId);

        // zera captura (pra não confundir com a chamada de iniciar)
        resetCaptures();

        // Instancia o DTO real via reflection: AtualizarStatusProducaoRequest(StatusProducao status, String observacao)
        Class<?> reqClazz = Class.forName("br.com.fiap.fastfood.producao.api.dto.AtualizarStatusProducaoRequest");
        assertTrue(reqClazz.isRecord());

        Constructor<?> ctor = reqClazz.getDeclaredConstructors()[0];
        Object reqObj = ctor.newInstance(StatusProducao.EM_PREPARACAO, "   "); // blank => vira padrão

        // chama o controller com cast seguro
        @SuppressWarnings("unchecked")
        var reqTyped = (br.com.fiap.fastfood.producao.api.dto.AtualizarStatusProducaoRequest) reqObj;

        ProducaoPedido atualizado = controller.atualizarStatus(pedidoId, reqTyped);

        assertEquals(StatusProducao.EM_PREPARACAO, atualizado.getStatusAtual());

        boolean temObsPadrao = atualizado.getHistorico().stream()
                .anyMatch(e -> e.getStatus() == StatusProducao.EM_PREPARACAO
                        && "Atualização de status".equals(e.getObservacao()));

        assertTrue(temObsPadrao, "Esperava observação padrão 'Atualização de status' no histórico");
    }

    // =====================
    // TESTES DO DOMAIN (GET/SET E REGRAS)
    // =====================
    @Test
    void domain_producaoPedido_deveIniciarComRecebido_eHistorico() {
        Long pedidoId = 777L;
        ProducaoPedido p = new ProducaoPedido(pedidoId);

        assertEquals(pedidoId, p.getPedidoId());
        assertEquals(StatusProducao.RECEBIDO, p.getStatusAtual());
        assertNotNull(p.getCriadoEm());
        assertNotNull(p.getAtualizadoEm());
        assertNotNull(p.getHistorico());
        assertFalse(p.getHistorico().isEmpty());
        assertEquals(StatusProducao.RECEBIDO, p.getHistorico().get(0).getStatus());
    }

    @Test
    void domain_producaoPedido_atualizarStatus_deveAdicionarEventoNoHistorico() {
        ProducaoPedido p = new ProducaoPedido(1L);

        int before = p.getHistorico().size();
        p.atualizarStatus(StatusProducao.EM_PREPARACAO, "teste");

        assertEquals(StatusProducao.EM_PREPARACAO, p.getStatusAtual());
        assertTrue(p.getHistorico().size() == before + 1);
        assertEquals(StatusProducao.EM_PREPARACAO, p.getHistorico().get(p.getHistorico().size() - 1).getStatus());
        assertEquals("teste", p.getHistorico().get(p.getHistorico().size() - 1).getObservacao());
    }

    @Test
    void domain_eventoProducao_gettersSetters() {
        EventoProducao ev = new EventoProducao();

        LocalDateTime now = LocalDateTime.now();
        ev.setStatus(StatusProducao.EM_PREPARACAO);
        ev.setObservacao("ok");
        ev.setDataHora(now);

        assertEquals(StatusProducao.EM_PREPARACAO, ev.getStatus());
        assertEquals("ok", ev.getObservacao());
        assertEquals(now, ev.getDataHora());
    }

    // =====================
    // VALIDAÇÃO DO DTO (SEM SPRING) - OPCIONAL
    // Se falhar porque seu DTO não tem @NotNull, pode apagar este teste.
    // =====================
    @Test
    void validarDTO_deAtualizarStatusProducaoRequest_semSpring() throws Exception {
        Class<?> reqClazz = Class.forName("br.com.fiap.fastfood.producao.api.dto.AtualizarStatusProducaoRequest");
        assertTrue(reqClazz.isRecord());

        Object req = reqClazz.getDeclaredConstructors()[0].newInstance(null, "x"); // status nulo

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator v = factory.getValidator();

        @SuppressWarnings("unchecked")
        Set<ConstraintViolation<Object>> violations = (Set) v.validate(req);

        // Se não tiver validação, esse teste pode falhar.
        assertFalse(violations.isEmpty());
    }
}
