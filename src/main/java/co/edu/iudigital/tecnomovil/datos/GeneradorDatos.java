package co.edu.iudigital.tecnomovil.datos;

import co.edu.iudigital.tecnomovil.modelo.Accion;
import co.edu.iudigital.tecnomovil.modelo.Registro;
import co.edu.iudigital.tecnomovil.modelo.Ruta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Simulador de registros de validadores.
 *
 * Es determinista: dadas las mismas entradas (cantidad, semilla, fecha) produce
 * siempre la misma lista. Cada viaje se calcula como función pura de
 * (semilla, usuario, número de viaje), por lo que la generación puede ser paralela.
 */
public final class GeneradorDatos {

    /** Horas de inicio de viaje; las repeticiones modelan las horas pico (7-8 y 17-18). */
    private static final List<Integer> HORAS_PONDERADAS = List.of(
            5, 6, 6, 7, 7, 7, 7, 8, 8, 8, 9, 10, 11, 12, 12,
            13, 14, 15, 16, 17, 17, 17, 18, 18, 18, 19, 20, 21);

    /** Rutas repetidas según su peso: un índice al azar equivale a un sorteo ponderado. */
    private static final List<Ruta> RUTAS_PONDERADAS = Catalogo.RUTAS.stream()
            .flatMap(r -> Collections.nCopies(r.peso(), r).stream())
            .collect(Collectors.toUnmodifiableList());

    private GeneradorDatos() {
    }

    public static List<Registro> generar(int cantidadUsuarios, long semilla, LocalDate fecha) {
        return IntStream.range(0, cantidadUsuarios)
                .parallel()
                .boxed()
                .flatMap(u -> viajesDeUsuario(u, semilla, fecha))
                .toList(); // lista inmutable; el flujo ordenado conserva el orden de los usuarios
    }

    private static Stream<Registro> viajesDeUsuario(int usuario, long semilla, LocalDate fecha) {
        String id = String.format("U%07d", usuario);
        SplittableRandom rnd = new SplittableRandom(mezclar(semilla, usuario, -1));
        int cantidadViajes = 2 + rnd.nextInt(3); // 2..4 viajes por usuario

        // Horas distintas para que los viajes de un mismo usuario no se solapen.
        List<Integer> horas = rnd.ints(8, 0, HORAS_PONDERADAS.size())
                .boxed()
                .map(HORAS_PONDERADAS::get)
                .distinct()
                .limit(cantidadViajes)
                .toList();

        return IntStream.range(0, horas.size())
                .boxed()
                .flatMap(v -> viaje(id, horas.get(v),
                        new SplittableRandom(mezclar(semilla, usuario, v)), fecha));
    }

    private static Stream<Registro> viaje(String idUsuario, int hora, SplittableRandom rnd, LocalDate fecha) {
        LocalDateTime inicio = fecha.atTime(hora, rnd.nextInt(50), rnd.nextInt(60));

        Ruta ruta = RUTAS_PONDERADAS.get(rnd.nextInt(RUTAS_PONDERADAS.size()));
        int origen = rnd.nextInt(ruta.estaciones().size() - 1);
        int destino = origen + 1; // estaciones consecutivas

        boolean horaPico = hora == 7 || hora == 8 || hora == 17 || hora == 18;
        int minutosViaje = 2 + rnd.nextInt(4) + (horaPico ? 3 : 0);

        return Stream.of(
                new Registro(idUsuario, ruta.id(), ruta.estaciones().get(origen),
                        Accion.ENTRADA, inicio),
                new Registro(idUsuario, ruta.id(), ruta.estaciones().get(destino),
                        Accion.SALIDA, inicio.plusMinutes(minutosViaje)));
    }

    /** Mezcla de bits (tipo splitmix) para obtener semillas independientes por viaje. */
    private static long mezclar(long semilla, int usuario, int viaje) {
        long z = semilla + usuario * 0x9E3779B97F4A7C15L + (viaje + 1) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
