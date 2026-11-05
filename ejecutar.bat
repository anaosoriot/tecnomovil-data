@echo off
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > fuentes.txt
javac -encoding UTF-8 -d out @fuentes.txt
del fuentes.txt
echo === PRUEBAS ===
java -cp out co.edu.iudigital.tecnomovil.PruebasFuncionales
echo === PROGRAMA PRINCIPAL ===
java -Xmx2g -cp out co.edu.iudigital.tecnomovil.Main
