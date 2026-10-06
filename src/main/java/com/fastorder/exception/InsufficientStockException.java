package com.fastorder.exception;

public class InsufficientStockException extends RuntimeException {

    private final String productoNombre;
    private final int stockDisponible;
    private final int cantidadSolicitada;

    public InsufficientStockException(String productoNombre, int stockDisponible, int cantidadSolicitada) {
        super("Stock insuficiente para el producto '" + productoNombre + "': disponible "
                + stockDisponible + ", solicitado " + cantidadSolicitada);
        this.productoNombre = productoNombre;
        this.stockDisponible = stockDisponible;
        this.cantidadSolicitada = cantidadSolicitada;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public int getStockDisponible() {
        return stockDisponible;
    }

    public int getCantidadSolicitada() {
        return cantidadSolicitada;
    }
}
