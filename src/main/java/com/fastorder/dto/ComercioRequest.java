package com.fastorder.dto;

import com.fastorder.enums.CategoriaComercio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ComercioRequest {

    @NotBlank(message = "el nombre es obligatorio")
    @Size(max = 120, message = "el nombre no puede exceder 120 caracteres")
    private String nombre;

    @NotNull(message = "la categoria es obligatoria")
    private CategoriaComercio categoria;

    @Size(max = 255, message = "la direccion no puede exceder 255 caracteres")
    private String direccion;

    private Boolean abierto;
}
