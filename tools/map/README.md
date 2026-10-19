# Mapa offline de Santiago

SaveQuest incluye un extracto de calles, parques (polígonos simples) y cursos de agua de OpenStreetMap. Se dibuja con MapLibre Native Android 13.7.1 usando únicamente assets locales. No se descargan teselas públicas ni se necesita una caché previa. El manifiesto elimina el permiso INTERNET que pudiera aportar una dependencia.

## Datos y licencia

© OpenStreetMap contributors. Datos distribuidos bajo [Open Database License 1.0](https://opendatacommons.org/licenses/odbl/1-0/). Información de atribución: https://www.openstreetmap.org/copyright.

El extracto adaptado se distribuye bajo la misma licencia ODbL. Su forma editable es `app/src/main/assets/map/santiago.geojson`; incluye las geometrías y propiedades utilizadas por la aplicación. La licencia de los datos no cambia automáticamente la licencia del código de la aplicación.

`source.json` conserva consulta, fecha, origen y cantidad de elementos. Se redondean coordenadas a seis decimales, se seleccionan propiedades de presentación y se excluyen parques que no sean polígonos cerrados simples.

## Cobertura y funcionamiento

- Rectángulo: latitudes −33.65 a −33.25; longitudes −70.85 a −70.45. Es una zona urbana de Santiago, no toda la Región Metropolitana.
- Zoom y desplazamiento táctiles; calles violetas, parques verdes, agua azul.
- Tocar una calle muestra su nombre cuando existe en los datos. No se incluyen etiquetas de texto sobre todas las calles, búsqueda de direcciones ni cálculo de rutas.
- Los movimientos con coordenadas aparecen como marcadores. Verde: ingreso; rosado: gasto. Los movimientos anteriores sin coordenadas permanecen sin cambios.
- El formulario permite elegir un punto o pedir una lectura puntual de ubicación. Se puede negar el permiso, quitar la ubicación o guardar sin ella. No hay seguimiento en segundo plano.
- La cartografía permanece fija hasta regenerar los assets y compilar una nueva versión. No se envían movimientos, coordenadas o saldos a OpenStreetMap.
- El GPS puede funcionar sin conexión, pero la ubicación aproximada del proveedor de red depende del teléfono y de su conectividad. La selección manual siempre funciona offline dentro de la cobertura.
- Un movimiento ubicado fuera del rectángulo conserva sus coordenadas, aunque no tiene cartografía local en esta demostración.

## Regeneración manual

Requiere Python 3 y conexión a Overpass API solo en la computadora de desarrollo. Ejecutar `python tools/map/download_santiago.py`; el script escribe en su propia carpeta. Revisar `source.json` y copiar `santiago.geojson` y `source.json` a `app/src/main/assets/map/`. No modifica el estilo. Revisar cobertura, tamaño y renderizado antes de confirmar el cambio. Nunca automatizar descargas frecuentes desde la app.

## Validación

`OfflineMapTest` comprueba ausencia de permiso INTERNET, carga del estilo local, selección de un marcador real renderizado y detalle de un movimiento. `LocationPersistenceTest` comprueba coordenadas opcionales, rechazo de pares inválidos, saldo y XP. Estas pruebas usan datos ficticios o bases en memoria.

La precisión del sensor y el flujo de permiso se deben comprobar también en el teléfono físico. Si se superponen varios movimientos, al tocar el lugar se abre una lista para elegir el detalle.

## Referencias de implementación

- https://maplibre.org/maplibre-native/android/examples/geojson-guide/
- https://maplibre.org/maplibre-native/android/examples/getting-started/
- https://operations.osmfoundation.org/policies/tiles/

## Comercios seleccionables (Room v8)

El mapa ahora incluye un catálogo local de comercios, símbolos y nombres. Consulta docs/comercios-offline.md para el flujo, los límites, la migración y la regeneración con download_places.py y download_map_fonts.py. Los metadatos están en app/src/main/assets/map/places-source.json y fonts/source.json. No se requiere permiso INTERNET en la aplicación.
