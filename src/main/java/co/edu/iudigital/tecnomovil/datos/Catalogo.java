package co.edu.iudigital.tecnomovil.datos;

import co.edu.iudigital.tecnomovil.modelo.Ruta;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catálogo inmutable de rutas y estaciones de TecnoValle (datos simulados).
 * Se usa List.of para que el orden de iteración sea estable entre ejecuciones.
 */
public final class Catalogo {

    public static final List<Ruta> RUTAS = List.of(
            new Ruta("T1", List.of("Terminal Norte", "Plaza Mayor", "Centro Civico",
                    "Universidad", "Hospital Central", "Estacion Sur"), 10, 17000),
            new Ruta("T2", List.of("Aeropuerto", "Zona Franca", "Plaza Mayor",
                    "Mercado Central", "Biblioteca", "Estadio"), 8, 17000),
            new Ruta("T3", List.of("Ciudadela", "Parque Tecno", "Centro Civico",
                    "Mercado Central", "Puerto Seco", "Estacion Sur"), 8, 14000),
            new Ruta("T4", List.of("Terminal Norte", "Universidad", "Biblioteca",
                    "Parque Tecno", "Zona Franca", "Ciudadela"), 6, 12000),
            new Ruta("A1", List.of("Barrio Norte", "Terminal Norte", "Plaza Mayor",
                    "Universidad"), 5, 8000),
            new Ruta("A2", List.of("Villa Sur", "Estacion Sur", "Hospital Central",
                    "Biblioteca"), 4, 7500),
            new Ruta("A3", List.of("Altos del Rio", "Puerto Seco", "Mercado Central",
                    "Centro Civico"), 3, 6000),
            new Ruta("A4", List.of("Loma Verde", "Ciudadela", "Parque Tecno",
                    "Estadio"), 2, 5000));

    /** Capacidad horaria por ruta, derivada del catálogo (función de consulta, sin estado). */
    private static final Map<String, Integer> CAPACIDADES = RUTAS.stream()
            .collect(Collectors.toUnmodifiableMap(Ruta::id, Ruta::capacidadHoraria));

    public static final Function<String, Integer> CAPACIDAD_POR_RUTA = CAPACIDADES::get;

    private Catalogo() {
    }
}
