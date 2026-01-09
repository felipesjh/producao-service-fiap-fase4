package br.com.fiap.fastfood.producao.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PedidoClient {

    private final RestClient restClient;

    public PedidoClient(@Value("${pedido.service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void atualizarStatusPedido(Long pedidoId, String status) {
        // Ajuste para o formato que seu pedido-service espera.
        // Aqui assume: PUT /pedidos/{id}/status com body: {"status":"EM_PRODUCAO"}
        restClient.put()
                .uri("/pedidos/{id}/status", pedidoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AtualizarStatusPedidoRequest(status))
                .retrieve()
                .toBodilessEntity();
    }

    private record AtualizarStatusPedidoRequest(String status) {}
}
