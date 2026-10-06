package com.fastorder.dto;

import com.fastorder.enums.EstadoPedido;
import jakarta.validation.constraints.NotNull;
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
public class EstadoUpdateRequest {

    @NotNull(message = "el estado es obligatorio")
    private EstadoPedido estado;
}
