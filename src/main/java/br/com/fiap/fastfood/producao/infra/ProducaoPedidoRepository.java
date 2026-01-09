package br.com.fiap.fastfood.producao.infra;

import br.com.fiap.fastfood.producao.domain.ProducaoPedido;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProducaoPedidoRepository extends MongoRepository<ProducaoPedido, String> {
    Optional<ProducaoPedido> findByPedidoId(Long pedidoId);
    boolean existsByPedidoId(Long pedidoId);
}
