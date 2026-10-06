package com.fastorder.enums;

public enum EstadoPedido {
    PENDIENTE,
    EN_PREPARACION,
    EN_CAMINO,
    ENTREGADO,
    CANCELADO;

    public boolean puedeTransicionarA(EstadoPedido destino) {
        return switch (this) {
            case PENDIENTE -> destino == EN_PREPARACION || destino == CANCELADO;
            case EN_PREPARACION -> destino == EN_CAMINO;
            case EN_CAMINO -> destino == ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}
