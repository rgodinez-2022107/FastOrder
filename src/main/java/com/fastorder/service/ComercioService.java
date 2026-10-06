package com.fastorder.service;

import com.fastorder.dto.ComercioRequest;
import com.fastorder.dto.ComercioResponse;
import com.fastorder.dto.DtoMapper;
import com.fastorder.entity.Comercio;
import com.fastorder.enums.CategoriaComercio;
import com.fastorder.exception.ResourceNotFoundException;
import com.fastorder.repository.ComercioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComercioService {

    private final ComercioRepository comercioRepository;

    public ComercioService(ComercioRepository comercioRepository) {
        this.comercioRepository = comercioRepository;
    }

    @Transactional(readOnly = true)
    public Page<ComercioResponse> listar(CategoriaComercio categoria, Pageable pageable) {
        Page<Comercio> comercios = categoria != null
                ? comercioRepository.findByAbiertoTrueAndCategoria(categoria, pageable)
                : comercioRepository.findByAbiertoTrue(pageable);
        return comercios.map(DtoMapper::toComercioResponse);
    }

    @Transactional
    public ComercioResponse crear(ComercioRequest request) {
        Comercio comercio = Comercio.builder()
                .nombre(request.getNombre())
                .categoria(request.getCategoria())
                .direccion(request.getDireccion())
                .abierto(request.getAbierto() == null || request.getAbierto())
                .build();
        Comercio guardado = comercioRepository.save(comercio);
        return DtoMapper.toComercioResponse(guardado);
    }

    @Transactional(readOnly = true)
    public Comercio obtenerPorId(Long id) {
        return comercioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio", id));
    }
}
