# SaveQuest: héroe, ubicación y nombres de calles

## Uso

Pulsa el botón violeta de ubicación en la esquina inferior derecha del mapa y concede permiso. El héroe aparece en la coordenada recibida y la cámara se centra con zoom 16,5. Puedes desplazar el mapa libremente: las actualizaciones del GPS mueven el héroe, pero no arrastran la cámara. Pulsa otra vez para volver a tu posición.

Si ya existe permiso, el mapa obtiene la posición mientras está visible. El personaje no se muestra en una posición inventada ni utiliza lecturas de más de 30 segundos. Si se pierde la señal, se retira y aparece un mensaje. Se aceptan ubicaciones aproximadas y se indica su margen cuando supera 100 metros. El halo verde es decorativo, no representa un radio exacto de precisión.

Si la ubicación del teléfono está desactivada, el botón abre los ajustes de ubicación. Si se deniega permiso, el mapa sigue disponible. Fuera del área cartográfica de Santiago se permite centrar la coordenada real y se muestra una advertencia sobre la cobertura.

No hay seguimiento en segundo plano ni almacenamiento de la ubicación en Room. Las suscripciones se retiran al pausar/salir del mapa. Los gastos siguen utilizando su selección de ubicación independiente: pulsar este botón no modifica automáticamente la ubicación de un gasto.

## Diseño y cartografía

- Héroe original de 16 × 20 píxeles, dibujado con Canvas: capa violeta, detalles dorados y base verde. Se crea localmente, sin imágenes remotas. Densidad explícita para mantener un tamaño legible en distintas pantallas.
- Nombres de autopistas, avenidas y calles principales (`motorway`, `trunk`, `primary`, `secondary`) desde zoom **11**.
- Nombres de otras calles desde zoom **15**.
- Texto orientado a lo largo de las vías, halo oscuro, colisiones activas y fuentes locales. El motor decide qué nombres caben; no aparecen todos simultáneamente.
- El héroe tiene fuente propia `user-location`, separada de `places` y `movements`; se restaura al conmutar el estilo híbrido.

## Archivos

En `app/src/main/java/com/rpgahorro/savequest/ui/map/`:

- `HeroLocation.kt`: permisos, ciclo de vida, lecturas recientes y solicitud de centrado.
- `HeroLocationIcon.kt`: dibujo original del personaje.
- `OfflineMap.kt`: fuente del héroe, botón y cámara.

También se modifica `app/src/main/assets/map/style.json` para las capas de etiquetas y del héroe. No se regeneran las teselas ni se cambia la base de datos. Los cambios locales que ya existían en otras pantallas y datos se conservan fuera de estos commits.

## Validación

`assembleDebug` correcto y 13 pruebas unitarias aprobadas. Se aprobaron 10 pruebas Android distintas en dos conjuntos: nueve de héroe/mapa/comercios/conmutación y una adicional de nombres de calles. Después del ajuste de escala se repitieron las tres pruebas de héroe y etiquetas, todas aprobadas.

La prueba GPS comprueba que el personaje se dibuja en la posición simulada, sale del centro al desplazar el mapa y regresa al pulsar el botón. Otra prueba comprueba que una lectura antigua no crea un héroe ni satisface una solicitud de centrado. La prueba de etiquetas consulta elementos efectivamente renderizados en zoom 12 y 16,5.

Se revisan capturas reales del emulador y el bitmap exacto utilizado. La prueba de GPS usa un proveedor simulado y restaura su autorización al terminar. Esto no sustituye probar recepción y precisión en un teléfono físico, especialmente con permiso aproximado o sin señal GPS. El funcionamiento online sigue dependiendo del hosting pendiente descrito en el informe del mapa híbrido.

Referencia del estilo: https://maplibre.org/maplibre-style-spec/layers/ .
