package co.edu.iudigital.tecnomovil.modelo;

/**
 * Resultado del análisis de sobrecarga para una ruta.
 *
 * @param ruta          identificador de la ruta
 * @param horaCritica   hora del día con más ingresos en la ruta
 * @param pasajerosPico ingresos registrados en esa hora
 * @param ocupacionPct  pasajerosPico / capacidadHoraria * 100
 * @param critica       true si la ocupación supera el umbral
 */
public record EstadoRuta(String ruta, int horaCritica, long pasajerosPico,
                         double ocupacionPct, boolean critica) {
}
