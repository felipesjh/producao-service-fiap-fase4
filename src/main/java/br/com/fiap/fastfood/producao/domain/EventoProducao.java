package br.com.fiap.fastfood.producao.domain;

import java.time.LocalDateTime;

public class EventoProducao {

    private StatusProducao status;
    private LocalDateTime dataHora;
    private String observacao;

    public EventoProducao() {}

    public EventoProducao(StatusProducao status, LocalDateTime dataHora, String observacao) {
        this.status = status;
        this.dataHora = dataHora;
        this.observacao = observacao;
    }

    public StatusProducao getStatus() {
        return status;
    }

    public void setStatus(StatusProducao status) {
        this.status = status;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
