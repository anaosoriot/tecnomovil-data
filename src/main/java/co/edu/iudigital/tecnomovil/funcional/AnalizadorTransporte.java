package co.edu.iudigital.tecnomovil.funcional;

import static co.edu.iudigital.tecnomovil.funcional.Funciones.ES_ENTRADA;
import static co.edu.iudigital.tecnomovil.funcional.Funciones.FRANJA_HORARIA;
import static co.edu.iudigital.tecnomovil.funcional.Funciones.ESTACION;
import static co.edu.iudigital.tecnomovil.funcional.Funciones.HORA_DEL_DIA;
import static co.edu.iudigital.tecnomovil.funcional.Funciones.RUTA;
import static co.edu.iudigital.tecnomovil.funcional.Funciones.deRuta;

import co.edu.iudigital.tecnomovil.modelo.EstadoRuta;
import co.edu.iudigital.tecnomovil.modelo.Registro;
import co.edu.iudigital.tecnomovil.modelo.TramoViaje;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Módulo funcional de procesamiento de datos diarios de TecnoMovil Data.
 *
 * Diseño:
 *  - La instancia es inmutable: guarda una copia inmutable de los registros (List.copyOf).
 *  - Todos los métodos son puros: no modifican la lista, no usan variables compartidas
 *    y devuelven colecciones no modificables.
 *  - Si {@code paralelo} es true, cada pipeline usa parallelStream(); como no hay estado
 *    compartido mutable, el resultado es idéntico al secuencial.
 */
public final class AnalizadorTransporte {

    private final List<Registro> registros;
    private final boolean paralelo;

    public AnalizadorTransporte(List<Registro> registros, boolean paralelo) {
        this.registros = List.copyOf(registros); // copia defensiva inmutable
        this.paralelo = paralelo;
    }

    // =====================================================================
    // a) Afluencia por estación
    // =====================================================================
    public Map<String, Long> afluenciaPorEstacion() {
        return ordenarDescendente(contarPor(ES_ENTRADA, ESTACION), Integer.MAX_VALUE);
    }

    /** Variante que demuestra composición de predicados: ES_ENTRADA.and(deRuta(...)). */
    public Map<String, Long> afluenciaPorEstacionEnRuta(String ruta) {
        return ordenarDescendente(contarPor(ES_ENTRADA.and(deRuta(ruta)), ESTACION), Integer.MAX_VALUE);
    }

    // =====================================================================
    // b) Horas pico y horas valle
    // =====================================================================
    /** Ingresos por hora del día, ordenados por hora (0-23). */
    public Map<Integer, Long> distribucionPorHora() {
        return Collections.unmodifiableMap(new TreeMap<>(contarPor(ES_ENTRADA, HORA_DEL_DIA)));
    }

    /** Ingresos por franja horaria ("07:00-07:59"), en orden cronológico. Usa composición andThen. */
    public Map<String, Long> ingresosPorFranja() {
        return Collections.unmodifiableMap(new TreeMap<>(contarPor(ES_ENTRADA, FRANJA_HORARIA)));
    }

    public Map<Integer, Long> horasPico(int n) {
        return ordenarDescendente(contarPor(ES_ENTRADA, HORA_DEL_DIA), n);
    }

