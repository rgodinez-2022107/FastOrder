package com.fastorder.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoPedidoTest {

    @Test
    void flujoNormalPermitido() {
        assertThat(EstadoPedido.PENDIENTE.puedeTransicionarA(EstadoPedido.EN_PREPARACION)).isTrue();
        assertThat(EstadoPedido.EN_PREPARACION.puedeTransicionarA(EstadoPedido.EN_CAMINO)).isTrue();
        assertThat(EstadoPedido.EN_CAMINO.puedeTransicionarA(EstadoPedido.ENTREGADO)).isTrue();
    }

    @Test
    void cancelacionSoloDesdePendiente() {
        assertThat(EstadoPedido.PENDIENTE.puedeTransicionarA(EstadoPedido.CANCELADO)).isTrue();
        assertThat(EstadoPedido.EN_PREPARACION.puedeTransicionarA(EstadoPedido.CANCELADO)).isFalse();
        assertThat(EstadoPedido.EN_CAMINO.puedeTransicionarA(EstadoPedido.CANCELADO)).isFalse();
        assertThat(EstadoPedido.ENTREGADO.puedeTransicionarA(EstadoPedido.CANCELADO)).isFalse();
    }

    @Test
    void estadosFinalesNoTransicionan() {
        for (EstadoPedido destino : EstadoPedido.values()) {
            assertThat(EstadoPedido.ENTREGADO.puedeTransicionarA(destino)).isFalse();
            assertThat(EstadoPedido.CANCELADO.puedeTransicionarA(destino)).isFalse();
        }
    }

    @Test
    void transicionesSaltadasNoPermitidas() {
        assertThat(EstadoPedido.PENDIENTE.puedeTransicionarA(EstadoPedido.EN_CAMINO)).isFalse();
        assertThat(EstadoPedido.PENDIENTE.puedeTransicionarA(EstadoPedido.ENTREGADO)).isFalse();
        assertThat(EstadoPedido.EN_PREPARACION.puedeTransicionarA(EstadoPedido.ENTREGADO)).isFalse();
        assertThat(EstadoPedido.EN_PREPARACION.puedeTransicionarA(EstadoPedido.PENDIENTE)).isFalse();
        assertThat(EstadoPedido.EN_CAMINO.puedeTransicionarA(EstadoPedido.EN_PREPARACION)).isFalse();
    }

    @Test
    void ningunEstadoTransicionaASiMismo() {
        for (EstadoPedido actual : EstadoPedido.values()) {
            assertThat(actual.puedeTransicionarA(actual)).isFalse();
        }
    }
}
