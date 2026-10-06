package com.fastorder.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fastorder.entity.Usuario;
import com.fastorder.enums.Rol;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "fastorder-dev-secret-change-me-0123456789abcdef0123";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    private Usuario usuario(long id, String email, Rol rol) {
        return Usuario.builder()
                .id(id)
                .nombre("Test")
                .email(email)
                .password("no-debe-aparecer")
                .rol(rol)
                .build();
    }

    @Test
    void generaTokenConIdentidadYRol() {
        String token = jwtService.generateToken(usuario(5, "admin@test.com", Rol.ADMIN));

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@test.com");
        String rol = jwtService.extractClaim(token, c -> c.get("rol", String.class));
        Long id = jwtService.extractClaim(token, c -> c.get("id", Long.class));
        assertThat(rol).isEqualTo("ADMIN");
        assertThat(id).isEqualTo(5L);
    }

    @Test
    void tokenValidoParaSuPropietario() {
        String token = jwtService.generateToken(usuario(1, "cliente@test.com", Rol.CLIENTE));

        assertThat(jwtService.isTokenValid(token, "cliente@test.com")).isTrue();
        assertThat(jwtService.isTokenValid(token, "otro@test.com")).isFalse();
    }

    @Test
    void tokenExpiradoNoEsValido() {
        JwtService caducado = new JwtService(SECRET, -1000L);
        String token = caducado.generateToken(usuario(1, "cliente@test.com", Rol.CLIENTE));

        assertThat(jwtService.isTokenValid(token, "cliente@test.com")).isFalse();
    }

    @Test
    void tokenFirmadoConOtroSecretRechazado() {
        String token = new JwtService("otro-secret-distinto-0123456789abcdef0123", 3_600_000L)
                .generateToken(usuario(1, "cliente@test.com", Rol.CLIENTE));

        assertThat(jwtService.isTokenValid(token, "cliente@test.com")).isFalse();
    }

    @Test
    void tokenManipuladoRechazado() {
        String token = jwtService.generateToken(usuario(1, "cliente@test.com", Rol.CLIENTE));
        String manipulado = token.substring(0, token.length() - 3) + "abc";

        assertThat(jwtService.isTokenValid(manipulado, "cliente@test.com")).isFalse();
    }
}
