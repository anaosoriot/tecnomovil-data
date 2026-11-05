package co.edu.iudigital.tecnomovil.modelo;

/**
 * Acciones posibles de un registro de validación.
 * Se modela como enum (tipo cerrado e inmutable) en lugar de un String libre.
 */
public enum Accion {
    ENTRADA("entrada"),
    SALIDA("salida");

    private final String texto;

    Accion(String texto) {
        this.texto = texto;
    }

    public String texto() {
        return texto;
    }

    /** Función pura: convierte "entrada"/"salida" en la constante correspondiente. */
    public static Accion desdeTexto(String texto) {
        return switch (texto.toLowerCase()) {
            case "entrada" -> ENTRADA;
            case "salida" -> SALIDA;
            default -> throw new IllegalArgumentException("Acción desconocida: " + texto);
        };
    }
}
