package com.fastorder.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class RegisterRequest {

    @NotBlank(message = "el nombre es obligatorio")
    @Size(max = 100, message = "el nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotBlank(message = "el email es obligatorio")
    @Email(message = "el email no es valido")
    @Size(max = 150, message = "el email no puede exceder 150 caracteres")
    private String email;

    @NotBlank(message = "la password es obligatoria")
    @Size(min = 6, max = 72, message = "la password debe tener entre 6 y 72 caracteres")
    private String password;

    @Size(max = 255, message = "la direccion no puede exceder 255 caracteres")
    private String direccion;

    @Size(max = 30, message = "el telefono no puede exceder 30 caracteres")
    private String telefono;
}