    public Map<Integer, Long> horasValle(int n) {
        Map<Integer, Long> porHora = contarPor(ES_ENTRADA, HORA_DEL_DIA);
        Comparator<Map.Entry<Integer, Long>> ascendente =
                Map.Entry.<Integer, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey());
        return ordenar(porHora, ascendente, n);
    }

    // =====================================================================
    // c) Rutas más utilizadas
    // =====================================================================
    public Map<String, Long> rutasMasUtilizadas(int n) {
        return ordenarDescendente(contarPor(ES_ENTRADA, RUTA), n);
    }

    // =====================================================================
    // d) Patrones de viaje por usuario
    // =====================================================================
    /**
     * Estaciones visitadas por cada usuario, en el orden de sus registros
     * (ordenados por timestamp; el orden es estable ante empates).
     */
    public Map<String, List<String>> patronesDeViaje() {
        return flujo(registros)
                .sorted(Comparator.comparing(Registro::timestamp))
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Registro::idUsuario, TreeMap::new,
                                Collectors.mapping(ESTACION,
                                        Collectors.collectingAndThen(Collectors.toList(), List::copyOf))),
                        Collections::unmodifiableMap));
    }

    // =====================================================================
    // e) Tiempo promedio (minutos) entre estaciones consecutivas
    // =====================================================================
    public Map<String, Double> tiempoPromedioEntreEstaciones() {
        Map<String, List<Registro>> porUsuario =
                flujo(registros).collect(Collectors.groupingBy(Registro::idUsuario));

        return flujo(porUsuario.values())
                .flatMap(AnalizadorTransporte::tramosDeUsuario)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(TramoViaje::etiqueta, TreeMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.averagingLong(TramoViaje::segundos),
                                        segundos -> segundos / 60.0)),
                        Collections::unmodifiableMap));
    }

    /** Función pura: pares consecutivos de registros de un usuario -> tramos válidos. */
    static Stream<TramoViaje> tramosDeUsuario(List<Registro> delUsuario) {
        List<Registro> ordenados = delUsuario.stream()
                .sorted(Comparator.comparing(Registro::timestamp))
                .toList();
        return IntStream.range(0, ordenados.size() - 1)
                .mapToObj(i -> TramoViaje.entre(ordenados.get(i), ordenados.get(i + 1)))
                .flatMap(Optional::stream);
    }

    // =====================================================================
    // f) Detección de sobrecarga en rutas
    // =====================================================================
    /**
     * Marca como crítica la ruta cuya hora de mayor demanda supera el umbral de ocupación.
     *
     * @param umbralPct       porcentaje (0-100) a partir del cual la ruta es crítica
     * @param capacidadHoraria función que entrega la capacidad simulada de cada ruta
     *                         (se inyecta como parámetro: función de orden superior)
     */
    public List<EstadoRuta> detectarSobrecarga(double umbralPct, ToIntFunction<String> capacidadHoraria) {
        Map<String, Map<Integer, Long>> ingresosPorRutaYHora = flujo(registros)
                .filter(ES_ENTRADA)
                .collect(Collectors.groupingBy(RUTA,
                        Collectors.groupingBy(HORA_DEL_DIA, Collectors.counting())));

        return ingresosPorRutaYHora.entrySet().stream()
                .map(e -> estadoDeRuta(e.getKey(), e.getValue(), umbralPct, capacidadHoraria))
                .sorted(Comparator.comparingDouble(EstadoRuta::ocupacionPct).reversed()
                        .thenComparing(EstadoRuta::ruta))
                .toList();
    }

    /** Función pura: calcula el estado de una ruta a partir de sus ingresos por hora. */
    static EstadoRuta estadoDeRuta(String ruta, Map<Integer, Long> porHora,
                                   double umbralPct, ToIntFunction<String> capacidadHoraria) {
        Map.Entry<Integer, Long> pico = porHora.entrySet().stream()
                .max(Map.Entry.<Integer, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .orElseThrow();
        double ocupacion = 100.0 * pico.getValue() / capacidadHoraria.applyAsInt(ruta);
        return new EstadoRuta(ruta, pico.getKey(), pico.getValue(), ocupacion, ocupacion > umbralPct);
    }

    // =====================================================================
    // Utilidades genéricas (funciones de orden superior)
    // =====================================================================

    /** Cuenta registros que cumplen {@code filtro}, agrupados por {@code clasificador}. */
    private <K> Map<K, Long> contarPor(Predicate<Registro> filtro, Function<Registro, K> clasificador) {
        return flujo(registros)
                .filter(filtro)
                .collect(Collectors.groupingBy(clasificador, Collectors.counting()));
    }

    private <T> Stream<T> flujo(Collection<T> coleccion) {
        return paralelo ? coleccion.parallelStream() : coleccion.stream();
    }

    private static <K extends Comparable<K>, V extends Comparable<V>> Map<K, V> ordenarDescendente(
            Map<K, V> mapa, int limite) {
        Comparator<Map.Entry<K, V>> descendente =
                Map.Entry.<K, V>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey());
        return ordenar(mapa, descendente, limite);
    }

    private static <K, V> Map<K, V> ordenar(Map<K, V> mapa, Comparator<Map.Entry<K, V>> criterio, int limite) {
        return mapa.entrySet().stream()
                .sorted(criterio)
                .limit(limite)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (a, b) -> a, LinkedHashMap::new),
                        Collections::unmodifiableMap));
    }
}
