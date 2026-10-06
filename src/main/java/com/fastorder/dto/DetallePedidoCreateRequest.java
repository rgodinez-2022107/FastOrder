package com.fastorder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class DetallePedidoCreateRequest {

    @NotNull(message = "el producto es obligatorio")
    private Long productoId;

    @NotNull(message = "la cantidad es obligatoria")
    @Positive(message = "la cantidad debe ser mayor a cero")
    private Integer cantidad;
}
