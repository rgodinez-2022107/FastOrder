package com.fastorder.dto;

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
public class ProductoResponse {

    private Long id;
    private Long comercioId;
    private String comercioNombre;
    private String nombre;
    private BigDecimal precio;
    private int stock;
    private boolean disponible;
}
