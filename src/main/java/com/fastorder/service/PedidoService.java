package com.fastorder.service;

import com.fastorder.dto.DetallePedidoCreateRequest;
import com.fastorder.dto.DetallePedidoResponse;
import com.fastorder.dto.DtoMapper;
import com.fastorder.dto.EstadoUpdateRequest;
import com.fastorder.dto.PedidoCreateRequest;
import com.fastorder.dto.PedidoResponse;
import com.fastorder.dto.PageResponse;
import com.fastorder.entity.Comercio;
import com.fastorder.entity.DetallePedido;
import com.fastorder.entity.Pedido;
import com.fastorder.entity.Producto;
import com.fastorder.entity.Usuario;
import com.fastorder.enums.EstadoPedido;
import com.fastorder.enums.Rol;
import com.fastorder.exception.InsufficientStockException;
import com.fastorder.exception.InvalidStatusException;
import com.fastorder.exception.ResourceNotFoundException;
import com.fastorder.repository.ComercioRepository;
import com.fastorder.repository.DetallePedidoRepository;
import com.fastorder.repository.PedidoRepository;
import com.fastorder.repository.ProductoRepository;
import com.fastorder.repository.UsuarioRepository;
import com.fastorder.security.CustomUserDetailsService.UsuarioPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private static final int ESCALA_DINERO = 2;

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final ComercioRepository comercioRepository;
    private final UsuarioRepository usuarioRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final BigDecimal costoEnvio;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProductoRepository productoRepository,
                         ComercioRepository comercioRepository,
                         UsuarioRepository usuarioRepository,
                         DetallePedidoRepository detallePedidoRepository,
                         @Value("${fastorder.shipping-cost}") BigDecimal costoEnvio) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.comercioRepository = comercioRepository;
        this.usuarioRepository = usuarioRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.costoEnvio = costoEnvio.setScale(ESCALA_DINERO, RoundingMode.HALF_UP);
    }

    @Transactional
    public PedidoResponse crearPedido(PedidoCreateRequest request, UsuarioPrincipal principal) {
        if ((request.getItems() == null || request.getItems().isEmpty()) && (request.getProductos() == null || request.getProductos().isEmpty())) {
            throw new IllegalArgumentException("El pedido debe incluir al menos un producto");
        }

        Usuario cliente = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", principal.getId()));

        Comercio comercio = comercioRepository.findById(request.getComercioId())
                .orElseThrow(() -> new ResourceNotFoundException("Comercio", request.getComercioId()));

        List<DetallePedidoCreateRequest> itemsReq = (request.getItems() != null && !request.getItems().isEmpty())
                ? request.getItems()
                : request.getProductos();
        if (itemsReq == null || itemsReq.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe incluir al menos un producto");
        }
        Map<Long, Integer> cantidadesPorProducto = agruparCantidades(itemsReq);
        List<Long> productoIds = cantidadesPorProducto.keySet().stream().sorted().toList();

        List<Producto> productos = productoRepository.lockAllByIdInAndComercioId(productoIds, comercio.getId());
        if (productos.size() != productoIds.size()) {
            List<Long> encontrados = productos.stream().map(Producto::getId).toList();
            Long faltante = productoIds.stream().filter(id -> !encontrados.contains(id)).findFirst().orElseThrow();
            throw new ResourceNotFoundException("Producto " + faltante
                    + " no existe o no pertenece al comercio " + comercio.getId());
        }

        Map<Long, Integer> pendientes = new LinkedHashMap<>(cantidadesPorProducto);
        for (Producto producto : productos) {
            int cantidad = pendientes.get(producto.getId());
            if (!producto.isDisponible()) {
                throw new InsufficientStockException(producto.getNombre(), 0, cantidad);
            }
            if (producto.getStock() < cantidad) {
                throw new InsufficientStockException(producto.getNombre(), producto.getStock(), cantidad);
            }
        }

        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .fechaPedido(LocalDateTime.now())
                .costoEnvio(costoEnvio)
                .estado(EstadoPedido.PENDIENTE)
                .build();

        BigDecimal montoTotal = BigDecimal.ZERO;
        for (Producto producto : productos) {
            int cantidad = pendientes.get(producto.getId());
            BigDecimal precio = producto.getPrecio().setScale(ESCALA_DINERO, RoundingMode.HALF_UP);
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad))
                    .setScale(ESCALA_DINERO, RoundingMode.HALF_UP);

            DetallePedido detalle = DetallePedido.builder()
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(precio)
                    .subtotal(subtotal)
                    .build();
            pedido.addDetalle(detalle);

            producto.setStock(producto.getStock() - cantidad);
            montoTotal = montoTotal.add(subtotal);
        }

        pedido.setMontoTotal(montoTotal.add(costoEnvio).setScale(ESCALA_DINERO, RoundingMode.HALF_UP));

        Pedido guardado = pedidoRepository.save(pedido);
        log.info("Pedido {} creado para cliente {}: total {}", guardado.getId(), cliente.getEmail(),
                guardado.getMontoTotal());
        return DtoMapper.toPedidoResponse(guardado);
    }

    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> misPedidos(UsuarioPrincipal principal, Pageable pageable) {
        Page<Long> ids = pedidoRepository.findIdsByClienteId(principal.getId(), pageable);
        return mapearPaginaIds(ids, DtoMapper::toPedidoResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> disponibles(UsuarioPrincipal principal, Pageable pageable) {
        Page<Long> ids = principal.getUsuario().getRol() == Rol.ADMIN
                ? pedidoRepository.findIdsDisponiblesParaAdmin(pageable)
                : pedidoRepository.findIdsDisponiblesParaRepartidor(principal.getId(), pageable);
        return mapearPaginaIds(ids, DtoMapper::toPedidoResponse);
    }

    @Transactional
    public PedidoResponse actualizarEstado(Long id, EstadoUpdateRequest request, UsuarioPrincipal principal) {
        if (request.getEstado() != EstadoPedido.EN_PREPARACION
                && request.getEstado() != EstadoPedido.EN_CAMINO
                && request.getEstado() != EstadoPedido.ENTREGADO) {
            throw new InvalidStatusException("Estado no permitido por este endpoint: " + request.getEstado()
                    + ". Valores aceptados: EN_PREPARACION, EN_CAMINO, ENTREGADO");
        }

        Pedido pedido = pedidoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        Usuario responsable = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", principal.getId()));

        if (responsable.getRol() == Rol.REPARTIDOR && pedido.getRepartidor() != null
                && !pedido.getRepartidor().getId().equals(responsable.getId())) {
            throw new AccessDeniedException("El pedido ya esta asignado a otro repartidor");
        }

        EstadoPedido actual = pedido.getEstado();
        if (!actual.puedeTransicionarA(request.getEstado())) {
            throw new InvalidStatusException("Transicion invalida: " + actual + " -> " + request.getEstado());
        }

        if (pedido.getRepartidor() == null && responsable.getRol() == Rol.REPARTIDOR) {
            pedido.setRepartidor(responsable);
        }
        pedido.setEstado(request.getEstado());

        Pedido actualizado = pedidoRepository.save(pedido);
        log.info("Pedido {} : {} -> {}", actualizado.getId(), actual, request.getEstado());
        return DtoMapper.toPedidoResponse(actualizado);
    }

    @Transactional
    public PedidoResponse cancelar(Long id, UsuarioPrincipal principal) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (principal.getUsuario().getRol() == Rol.CLIENTE
                && !pedido.getCliente().getId().equals(principal.getId())) {
            throw new AccessDeniedException("No puede cancelar pedidos de otro cliente");
        }

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new InvalidStatusException("Solo se pueden cancelar pedidos en estado PENDIENTE. "
                    + "Estado actual: " + pedido.getEstado());
        }

        restaurarStock(pedido);
        pedido.setEstado(EstadoPedido.CANCELADO);
        Pedido cancelado = pedidoRepository.save(pedido);
        log.info("Pedido {} cancelado y stock restaurado", cancelado.getId());
        return DtoMapper.toPedidoResponse(cancelado);
    }

    private void restaurarStock(Pedido pedido) {
        List<DetallePedidoRepository.CantidadPorProducto> cantidades =
                detallePedidoRepository.findCantidadesByPedidoId(pedido.getId());
        for (DetallePedidoRepository.CantidadPorProducto item : cantidades) {
            productoRepository.incrementarStock(item.getProductoId(), item.getCantidad());
        }
    }

    private Map<Long, Integer> agruparCantidades(List<DetallePedidoCreateRequest> productos) {
        Map<Long, Integer> resultado = new LinkedHashMap<>();
        for (DetallePedidoCreateRequest item : productos) {
            if (item.getProductoId() == null) {
                throw new IllegalArgumentException("Cada producto del pedido debe tener productoId");
            }
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad del producto " + item.getProductoId()
                        + " debe ser mayor a cero");
            }
            resultado.merge(item.getProductoId(), item.getCantidad(), Integer::sum);
        }
        return resultado;
    }

    private <T> PageResponse<T> mapearPaginaIds(Page<Long> ids,
                                                java.util.function.Function<Pedido, T> mapper) {
        List<T> contenido = new ArrayList<>();
        if (!ids.isEmpty()) {
            List<Pedido> pedidos = pedidoRepository.findAllWithDetallesByIdIn(ids.getContent());
            for (Pedido pedido : pedidos) {
                contenido.add(mapper.apply(pedido));
            }
        }
        return PageResponse.<T>builder()
                .content(contenido)
                .page(ids.getNumber())
                .size(ids.getSize())
                .totalElements(ids.getTotalElements())
                .totalPages(ids.getTotalPages())
                .first(ids.isFirst())
                .last(ids.isLast())
                .build();
    }
}
