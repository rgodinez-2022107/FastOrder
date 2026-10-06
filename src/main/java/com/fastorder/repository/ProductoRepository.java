package com.fastorder.repository;

import com.fastorder.entity.Producto;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Page<Producto> findByComercioId(Long comercioId, Pageable pageable);

    Page<Producto> findByComercioIdAndDisponibleTrue(Long comercioId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id in :ids and p.comercio.id = :comercioId order by p.id")
    List<Producto> lockAllByIdInAndComercioId(@Param("ids") Collection<Long> ids,
                                              @Param("comercioId") Long comercioId);

    @Modifying(flushAutomatically = true)
    @Query("update Producto p set p.stock = p.stock + :cantidad where p.id = :id")
    int incrementarStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
