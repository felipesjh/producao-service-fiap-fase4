package br.com.fiap.fastfood.producao.application;

import br.com.fiap.fastfood.producao.domain.ProducaoPedido;
import br.com.fiap.fastfood.producao.domain.StatusProducao;
import br.com.fiap.fastfood.producao.infra.ProducaoPedidoRepository;
import br.com.fiap.fastfood.producao.integration.PedidoClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProducaoService {

    private final ProducaoPedidoRepository repository;
    private final PedidoClient pedidoClient;

    public ProducaoService(ProducaoPedidoRepository repository, PedidoClient pedidoClient) {
        this.repository = repository;
        this.pedidoClient = pedidoClient;
    }

    @Transactional
    public ProducaoPedido iniciar(Long pedidoId) {
        ProducaoPedido producao = repository.findByPedidoId(pedidoId)
                .orElseGet(() -> repository.save(new ProducaoPedido(pedidoId)));

        // Passo F: ao iniciar, precisa ir para EM_PREPARACAO
        if (producao.getStatusAtual() == StatusProducao.RECEBIDO) {
            producao.atualizarStatus(StatusProducao.EM_PREPARACAO, "Produção iniciada");
        }

        ProducaoPedido salvo = repository.save(producao);

        // Atualiza o pedido-service (mapeando status de produção -> status do pedido)
        // Seu enunciado diz que o producao-service chama com EM_PRODUCAO e PRONTO
        pedidoClient.atualizarStatusPedido(pedidoId, "EM_PREPARACAO");

        return salvo;
    }

    @Transactional
    public ProducaoPedido atualizarStatus(Long pedidoId, StatusProducao status, String observacao) {
        ProducaoPedido producao = repository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido " + pedidoId + " não existe na produção. Use /iniciar primeiro."));

        producao.atualizarStatus(status, observacao);
        ProducaoPedido salvo = repository.save(producao);

        // Se ficou pronto na produção, avisa o pedido-service
        if (status == StatusProducao.PRONTO) {
            pedidoClient.atualizarStatusPedido(pedidoId, "PRONTO");
        }

        return salvo;
    }

    public ProducaoPedido consultar(Long pedidoId) {
        return repository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido " + pedidoId + " não encontrado na produção."));
    }
}
