package com.fastorder.controller;

import com.fastorder.dto.EstadoUpdateRequest;
import com.fastorder.dto.PedidoCreateRequest;
import com.fastorder.dto.PedidoResponse;
import com.fastorder.dto.PageResponse;
import com.fastorder.security.CustomUserDetailsService.UsuarioPrincipal;
import com.fastorder.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crear(
            @Valid @RequestBody PedidoCreateRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crearPedido(request, principal));
    }

    @GetMapping("/mis-pedidos")
    public ResponseEntity<PageResponse<PedidoResponse>> misPedidos(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PageableDefault(size = 50, sort = "fechaPedido", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(pedidoService.misPedidos(principal, pageable));
    }

    @GetMapping("/disponibles")
    public ResponseEntity<PageResponse<PedidoResponse>> disponibles(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PageableDefault(size = 50, sort = "fechaPedido", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(pedidoService.disponibles(principal, pageable));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> actualizarEstado(
            @PathVariable("id") Long id,
            @Valid @RequestBody EstadoUpdateRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(pedidoService.actualizarEstado(id, request, principal));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponse> cancelar(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(pedidoService.cancelar(id, principal));
    }
}
