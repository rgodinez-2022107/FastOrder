package com.fastorder.dto;

import com.fastorder.enums.CategoriaComercio;
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
public class ComercioResponse {

    private Long id;
    private String nombre;
    private CategoriaComercio categoria;
    private String direccion;
    private boolean abierto;
}
