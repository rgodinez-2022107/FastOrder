package com.fastorder.dto;

import com.fastorder.entity.Comercio;
import com.fastorder.entity.DetallePedido;
import com.fastorder.entity.Pedido;
import com.fastorder.entity.Producto;
import com.fastorder.entity.Usuario;
import java.util.ArrayList;
import java.util.List;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static ComercioResponse toComercioResponse(Comercio comercio) {
        return ComercioResponse.builder()
                .id(comercio.getId())
                .nombre(comercio.getNombre())
                .categoria(comercio.getCategoria())
                .direccion(comercio.getDireccion())
                .abierto(comercio.isAbierto())
                .build();
    }

    public static ProductoResponse toProductoResponse(Producto producto) {
        return ProductoResponse.builder()
                .id(producto.getId())
                .comercioId(producto.getComercio().getId())
                .comercioNombre(producto.getComercio().getNombre())
                .nombre(producto.getNombre())
                .precio(producto.getPrecio())
                .stock(producto.getStock())
                .disponible(producto.isDisponible())
                .build();
    }

    public static UsuarioResponse toUsuarioResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .direccion(usuario.getDireccion())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol())
                .build();
    }

    public static PedidoResponse toPedidoResponse(Pedido pedido) {
        List<DetallePedidoResponse> detalles = new ArrayList<>(pedido.getDetalles().size());
        for (DetallePedido detalle : pedido.getDetalles()) {
            detalles.add(toDetallePedidoResponse(detalle));
        }
        return PedidoResponse.builder()
                .id(pedido.getId())
                .clienteId(pedido.getCliente().getId())
                .clienteNombre(pedido.getCliente().getNombre())
                .repartidorId(pedido.getRepartidor() != null ? pedido.getRepartidor().getId() : null)
                .repartidorNombre(pedido.getRepartidor() != null ? pedido.getRepartidor().getNombre() : null)
                .fechaPedido(pedido.getFechaPedido())
                .costoEnvio(pedido.getCostoEnvio())
                .montoTotal(pedido.getMontoTotal())
                .estado(pedido.getEstado())
                .detalles(detalles)
                .build();
    }

    public static DetallePedidoResponse toDetallePedidoResponse(DetallePedido detalle) {
        return DetallePedidoResponse.builder()
                .productoId(detalle.getProducto().getId())
                .productoNombre(detalle.getProducto().getNombre())
                .cantidad(detalle.getCantidad())
                .precioUnitario(detalle.getPrecioUnitario())
                .subtotal(detalle.getSubtotal())
                .build();
    }
}
