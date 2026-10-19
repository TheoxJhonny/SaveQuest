# SaveQuest: comercios seleccionables sin conexión

## Qué permite esta versión

El mapa incluye **10.809 comercios con nombre** de OpenStreetMap dentro de la zona urbana de Santiago cubierta por la aplicación. Los datos, los símbolos y las etiquetas están incluidos en la instalación; el mapa no necesita una descarga previa ni permiso INTERNET.

- Buscar por nombre o por la parte de dirección disponible.
- Filtrar supermercados/almacenes, comida, farmacias y otras tiendas.
- Ver íconos desde el nivel de zoom 14 y nombres desde el 15. Al acercar el mapa aparecen más etiquetas; se evitan superposiciones para mantenerlo legible.
- Tocar un comercio y ver su nombre, dirección cuando existe y las compras propias registradas allí.
- Elegir **Registrar gasto aquí**: el formulario recibe el comercio, su ubicación y una categoría sugerida editable. La persona ingresa el monto y selecciona la cuenta.
- Si falta un comercio, mantener pulsado el mapa, escribir su nombre y elegir el tipo. Este lugar se conserva cuando se guarda el gasto y puede reutilizarse desde el mapa.

Los supermercados y almacenes usan un carrito verde; los locales de comida, cubiertos naranjas; las farmacias, una cruz azul; las demás tiendas, una bolsa violeta. Los movimientos siguen usando sus marcadores de ingreso y gasto. Si se toca un movimiento asociado a un comercio, su detalle permite abrir ese comercio.

## Integridad del registro

Room pasa de **v7 a v8**. La migración añade cuatro columnas opcionales a `transactions`: `placeId`, `placeName`, `placeCategory` y `placeAddress`, además de un índice por comercio. Mantiene todas las columnas y filas anteriores. Las coordenadas continúan en `latitude` y `longitude`.

Cada gasto conserva una copia del nombre y la dirección que estaban disponibles al registrarlo. El vínculo usa un identificador estable de OpenStreetMap, no el nombre del negocio: dos sucursales con el mismo nombre no se mezclan por ese motivo. Los lugares manuales reciben un identificador propio.

El flujo reutiliza el mismo guardado atómico de movimientos, XP y desafíos. No crea un gasto adicional por venir del mapa. Si se cambia o elimina la ubicación, el formulario desvincula el comercio; el repositorio también rechaza pares de comercio y coordenadas que no coincidan.

## Prueba manual

1. Actualizar la aplicación desde Android Studio sobre la instalación existente, sin borrar sus datos.
2. Abrir **Mapa**, acercarse a una zona de Santiago y buscar un supermercado o filtrar una categoría.
3. Tocar un ícono o un resultado de búsqueda. Confirmar el comercio y pulsar **Registrar gasto aquí**.
4. Comprobar nombre, categoría y ubicación precargados; completar monto y cuenta, y guardar.
5. Verificar un único movimiento en el historial, el descuento en la cuenta y 10 XP base. El desafío puede añadir su recompensa al completarse.
6. Volver al comercio: el resumen debe incluir la compra. Tocar el resumen para consultar los gastos asociados.
7. Mantener pulsado un punto para registrar un local ausente. Guardar un gasto allí y comprobar que el lugar vuelve a estar disponible.
8. Repetir la selección y el registro sin conexión.

## Datos y límites

