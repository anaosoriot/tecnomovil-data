package co.edu.iudigital.tecnomovil.modelo;

import java.time.Duration;
import java.util.Optional;

/**
 * Tramo recorrido por un usuario entre dos estaciones consecutivas de sus registros.
 */
public record TramoViaje(String ruta, String origen, String destino, long segundos) {

    /**
     * Función pura: un par de registros consecutivos forma un tramo válido solo si
     * pertenecen a la misma ruta, el primero es ENTRADA, el segundo es SALIDA y
     * las estaciones son distintas.
     */
    public static Optional<TramoViaje> entre(Registro a, Registro b) {
        boolean valido = a.ruta().equals(b.ruta())
                && a.accion() == Accion.ENTRADA
                && b.accion() == Accion.SALIDA
                && !a.estacion().equals(b.estacion());
        return valido
                ? Optional.of(new TramoViaje(a.ruta(), a.estacion(), b.estacion(),
                        Duration.between(a.timestamp(), b.timestamp()).getSeconds()))
                : Optional.empty();
    }

    /** Etiqueta legible usada como llave de agrupación. */
    public String etiqueta() {
        return ruta + ": " + origen + " -> " + destino;
    }
}
