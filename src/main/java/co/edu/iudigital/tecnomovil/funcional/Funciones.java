package co.edu.iudigital.tecnomovil.funcional;

import co.edu.iudigital.tecnomovil.modelo.Accion;
import co.edu.iudigital.tecnomovil.modelo.Registro;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Biblioteca de funciones puras reutilizables (lambdas con nombre).
 * Ninguna modifica estado externo: el resultado depende solo de los argumentos.
 */
public final class Funciones {

    // ---- Predicados ----
    public static final Predicate<Registro> ES_ENTRADA = r -> r.accion() == Accion.ENTRADA;
    public static final Predicate<Registro> ES_SALIDA = r -> r.accion() == Accion.SALIDA;

    /** Fábrica de predicados: función de orden superior que RETORNA una función. */
    public static Predicate<Registro> deRuta(String ruta) {
        return r -> r.ruta().equals(ruta);
    }

    // ---- Clasificadores (funciones de extracción) ----
    public static final Function<Registro, String> ESTACION = Registro::estacion;
    public static final Function<Registro, String> RUTA = Registro::ruta;
    public static final Function<Registro, Integer> HORA_DEL_DIA = r -> r.timestamp().getHour();

    // ---- Composición de funciones (andThen) ----
    public static final Function<Registro, String> FRANJA_HORARIA =
            HORA_DEL_DIA.andThen(h -> String.format("%02d:00-%02d:59", h, h));

    private Funciones() {
    }
}
