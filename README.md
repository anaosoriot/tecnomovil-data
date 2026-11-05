# TecnoMovil Data – Procesamiento funcional de transporte urbano

Solución de la **EA2 – Creación de repositorio** (Tecnología en Desarrollo de Software, IU Digital).
Módulo en **Java 17+ sin frameworks externos** que procesa ~1,2 millones de registros simulados de
validadores usando **Streams, lambdas, funciones puras y colecciones inmutables**.

## Requerimientos cubiertos

| Req. | Método (`AnalizadorTransporte`)                         |
|------|---------------------------------------------------------|
| a)   | `afluenciaPorEstacion()`                                |
| b)   | `horasPico(n)`, `horasValle(n)`, `distribucionPorHora()`|
| c)   | `rutasMasUtilizadas(n)`                                 |
| d)   | `patronesDeViaje()`                                     |
| e)   | `tiempoPromedioEntreEstaciones()`                       |
| f)   | `detectarSobrecarga(umbral, capacidadPorRuta)`          |

## Estructura

```
src/main/java/co/edu/iudigital/tecnomovil/
  Main.java                      # capa imperativa: imprime resultados
  modelo/    Accion, Registro, Ruta, TramoViaje, EstadoRuta   (records inmutables)
  datos/     Catalogo, GeneradorDatos                         (datos simulados deterministas)
  funcional/ Funciones, AnalizadorTransporte                  (núcleo funcional puro)
src/test/java/co/edu/iudigital/tecnomovil/PruebasFuncionales.java
```

## Compilar y ejecutar

Requiere JDK 17 o superior (`javac -version`).

Linux / macOS:
```bash
./ejecutar.sh          # compila, corre pruebas y programa principal
```
Windows:
```bat
ejecutar.bat
```
Manual:
```bash
mkdir out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out co.edu.iudigital.tecnomovil.PruebasFuncionales
java -Xmx2g -cp out co.edu.iudigital.tecnomovil.Main
```

## Principios funcionales aplicados

- **Funciones puras:** `TramoViaje.entre`, `estadoDeRuta`, `Funciones.*`; mismo input ⇒ mismo output, sin efectos.
- **Inmutabilidad:** `record`, `List.copyOf`, `List.of`, `Stream.toList()`, `Collections.unmodifiableMap`.
- **Orden superior:** `contarPor(filtro, clasificador)`, `deRuta(ruta)`, `detectarSobrecarga(umbral, capacidad)`.
- **Lambdas y Streams:** todos los requerimientos son pipelines declarativos (`filter`, `groupingBy`, `sorted`, `flatMap`…).
- **Paralelización:** `new AnalizadorTransporte(registros, true)` usa `parallelStream()`; resultados idénticos.
