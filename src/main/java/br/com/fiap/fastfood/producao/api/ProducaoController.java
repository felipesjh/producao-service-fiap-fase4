package br.com.fiap.fastfood.producao.api;

import br.com.fiap.fastfood.producao.api.dto.AtualizarStatusProducaoRequest;
import br.com.fiap.fastfood.producao.application.ProducaoService;
import br.com.fiap.fastfood.producao.domain.ProducaoPedido;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/producao")
public class ProducaoController {

    private final ProducaoService service;

    public ProducaoController(ProducaoService service) {
        this.service = service;
    }

    @PostMapping("/{pedidoId}/iniciar")
    @ResponseStatus(HttpStatus.CREATED)
    public ProducaoPedido iniciar(@PathVariable Long pedidoId) {
        return service.iniciar(pedidoId);
    }

    @PutMapping("/{pedidoId}/status")
    public ProducaoPedido atualizarStatus(@PathVariable Long pedidoId,
                                          @RequestBody @Valid AtualizarStatusProducaoRequest request) {
        String obs = (request.observacao() == null || request.observacao().isBlank())
                ? "Atualização de status"
                : request.observacao();

        return service.atualizarStatus(pedidoId, request.status(), obs);
    }

    @GetMapping("/{pedidoId}")
    public ProducaoPedido consultar(@PathVariable Long pedidoId) {
        return service.consultar(pedidoId);
    }
}
