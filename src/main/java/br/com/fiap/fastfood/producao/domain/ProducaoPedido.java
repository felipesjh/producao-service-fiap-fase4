package br.com.fiap.fastfood.producao.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "producao_pedidos")
public class ProducaoPedido {

    @Id
    private String id;

    private Long pedidoId;
    private StatusProducao statusAtual;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    private List<EventoProducao> historico = new ArrayList<>();

    public ProducaoPedido() {}

    public ProducaoPedido(Long pedidoId) {
        this.pedidoId = pedidoId;
        this.statusAtual = StatusProducao.RECEBIDO;
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
        this.historico.add(new EventoProducao(
                StatusProducao.RECEBIDO,
                LocalDateTime.now(),
                "Pedido recebido na produção"
        ));
    }

    public void atualizarStatus(StatusProducao novoStatus, String observacao) {
        this.statusAtual = novoStatus;
        this.atualizadoEm = LocalDateTime.now();
        this.historico.add(new EventoProducao(novoStatus, LocalDateTime.now(), observacao));
    }

    public String getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public StatusProducao getStatusAtual() { return statusAtual; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public List<EventoProducao> getHistorico() { return historico; }

    public void setId(String id) { this.id = id; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }
    public void setStatusAtual(StatusProducao statusAtual) { this.statusAtual = statusAtual; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public void setHistorico(List<EventoProducao> historico) { this.historico = historico; }
}
