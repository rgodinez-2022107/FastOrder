package com.fastorder.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastorder.repository.UsuarioRepository;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String login(String email, String password) throws Exception {
        Map<String, String> body = Map.of("email", email, "password", password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    private long registrar(String email) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("nombre", "Usuario Test");
        body.put("email", email);
        body.put("password", "Secret123!");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    @Test
    void registerCreaUsuarioConRolCliente() throws Exception {
        String email = "test-register-" + System.nanoTime() + "@test.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"email\":\"" + email + "\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void registerIgnoraIntentoDeElevacionDePrivilegios() throws Exception {
        String email = "test-hack-" + System.nanoTime() + "@test.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Hacker\",\"email\":\"" + email
                                + "\",\"password\":\"Secret123!\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        String email2 = "test-hack2-" + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Hacker2\",\"email\":\"" + email2
                                + "\",\"password\":\"Secret123!\",\"rol\":\"REPARTIDOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void registerEmailDuplicadoDevuelve409() throws Exception {
        String email = "test-dup-" + System.nanoTime() + "@test.com";
        registrar(email);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Dup\",\"email\":\"" + email + "\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registerEmailInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"email\":\"no-es-email\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginConCredencialesValidasDevuelve200YToken() throws Exception {
        String token = login("cliente@fastorder.com", "Cliente123!");

        assertThat(token).isNotBlank();
        assertThat(usuarioRepository.findByEmail("cliente@fastorder.com")).isPresent();
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cliente@fastorder.com\",\"password\":\"Mala123!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/comercios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenBasuroDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/comercios")
                        .header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoPuedeCrearComercio403() throws Exception {
        String token = login("cliente@fastorder.com", "Cliente123!");

        mockMvc.perform(post("/api/v1/comercios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"No Permitido\",\"categoria\":\"FARMACIA\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void repartidorNoPuedeCrearComercio403() throws Exception {
        String token = login("repartidor@fastorder.com", "Repartidor123!");

        mockMvc.perform(post("/api/v1/comercios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"No Permitido\",\"categoria\":\"FARMACIA\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeCrearComercio201() throws Exception {
        String token = login("admin@fastorder.com", "Admin123!");
        String nombre = "Comercio Admin " + System.nanoTime();

        mockMvc.perform(post("/api/v1/comercios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"categoria\":\"SUPERMERCADO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.categoria").value("SUPERMERCADO"));
    }

    @Test
    void clienteNoVeDisponibles403() throws Exception {
        String token = login("cliente@fastorder.com", "Cliente123!");

        mockMvc.perform(get("/api/v1/pedidos/disponibles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void repartidorSiVeDisponibles200() throws Exception {
        String token = login("repartidor@fastorder.com", "Repartidor123!");

        mockMvc.perform(get("/api/v1/pedidos/disponibles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoAccedeEstadoDePedido403() throws Exception {
        String token = login("cliente@fastorder.com", "Cliente123!");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/pedidos/1/estado")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PREPARACION\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNoPuedeCrearPedido403() throws Exception {
        String token = login("admin@fastorder.com", "Admin123!");

        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comercioId\":1,\"productos\":[{\"productoId\":1,\"cantidad\":1}]}"))
                .andExpect(status().isForbidden());
    }
}
