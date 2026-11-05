#!/usr/bin/env bash
set -e
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src -name "*.java")
echo "=== PRUEBAS ===" && java -cp out co.edu.iudigital.tecnomovil.PruebasFuncionales
echo && echo "=== PROGRAMA PRINCIPAL ===" && java -Xmx2g -cp out co.edu.iudigital.tecnomovil.Main
