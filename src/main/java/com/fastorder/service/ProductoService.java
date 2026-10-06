package com.fastorder.service;

import com.fastorder.dto.DtoMapper;
import com.fastorder.dto.ProductoRequest;
import com.fastorder.dto.ProductoResponse;
import com.fastorder.entity.Comercio;
import com.fastorder.entity.Producto;
import com.fastorder.exception.ResourceNotFoundException;
import com.fastorder.repository.ComercioRepository;
import com.fastorder.repository.ProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ComercioRepository comercioRepository;

    public ProductoService(ProductoRepository productoRepository, ComercioRepository comercioRepository) {
        this.productoRepository = productoRepository;
        this.comercioRepository = comercioRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponse> listarPorComercio(Long comercioId, boolean soloDisponibles, Pageable pageable) {
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio", comercioId));
        Page<Producto> productos = soloDisponibles
                ? productoRepository.findByComercioIdAndDisponibleTrue(comercio.getId(), pageable)
                : productoRepository.findByComercioId(comercio.getId(), pageable);
        return productos.map(DtoMapper::toProductoResponse);
    }

    @Transactional
    public ProductoResponse crear(Long comercioId, ProductoRequest request) {
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio", comercioId));

        Producto producto = Producto.builder()
                .comercio(comercio)
                .nombre(request.getNombre())
                .precio(request.getPrecio())
                .stock(request.getStock())
                .disponible(request.getDisponible() == null || request.getDisponible())
                .build();

        Producto guardado = productoRepository.save(producto);
        return DtoMapper.toProductoResponse(guardado);
    }
}