- Cobertura: latitudes −33.65 a −33.25; longitudes −70.85 a −70.45. No es toda la Región Metropolitana.
- Es un catálogo incluido en esta versión, no un directorio en tiempo real. Pueden faltar comercios, direcciones o cambios recientes. No se muestran horarios, precios o stock.
- Se incluyen locales con nombre o marca de las categorías seleccionadas. Los objetos que representan edificios o áreas usan el centro de su recuadro geográfico; no necesariamente su entrada.
- La identidad se conserva por objeto OSM. Si el mapa de origen contiene dos objetos para un mismo negocio, pueden aparecer dos resultados. No se fusionan automáticamente comercios cercanos, para no mezclar sucursales legítimas.
- Buscar una dirección solo consulta el texto disponible en este catálogo. No es un buscador general de direcciones ni un sistema de rutas.
- Los gastos anteriores sin vínculo no se asignan a negocios por proximidad. Se conservan tal como estaban.
- El resumen de un comercio considera gastos ordinarios vinculados, de todas las fechas. La ubicación de compras compartidas y los filtros financieros por período siguen pendientes.
- Los caracteres de los nombres incluidos y los rangos latinos básicos tienen recursos locales. Un nombre manual con otros alfabetos puede no mostrar todos sus caracteres sobre el mapa; la tarjeta usa la tipografía de Android.

## Archivos y mantenimiento

Los archivos Kotlin nuevos y los modificados para esta etapa se formatearon con **ktfmt 0.54, estilo Kotlin**. No necesitas formatearlos manualmente.

Archivos Kotlin nuevos, dentro de `app/src/main/java/com/rpgahorro/savequest/`:

- `data/model/MapPlace.kt`: modelo validado y categorías.
- `data/repository/PlaceCatalog.kt`: carga del catálogo local fuera del hilo de interfaz, con caché en memoria.
- `data/local/PlaceMigrations.kt`: migración v7→v8.
- `ui/map/PlaceMapData.kt`: conversión de lugares a la fuente del mapa y recuperación del comercio guardado.
- `ui/map/PlaceMapIcons.kt`: cuatro símbolos dibujados localmente y reutilizados.

Pruebas nuevas en `app/src/androidTest/java/com/rpgahorro/savequest/`: `PlacePersistenceTest.kt`, `PlaceMigrationTest.kt` y `PlaceMapUiTest.kt`.

Recursos: `app/src/main/assets/map/places.json`, `places-source.json` y `fonts/`. El catálogo ocupa aproximadamente 1,6 MB y las fuentes/licencias 2,0 MB antes de la compresión del APK.

Herramientas reproducibles en `tools/map/`:

```text
python download_places.py --output <carpeta-temporal>
python download_map_fonts.py --catalog <carpeta-temporal>/places.json --output <carpeta-temporal>/fonts --revision <commit-de-fonts/source.json>
```

La descarga se realiza solo en la computadora de desarrollo. Revisar los datos generados, conservar la atribución, copiar los recursos al proyecto y ejecutar las pruebas antes de actualizar la aplicación. La revisión y las sumas SHA-256 de los archivos de fuente están registradas en `fonts/source.json`.

Datos: © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright), ODbL 1.0. Glifos distribuidos por [MapLibre demotiles](https://github.com/maplibre/demotiles), con sus avisos incluidos; tipografía Noto Sans bajo SIL Open Font License. Las licencias están dentro de los recursos distribuidos.

## Validación realizada — 19 de octubre de 2026

- Compilación `assembleDebug` correcta y 13 pruebas unitarias aprobadas.
- 35 pruebas Android aprobadas entre la ejecución de regresión y las repeticiones focalizadas después de corregir los fallos encontrados. No se presenta como una única ejecución completa posterior a las correcciones.
- Las cuatro pruebas de interfaz nuevas verifican selección real de un marcador, búsqueda y registro completo del gasto, restauración del formulario y creación manual mediante pulsación larga.
- Persistencia y migración v7→v8 verificadas: se conservan registros existentes y se guarda el nombre, categoría, dirección y ubicación del comercio. Un gasto produce un único cargo y +10 XP base.
- Revisión visual en emulador: símbolos y nombres legibles, tarjeta de comercio y atribución sin superposición del botón flotante.
- La prueba visual de movimientos espera el cierre real del diálogo nativo antes de comprobar los píxeles de las calles; conserva la exigencia de cartografía renderizada.

Las pruebas se ejecutaron en un emulador aislado. En esta etapa no se repitió la prueba especial del PIN del dispositivo ni se verificó en un teléfono físico.
