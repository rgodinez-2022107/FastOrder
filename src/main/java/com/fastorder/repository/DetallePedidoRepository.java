package com.fastorder.repository;

import com.fastorder.entity.DetallePedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    interface CantidadPorProducto {
        Long getProductoId();

        Integer getCantidad();
    }

    @Query("select d.producto.id as productoId, d.cantidad as cantidad "
            + "from DetallePedido d where d.pedido.id = :pedidoId order by d.producto.id")
    List<CantidadPorProducto> findCantidadesByPedidoId(@Param("pedidoId") Long pedidoId);
}
