package com.fastorder.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastorder.entity.Producto;
import com.fastorder.repository.ProductoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrencyIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductoRepository productoRepository;

    private String tokenCliente;
    private String tokenAdmin;

    @BeforeEach
    void preparar() throws Exception {
        tokenAdmin = login("admin@fastorder.com", "Admin123!");
        tokenCliente = login("cliente@fastorder.com", "Cliente123!");
    }

    private String login(String email, String password) throws Exception {
        ResponseEntity<String> response = rest.postForEntity("/api/v1/auth/login",
                cuerpo(Map.of("email", email, "password", password)), String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return objectMapper.readTree(response.getBody()).get("token").asText();
    }

    private HttpEntity<String> cuerpo(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(objectMapper.valueToTree(body).toString(), headers);
    }

    private HttpEntity<String> autenticado(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(objectMapper.valueToTree(body).toString(), headers);
    }

    private long crearComercio() throws Exception {
        String nombre = "Comercio Conc " + System.nanoTime();
        ResponseEntity<String> response = rest.exchange("/api/v1/comercios", HttpMethod.POST,
                autenticado(Map.of("nombre", nombre, "categoria", "SUPERMERCADO"), tokenAdmin), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }

    private long crearProducto(long comercioId, int stock, String precio) throws Exception {
        String nombre = "Prod Conc " + System.nanoTime();
        ResponseEntity<String> response = rest.exchange(
                "/api/v1/comercios/" + comercioId + "/productos", HttpMethod.POST,
                autenticado(Map.of("nombre", nombre, "precio", precio, "stock", stock), tokenAdmin),
                String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }

    private ResponseEntity<String> crearPedidoHttp(long comercioId, long productoId, int cantidad) {
        Map<String, Object> body = Map.of(
                "comercioId", comercioId,
                "productos", List.of(Map.of("productoId", productoId, "cantidad", cantidad)));
        return rest.exchange("/api/v1/pedidos", HttpMethod.POST, autenticado(body, tokenCliente), String.class);
    }

    private int stockActual(long productoId) {
        return productoRepository.findById(productoId).map(Producto::getStock).orElse(-1);
    }

    private List<Integer> ejecutarConcurrently(int hilos, Callable<Integer> tarea) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();
        try {
            for (int i = 0; i < hilos; i++) {
                futuros.add(pool.submit(() -> {
                    listos.countDown();
                    largada.await(10, TimeUnit.SECONDS);
                    return tarea.call();
                }));
            }
            assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
            largada.countDown();
            List<Integer> codigos = new ArrayList<>();
            for (Future<Integer> futuro : futuros) {
                codigos.add(futuro.get(30, TimeUnit.SECONDS));
            }
            return codigos;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void pruebaA_stockDiezDiezClientesUnoCadaUno() throws Exception {
        long comercioId = crearComercio();
        long productoId = crearProducto(comercioId, 10, "10.00");

        List<Integer> codigos = ejecutarConcurrently(10,
                () -> crearPedidoHttp(comercioId, productoId, 1).getStatusCode().value());

        long exitosos = codigos.stream().filter(c -> c == 201).count();
        assertThat(exitosos).as("exactamente 10 pedidos deben crearse").isEqualTo(10);
        assertThat(stockActual(productoId)).as("stock final debe ser 0").isZero();
        assertThat(codigos).allMatch(c -> c == 201 || c == 409);
    }

    @Test
    void pruebaB_dosSieteConStockDiez() throws Exception {
        long comercioId = crearComercio();
        long productoId = crearProducto(comercioId, 10, "10.00");

        List<Integer> codigos = ejecutarConcurrently(2,
                () -> crearPedidoHttp(comercioId, productoId, 7).getStatusCode().value());

        long exitosos = codigos.stream().filter(c -> c == 201).count();
        long conflictos = codigos.stream().filter(c -> c == 409).count();
        assertThat(exitosos).as("solo un pedido de 7 puede pasar").isEqualTo(1);
        assertThat(conflictos).as("el segundo debe recibir 409").isEqualTo(1);
        assertThat(stockActual(productoId)).as("stock final = 10 - 7 = 3").isEqualTo(3);
    }

    @Test
    void pruebaC_falloParcialNoDescuentaNada() throws Exception {
        long comercioId = crearComercio();
        long productoOk = crearProducto(comercioId, 10, "15.00");
        long productoAgotado = crearProducto(comercioId, 1, "30.00");
        int stockAntes = stockActual(productoOk);

        List<Integer> codigos = ejecutarConcurrently(4, () -> {
            Map<String, Object> body = Map.of(
                    "comercioId", comercioId,
                    "productos", List.of(
                            Map.of("productoId", productoOk, "cantidad", 2),
                            Map.of("productoId", productoAgotado, "cantidad", 5)));
            ResponseEntity<String> response = rest.exchange("/api/v1/pedidos", HttpMethod.POST,
                    autenticado(body, tokenCliente), String.class);
            return response.getStatusCode().value();
        });

        assertThat(codigos).allMatch(c -> c == 409);
        assertThat(stockActual(productoOk))
                .as("ningun stock del pedido fallido puede descontarse")
                .isEqualTo(stockAntes);
        assertThat(stockActual(productoAgotado)).isEqualTo(1);
    }

    @Test
    void pruebaD_lecturasConcurrentesDevuelven200() throws Exception {
        List<Integer> codigos = ejecutarConcurrently(20, () -> {
            ResponseEntity<String> response = rest.exchange("/api/v1/comercios", HttpMethod.GET,
                    autenticado(null, tokenCliente), String.class);
            return response.getStatusCode().value();
        });

        AtomicInteger ok = new AtomicInteger();
        codigos.forEach(c -> {
            if (c == 200) {
                ok.incrementAndGet();
            }
        });
        assertThat(ok.get()).as("todas las lecturas concurrentes deben responder 200").isEqualTo(20);
    }
}
