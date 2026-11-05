package co.edu.iudigital.tecnomovil.modelo;

import java.util.List;

/**
 * Definición (inmutable) de una ruta del sistema.
 *
 * @param id               identificador de la ruta (T = troncal, A = alimentadora)
 * @param estaciones       estaciones en orden de recorrido
 * @param peso             popularidad relativa usada por el simulador de datos
 * @param capacidadHoraria pasajeros por hora que la ruta puede mover (valor simulado)
 */
public record Ruta(String id, List<String> estaciones, int peso, int capacidadHoraria) {

    public Ruta {
        estaciones = List.copyOf(estaciones); // copia defensiva inmutable
    }
}
