package co.edu.iudigital.tecnomovil;

import co.edu.iudigital.tecnomovil.funcional.AnalizadorTransporte;
import co.edu.iudigital.tecnomovil.modelo.Accion;
import co.edu.iudigital.tecnomovil.modelo.EstadoRuta;
import co.edu.iudigital.tecnomovil.modelo.Registro;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Pruebas manuales (sin frameworks externos) sobre un conjunto pequeño y verificable a mano.
 * Ejecutar con: java -cp out co.edu.iudigital.tecnomovil.PruebasFuncionales
 */
public final class PruebasFuncionales {

    private static int exitosas = 0;

    public static void main(String[] args) {
        List<Registro> datos = List.of(
                reg("U1", "T1", "Plaza Mayor", Accion.ENTRADA, 7, 0),
                reg("U1", "T1", "Centro Civico", Accion.SALIDA, 7, 5),
                reg("U2", "T1", "Plaza Mayor", Accion.ENTRADA, 7, 10),
                reg("U2", "T1", "Centro Civico", Accion.SALIDA, 7, 16),
                reg("U3", "T2", "Plaza Mayor", Accion.ENTRADA, 8, 0),
                reg("U3", "T2", "Mercado Central", Accion.SALIDA, 8, 4));
        List<Registro> copiaOriginal = List.copyOf(datos);

        for (boolean paralelo : new boolean[]{false, true}) {
            String modo = paralelo ? "[paralelo] " : "[secuencial] ";
            AnalizadorTransporte a = new AnalizadorTransporte(datos, paralelo);

            comprobar(modo + "afluencia por estacion",
                    Map.of("Plaza Mayor", 3L), a.afluenciaPorEstacion());
            comprobar(modo + "horas pico (7h=2, 8h=1)",
                    Map.of(7, 2L, 8, 1L), a.horasPico(2));
            comprobar(modo + "rutas mas utilizadas",
                    Map.of("T1", 2L, "T2", 1L), a.rutasMasUtilizadas(5));
            comprobar(modo + "patron de U1",
                    List.of("Plaza Mayor", "Centro Civico"), a.patronesDeViaje().get("U1"));
            comprobar(modo + "tiempo promedio T1 Plaza->Centro (5.5 min)",
                    5.5, a.tiempoPromedioEntreEstaciones().get("T1: Plaza Mayor -> Centro Civico"));

            List<EstadoRuta> estados = a.detectarSobrecarga(80.0, ruta -> ruta.equals("T1") ? 2 : 10);
            comprobar(modo + "T1 critica (100% > 80%)", true, estadoDe(estados, "T1").critica());
            comprobar(modo + "T2 normal (10% < 80%)", false, estadoDe(estados, "T2").critica());
        }

        comprobar("lista original no fue modificada", copiaOriginal, datos);
        System.out.printf("%nPruebas exitosas: %d%n", exitosas);
    }

    private static EstadoRuta estadoDe(List<EstadoRuta> estados, String ruta) {
        return estados.stream().filter(e -> e.ruta().equals(ruta)).findFirst().orElseThrow();
    }

    private static Registro reg(String u, String ruta, String est, Accion acc, int h, int m) {
        return new Registro(u, ruta, est, acc, LocalDateTime.of(2026, 11, 4, h, m));
    }

    private static void comprobar(String nombre, Object esperado, Object obtenido) {
        if (!esperado.equals(obtenido)) {
            throw new AssertionError(nombre + " -> esperado " + esperado + " pero fue " + obtenido);
        }
        exitosas++;
        System.out.println("  OK  " + nombre);
    }

    private PruebasFuncionales() {
    }
}
