package com.fastorder.dto;

import com.fastorder.enums.EstadoPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
public class PedidoResponse {

    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private Long repartidorId;
    private String repartidorNombre;
    private LocalDateTime fechaPedido;
    private BigDecimal costoEnvio;
    private BigDecimal montoTotal;
    private EstadoPedido estado;
    private List<DetallePedidoResponse> detalles;
}
