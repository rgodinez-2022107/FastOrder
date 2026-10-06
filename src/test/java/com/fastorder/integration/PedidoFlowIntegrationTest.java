package com.fastorder.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastorder.entity.Producto;
import com.fastorder.repository.ProductoRepository;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PedidoFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductoRepository productoRepository;

    private String tokenCliente;
    private String tokenAdmin;
    private String tokenRepartidor;

    @BeforeEach
    void autenticar() throws Exception {
        tokenCliente = login("cliente@fastorder.com", "Cliente123!");
        tokenAdmin = login("admin@fastorder.com", "Admin123!");
        tokenRepartidor = login("repartidor@fastorder.com", "Repartidor123!");
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long crearProducto(long comercioId, int stock, String precio) throws Exception {
        String nombre = "Prod-" + ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
        MvcResult result = mockMvc.perform(post("/api/v1/comercios/" + comercioId + "/productos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"precio\":" + precio
                                + ",\"stock\":" + stock + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long crearPedido(long comercioId, long productoId, int cantidad) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comercioId\":" + comercioId + ",\"productos\":[{\"productoId\":"
                                + productoId + ",\"cantidad\":" + cantidad + "}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void crearPedidoCalculaTotalesEnElServidor() throws Exception {
        long productoId = crearProducto(1, 50, "33.50");

        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comercioId\":1,\"productos\":[{\"productoId\":" + productoId
                                + ",\"cantidad\":3}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costoEnvio").value(20.00))
                .andExpect(jsonPath("$.montoTotal").value(120.50))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.detalles[0].precioUnitario").value(33.50))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(100.50))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(3));
    }

    @Test
    void crearPedidoDescuentaStock() throws Exception {
        long productoId = crearProducto(1, 10, "15.00");
        int antes = productoRepository.findById(productoId).orElseThrow().getStock();

        crearPedido(1, productoId, 4);

        int despues = productoRepository.findById(productoId).orElseThrow().getStock();
        assertThat(despues).isEqualTo(antes - 4);
    }

    @Test
    void stockInsuficienteDevuelve409SinDescuento() throws Exception {
        long productoId = crearProducto(1, 2, "10.00");
        int antes = productoRepository.findById(productoId).orElseThrow().getStock();

        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comercioId\":1,\"productos\":[{\"productoId\":" + productoId
                                + ",\"cantidad\":5}]}"))
                .andExpect(status().isConflict());

        assertThat(productoRepository.findById(productoId).orElseThrow().getStock()).isEqualTo(antes);
    }

    @Test
    void falloParcialDeUnProductoHaceRollbackCompleto() throws Exception {
        long productoOk = crearProducto(1, 10, "10.00");
        long productoSinStock = crearProducto(1, 1, "20.00");
        int stockAntes = productoRepository.findById(productoOk).orElseThrow().getStock();

        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comercioId\":1,\"productos\":[{\"productoId\":" + productoOk
                                + ",\"cantidad\":2},{\"productoId\":" + productoSinStock
                                + ",\"cantidad\":99}]}"))
                .andExpect(status().isConflict());

        assertThat(productoRepository.findById(productoOk).orElseThrow().getStock())
                .isEqualTo(stockAntes);
    }

    @Test
    void productoDeOtroComercioDevuelve404() throws Exception {
        long productoId = crearProducto(1, 10, "10.00");

        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comercioId\":2,\"productos\":[{\"productoId\":" + productoId
                                + ",\"cantidad\":1}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void misPedidosSoloDevuelveLosDelUsuarioAutenticado() throws Exception {
        long productoId = crearProducto(1, 10, "10.00");
        long pedidoId = crearPedido(1, productoId, 1);

        MvcResult result = mockMvc.perform(get("/api/v1/pedidos/mis-pedidos")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        assertThat(content.isArray()).isTrue();
        boolean encontrado = false;
        for (JsonNode nodo : content) {
            assertThat(nodo.get("clienteId").asLong()).isEqualTo(3L);
            if (nodo.get("id").asLong() == pedidoId) {
                encontrado = true;
            }
        }
        assertThat(encontrado).as("el pedido creado debe aparecer en mis-pedidos").isTrue();
    }

    @Test
    void flujoCompletoDeEstadosFinalizaEnEntregado() throws Exception {
        long productoId = crearProducto(1, 10, "10.00");
        long pedidoId = crearPedido(1, productoId, 1);

        mockMvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/estado")
                        .header("Authorization", "Bearer " + tokenRepartidor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PREPARACION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"))
                .andExpect(jsonPath("$.repartidorId").value(2));

        mockMvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/estado")
                        .header("Authorization", "Bearer " + tokenRepartidor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_CAMINO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CAMINO"));

        mockMvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/estado")
                        .header("Authorization", "Bearer " + tokenRepartidor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ENTREGADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));
    }

    @Test
    void transicionInvalidaDevuelve409() throws Exception {
        long productoId = crearProducto(1, 10, "10.00");
        long pedidoId = crearPedido(1, productoId, 1);

        mockMvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/estado")
                        .header("Authorization", "Bearer " + tokenRepartidor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ENTREGADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void estadoCanceladoNoEsPermitidoEnEndpointDeEstado() throws Exception {
        long productoId = crearProducto(1, 10, "10.00");
        long pedidoId = crearPedido(1, productoId, 1);

        mockMvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/estado")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarPedidoPendienteRestauraStock() throws Exception {
        long productoId = crearProducto(1, 10, "10.00");
        int stockOriginal = productoRepository.findById(productoId).orElseThrow().getStock();
        long pedidoId = crearPedido(1, productoId, 3);

        assertThat(productoRepository.findById(productoId).orElseThrow().getStock())
                .isEqualTo(stockOriginal - 3);

        mockMvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        assertThat(productoRepository.findById(productoId).orElseThrow().getStock())
                .isEqualTo(stockOriginal);
    }

    @Test
    void cancelarPedidoNoPendienteDevuelve409() throws Exception {
        long productoId = crearProducto(1, 10, "10.00");
        long pedidoId = crearPedido(1, productoId, 1);

        mockMvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/estado")
                        .header("Authorization", "Bearer " + tokenRepartidor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PREPARACION\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isConflict());
    }

    @Test
    void clienteNoPuedeCancelarPedidoDeOtroCliente() throws Exception {
        long productoId = crearProducto(1, 10, "10.00");
        long pedidoId = crearPedido(1, productoId, 1);

        String emailOtro = "otro-cliente-" + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otro\",\"email\":\"" + emailOtro
                                + "\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isCreated());
        String tokenOtro = login(emailOtro, "Secret123!");

        mockMvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenOtro))
                .andExpect(status().isForbidden());
    }

    @Test
    void pedidoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(patch("/api/v1/pedidos/999999/estado")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PREPARACION\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listadoDeComerciosConFiltroDeCategoria() throws Exception {
        mockMvc.perform(get("/api/v1/comercios?categoria=RESTAURANTE")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoria").value("RESTAURANTE"));

        mockMvc.perform(get("/api/v1/comercios?categoria=NOEXISTE")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listadoDeProductosPorComercio() throws Exception {
        mockMvc.perform(get("/api/v1/comercios/1/productos")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void stockNuncaNegativoEnBD() throws Exception {
        long productoId = crearProducto(1, 3, "5.00");
        long antes = productoRepository.findById(productoId).orElseThrow().getStock();

        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comercioId\":1,\"productos\":[{\"productoId\":" + productoId
                                + ",\"cantidad\":3}]}"))
                .andExpect(status().isCreated());

        Producto producto = productoRepository.findById(productoId).orElseThrow();
        assertThat(producto.getStock()).isEqualTo(0);
        assertThat(antes).isEqualTo(3);
    }
}
