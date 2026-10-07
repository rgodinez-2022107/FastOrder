package com.fastorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoCreateRequest {

    @NotNull(message = "el comercio es obligatorio")
    private Long comercioId;

    @NotNull(message = "los productos son obligatorios")
    @Valid
    private List<DetallePedidoCreateRequest> productos;

    @NotNull(message = "los productos son obligatorios")
    @Valid
    private List<DetallePedidoCreateRequest> items;

    public List<DetallePedidoCreateRequest> getProductos() {
        if (productos != null && !productos.isEmpty()) {
            return productos;
        }
        return items;
    }

    public List<DetallePedidoCreateRequest> getItems() {
        if (items != null && !items.isEmpty()) {
            return items;
        }
        return productos;
    }

    public void setProductos(List<DetallePedidoCreateRequest> productos) {
        this.productos = productos;
    }

    public void setItems(List<DetallePedidoCreateRequest> items) {
        this.items = items;
    }
}
