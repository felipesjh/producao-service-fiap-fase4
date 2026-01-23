package br.com.fiap.fastfood.producao.application;

import br.com.fiap.fastfood.producao.domain.ProducaoPedido;
import br.com.fiap.fastfood.producao.domain.StatusProducao;
import br.com.fiap.fastfood.producao.infra.ProducaoPedidoRepository;
import br.com.fiap.fastfood.producao.integration.PedidoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "pedido.service.base-url=http://localhost:9999"
})
class ProducaoServiceBDDTest {

    @Autowired
    private ProducaoService producaoService;

    @Autowired
    private ProducaoPedidoRepository repository;

    @BeforeEach
    void limparColecao() {
        // evita erro de "non unique result" por dados antigos no Mongo
        repository.deleteAll();
    }

    @Test
    void dadoPedidoRecebido_quandoIniciar_entaoStatusDeveVirarEmPreparacao() {

        // GIVEN
        // usa um ID "quase impossível" de colidir
        Long pedidoId = Math.abs(UUID.randomUUID().getMostSignificantBits());

        ProducaoPedido pedido = new ProducaoPedido(pedidoId);
        repository.save(pedido);

        // WHEN
        ProducaoPedido atualizado = producaoService.iniciar(pedidoId);

        // THEN
        assertThat(atualizado.getStatusAtual()).isEqualTo(StatusProducao.EM_PREPARACAO);

        ProducaoPedido doBanco = repository.findByPedidoId(pedidoId).orElseThrow();
        assertThat(doBanco.getStatusAtual()).isEqualTo(StatusProducao.EM_PREPARACAO);
    }

    @TestConfiguration
    static class StubConfig {

        @Bean
        @Primary
        public PedidoClient pedidoClientStub() {
            return new PedidoClient("http://localhost:9999") {
                @Override
                public void atualizarStatusPedido(Long pedidoId, String status) {
                    // NO-OP
                }
            };
        }
    }
}
