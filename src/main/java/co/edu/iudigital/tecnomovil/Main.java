package co.edu.iudigital.tecnomovil;

import co.edu.iudigital.tecnomovil.datos.Catalogo;
import co.edu.iudigital.tecnomovil.datos.GeneradorDatos;
import co.edu.iudigital.tecnomovil.funcional.AnalizadorTransporte;
import co.edu.iudigital.tecnomovil.modelo.EstadoRuta;
import co.edu.iudigital.tecnomovil.modelo.Registro;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Punto de entrada. Es la "capa imperativa" del programa: aquí (y solo aquí) ocurren
 * los efectos secundarios de entrada/salida (imprimir en consola). Todo el cálculo
 * está en {@link AnalizadorTransporte}, que es puro.
 */
public final class Main {

    private static final int USUARIOS = 200_000;
    private static final long SEMILLA = 2026L;
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 4);
    private static final double UMBRAL_CRITICO_PCT = 85.0;

    public static void main(String[] args) {
        System.out.println("=== TecnoMovil Data - Procesamiento funcional de transporte urbano ===");

        List<Registro> registros = GeneradorDatos.generar(USUARIOS, SEMILLA, FECHA);
        System.out.printf("Registros simulados: %,d (usuarios: %,d, fecha: %s)%n%n",
                registros.size(), USUARIOS, FECHA);

        System.out.println("Primeros 6 registros:");
        registros.stream().limit(6).forEach(r -> System.out.printf(
                "  %s | %s | %-15s | %-7s | %s%n",
                r.idUsuario(), r.ruta(), r.estacion(), r.accion().texto(), r.timestamp()));

        verificarInmutabilidad(registros);

        AnalizadorTransporte secuencial = new AnalizadorTransporte(registros, false);

        System.out.println("\n--- a) Afluencia por estacion (ingresos del dia) ---");
        secuencial.afluenciaPorEstacion()
                .forEach((estacion, total) -> System.out.printf("  %-16s %,9d%n", estacion, total));
        System.out.println("  Afluencia en ruta T1 (ES_ENTRADA.and(deRuta)):");
        secuencial.afluenciaPorEstacionEnRuta("T1")
                .forEach((estacion, total) -> System.out.printf("    %-16s %,9d%n", estacion, total));

        System.out.println("\n--- b) Horas pico y horas valle ---");
        System.out.println("  Top 5 horas pico:");
        imprimirHoras(secuencial.horasPico(5));
        System.out.println("  3 horas valle:");
        imprimirHoras(secuencial.horasValle(3));
        System.out.println("  Distribucion completa por franja:");
        Map<String, Long> franjas = secuencial.ingresosPorFranja();
        long maximo = franjas.values().stream().mapToLong(Long::longValue).max().orElse(1);
        franjas.forEach((franja, total) -> System.out.printf("    %s %-40s %,8d%n",
                franja, "#".repeat((int) (40 * total / maximo)), total));

        System.out.println("\n--- c) Rutas mas utilizadas ---");
        secuencial.rutasMasUtilizadas(8)
                .forEach((ruta, total) -> System.out.printf("  %-4s %,9d%n", ruta, total));

        System.out.println("\n--- d) Patrones de viaje (primeros 5 usuarios) ---");
        secuencial.patronesDeViaje().entrySet().stream().limit(5)
                .forEach(e -> System.out.printf("  %s -> %s%n", e.getKey(), e.getValue()));

        System.out.println("\n--- e) Tiempo promedio entre estaciones (min, primeros 10 tramos) ---");
        secuencial.tiempoPromedioEntreEstaciones().entrySet().stream().limit(10)
                .forEach(e -> System.out.printf("  %-45s %5.2f%n", e.getKey(), e.getValue()));

        System.out.printf("%n--- f) Deteccion de sobrecarga (umbral %.0f%% de capacidad) ---%n",
                UMBRAL_CRITICO_PCT);
        secuencial.detectarSobrecarga(UMBRAL_CRITICO_PCT, Catalogo.CAPACIDAD_POR_RUTA::apply)
                .forEach(Main::imprimirEstadoRuta);

        compararSecuencialYParalelo(registros);
    }

    private static void imprimirHoras(Map<Integer, Long> horas) {
        horas.forEach((hora, total) ->
                System.out.printf("    %02d:00-%02d:59 %,9d%n", hora, hora, total));
    }

    private static void imprimirEstadoRuta(EstadoRuta e) {
        System.out.printf("  %-4s hora critica %02d:00 | pico %,7d pasajeros | ocupacion %6.1f%% | %s%n",
                e.ruta(), e.horaCritica(), e.pasajerosPico(), e.ocupacionPct(),
                e.critica() ? "CRITICA" : "normal");
    }

    /** Demuestra que la colección original es inmutable. */
    private static void verificarInmutabilidad(List<Registro> registros) {
        try {
            registros.add(registros.get(0));
            System.out.println("\n[ERROR] la lista original se pudo modificar");
        } catch (UnsupportedOperationException e) {
            System.out.println("\nInmutabilidad: la lista original rechaza modificaciones "
                    + "(UnsupportedOperationException).");
        }
    }

    /** Ejecuta todo el análisis en modo secuencial y paralelo y compara resultados y tiempos. */
    private static void compararSecuencialYParalelo(List<Registro> registros) {
        System.out.println("\n--- Secuencial vs paralelo (stream() vs parallelStream()) ---");
        var secuencial = new AnalizadorTransporte(registros, false);
        var paralelo = new AnalizadorTransporte(registros, true);

        ejecutarTodo(secuencial); // calentamiento de la JVM
        ejecutarTodo(paralelo);

        long t0 = System.nanoTime();
        var resSecuencial = ejecutarTodo(secuencial);
        long t1 = System.nanoTime();
        var resParalelo = ejecutarTodo(paralelo);
        long t2 = System.nanoTime();

        System.out.printf("  Secuencial: %d ms%n", (t1 - t0) / 1_000_000);
        System.out.printf("  Paralelo:   %d ms%n", (t2 - t1) / 1_000_000);
        System.out.println("  Resultados identicos: " + resSecuencial.equals(resParalelo));
        System.out.printf("  Procesadores disponibles: %d%n", Runtime.getRuntime().availableProcessors());
    }

    private static List<Object> ejecutarTodo(AnalizadorTransporte a) {
        return List.of(
                a.afluenciaPorEstacion(),
                a.distribucionPorHora(),
                a.rutasMasUtilizadas(8),
                a.patronesDeViaje(),
                a.tiempoPromedioEntreEstaciones(),
                a.detectarSobrecarga(UMBRAL_CRITICO_PCT, Catalogo.CAPACIDAD_POR_RUTA::apply));
    }

    private Main() {
    }
}
