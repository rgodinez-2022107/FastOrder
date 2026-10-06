package com.fastorder.controller;

import com.fastorder.dto.ComercioRequest;
import com.fastorder.dto.ComercioResponse;
import com.fastorder.dto.PageResponse;
import com.fastorder.dto.ProductoRequest;
import com.fastorder.dto.ProductoResponse;
import com.fastorder.enums.CategoriaComercio;
import com.fastorder.service.ComercioService;
import com.fastorder.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comercios")
public class ComercioController {

    private final ComercioService comercioService;
    private final ProductoService productoService;

    public ComercioController(ComercioService comercioService, ProductoService productoService) {
        this.comercioService = comercioService;
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ComercioResponse>> listar(
            @RequestParam(name = "categoria", required = false) CategoriaComercio categoria,
            @PageableDefault(size = 50, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(comercioService.listar(categoria, pageable)));
    }

    @PostMapping
    public ResponseEntity<ComercioResponse> crear(@Valid @RequestBody ComercioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comercioService.crear(request));
    }

    @GetMapping("/{id}/productos")
    public ResponseEntity<PageResponse<ProductoResponse>> listarProductos(
            @PathVariable("id") Long id,
            @PageableDefault(size = 100, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(productoService.listarPorComercio(id, false, pageable)));
    }

    @PostMapping("/{id}/productos")
    public ResponseEntity<ProductoResponse> crearProducto(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(id, request));
    }
}
