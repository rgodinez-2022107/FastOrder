package com.fastorder.repository;

import com.fastorder.entity.Pedido;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "select p.id from Pedido p where p.cliente.id = :clienteId "
            + "order by p.fechaPedido desc, p.id desc",
            countQuery = "select count(p) from Pedido p where p.cliente.id = :clienteId")
    Page<Long> findIdsByClienteId(@Param("clienteId") Long clienteId, Pageable pageable);

    @Query(value = "select p.id from Pedido p "
            + "where (p.estado = com.fastorder.enums.EstadoPedido.PENDIENTE and p.repartidor is null) "
            + "or (p.estado = com.fastorder.enums.EstadoPedido.EN_CAMINO and p.repartidor.id = :repartidorId) "
            + "order by p.fechaPedido asc, p.id asc",
            countQuery = "select count(p) from Pedido p "
            + "where (p.estado = com.fastorder.enums.EstadoPedido.PENDIENTE and p.repartidor is null) "
            + "or (p.estado = com.fastorder.enums.EstadoPedido.EN_CAMINO and p.repartidor.id = :repartidorId)")
    Page<Long> findIdsDisponiblesParaRepartidor(@Param("repartidorId") Long repartidorId, Pageable pageable);

    @Query(value = "select p.id from Pedido p "
            + "where (p.estado = com.fastorder.enums.EstadoPedido.PENDIENTE and p.repartidor is null) "
            + "or p.estado = com.fastorder.enums.EstadoPedido.EN_CAMINO "
            + "order by p.fechaPedido asc, p.id asc",
            countQuery = "select count(p) from Pedido p "
            + "where (p.estado = com.fastorder.enums.EstadoPedido.PENDIENTE and p.repartidor is null) "
            + "or p.estado = com.fastorder.enums.EstadoPedido.EN_CAMINO")
    Page<Long> findIdsDisponiblesParaAdmin(Pageable pageable);

    @Query("select distinct p from Pedido p "
            + "left join fetch p.detalles d "
            + "left join fetch d.producto "
            + "where p.id in :ids")
    List<Pedido> findAllWithDetallesByIdIn(@Param("ids") Collection<Long> ids);
}
