package br.com.fiap.fastfood.producao.api;

import br.com.fiap.fastfood.producao.application.ProducaoService;
import br.com.fiap.fastfood.producao.domain.ProducaoPedido;
import br.com.fiap.fastfood.producao.domain.StatusProducao;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProducaoController.class)
class ProducaoControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProducaoService service;

    @Test
    void iniciar_deveRetornar201() throws Exception {
        Long pedidoId = 1L;

        // Se o seu ProducaoPedido tiver esse construtor, ok (você já usou no outro teste)
        ProducaoPedido retorno = new ProducaoPedido(pedidoId);

        when(service.iniciar(pedidoId)).thenReturn(retorno);

        mockMvc.perform(post("/producao/{pedidoId}/iniciar", pedidoId))
                .andExpect(status().isCreated());

        verify(service, times(1)).iniciar(pedidoId);
        verifyNoMoreInteractions(service);
    }

    @Test
    void atualizarStatus_quandoObservacaoVazia_deveUsarPadrao() throws Exception {
        Long pedidoId = 10L;

        ProducaoPedido retorno = new ProducaoPedido(pedidoId);
        when(service.atualizarStatus(eq(pedidoId), eq(StatusProducao.PRONTO), anyString()))
                .thenReturn(retorno);

        String body = """
                {
                  "status": "PRONTO",
                  "observacao": "   "
                }
                """;

        mockMvc.perform(put("/producao/{pedidoId}/status", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<String> obsCaptor = ArgumentCaptor.forClass(String.class);
        verify(service, times(1)).atualizarStatus(eq(pedidoId), eq(StatusProducao.PRONTO), obsCaptor.capture());

        // regra do controller: quando observacao null/vazia -> "Atualização de status"
        assertThat(obsCaptor.getValue()).isEqualTo("Atualização de status");
        verifyNoMoreInteractions(service);
    }

    @Test
    void atualizarStatus_quandoObservacaoInformada_deveRepassar() throws Exception {
        Long pedidoId = 11L;

        ProducaoPedido retorno = new ProducaoPedido(pedidoId);
        when(service.atualizarStatus(eq(pedidoId), eq(StatusProducao.EM_PREPARACAO), anyString()))
                .thenReturn(retorno);

        String body = """
                {
                  "status": "EM_PREPARACAO",
                  "observacao": "Produção iniciada"
                }
                """;

        mockMvc.perform(put("/producao/{pedidoId}/status", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(service, times(1))
                .atualizarStatus(pedidoId, StatusProducao.EM_PREPARACAO, "Produção iniciada");

        verifyNoMoreInteractions(service);
    }

    @Test
    void consultar_deveRetornar200() throws Exception {
        Long pedidoId = 99L;

        ProducaoPedido retorno = new ProducaoPedido(pedidoId);
        when(service.consultar(pedidoId)).thenReturn(retorno);

        mockMvc.perform(get("/producao/{pedidoId}", pedidoId))
                .andExpect(status().isOk());

        verify(service, times(1)).consultar(pedidoId);
        verifyNoMoreInteractions(service);
    }
}
