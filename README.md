# AloNotes

App Android personal de apuntes (estilo Penly Notes) con el aspecto de Alouncher.
Kotlin, `com.alonotes.app`, minSdk 26, targetSdk 34. Sin librerías extra (solo recyclerview y core).

## Fase 1 — Interfaz
- Biblioteca "Mis notas": carpetas y notas en cuadrícula, migas de pan, menú flotante (+):
  Nuevo documento, Importar documento (próximamente), Crear carpeta. Renombrar y eliminar funcionan.
- Editor: barra de herramientas completa (todo "Próximamente" salvo volver y crear páginas),
  diálogo "Crear páginas nuevas" (A5/A4/A3/A2, orientación, color, cantidad) y contador de páginas.
- Ajustes: color de acento (12 colores) y opciones "Próximamente".
- Ícono: A cursiva dibujada de un solo trazo que termina en un broche.

Datos: `SharedPreferences "notes"` (clave `items`, JSON) y `"settings"` (clave `accent`).
No cambiar `applicationId`.

## Fase 1b — Kotlin y limpieza
- Proyecto migrado de Java a Kotlin (Kotlin 1.9.24, AGP 8.5.2). Mismos formatos de datos y comportamiento.
- Eliminado el botón flotante de lápiz del editor (el lápiz del borde es el panel edge de la tablet).
- versionCode 2 / versionName 0.1.1.

## Compilar
Sube el proyecto a GitHub: el workflow `.github/workflows/build.yml` (Build APK, JDK 17, Gradle 8.9)
genera el APK en la pestaña Actions → artefacto `AloNotes-debug`.
