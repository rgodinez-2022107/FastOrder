package com.fastorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
public class ProductoRequest {

    @NotBlank(message = "el nombre es obligatorio")
    @Size(max = 150, message = "el nombre no puede exceder 150 caracteres")
    private String nombre;

    @NotNull(message = "el precio es obligatorio")
    @DecimalMin(value = "0.00", message = "el precio no puede ser negativo")
    private BigDecimal precio;

    @NotNull(message = "el stock es obligatorio")
    @PositiveOrZero(message = "el stock no puede ser negativo")
    private Integer stock;

    private Boolean disponible;
}
