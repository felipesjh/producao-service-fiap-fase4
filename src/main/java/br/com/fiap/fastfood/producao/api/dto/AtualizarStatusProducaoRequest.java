package br.com.fiap.fastfood.producao.api.dto;

import br.com.fiap.fastfood.producao.domain.StatusProducao;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusProducaoRequest(
        @NotNull StatusProducao status,
        String observacao
) {}
