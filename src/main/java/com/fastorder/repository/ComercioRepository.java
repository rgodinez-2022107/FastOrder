package com.fastorder.repository;

import com.fastorder.entity.Comercio;
import com.fastorder.enums.CategoriaComercio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComercioRepository extends JpaRepository<Comercio, Long> {

    Page<Comercio> findByAbiertoTrue(Pageable pageable);

    Page<Comercio> findByAbiertoTrueAndCategoria(CategoriaComercio categoria, Pageable pageable);
}
