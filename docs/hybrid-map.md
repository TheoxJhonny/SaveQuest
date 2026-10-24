# SaveQuest: mapa híbrido PMTiles

Implementación del 24 de octubre de 2026 en el repositorio real `C:\Users\Gamer\AndroidStudioProjects\SaveQuest`, rama `feature/hybrid-map-pmtiles`. Se partió de `796275b`. No se utilizó el ZIP antiguo.

## Resultado y alcance

Mapa base vectorial local operativo, con selección online preparada mediante una URL HTTPS explícita. La configuración entregada tiene `onlinePmtilesUrl` vacío: abre inmediatamente el archivo incluido, sin depender de caché ni de una descarga inicial. No se ha contratado hosting ni inventado una URL.

MapLibre Android permanece en **13.7.1**. Esta versión admite PMTiles locales mediante `pmtiles://asset://...` y remotos mediante `pmtiles://https://...`; no necesita un servidor HTTP dentro de la app. Referencia oficial: https://maplibre.org/maplibre-native/android/examples/data/PMTiles/ .

Los cambios locales preexistentes en `places.json` y el ZIP sin seguimiento se conservaron y se excluyeron de los commits. Huella SHA-256 del catálogo antes y después: `029D900444F47310477611B7D01133900A5F63C167C1A1513A31A6A0278ACEE5`.

## Arquitectura y archivos

| Archivo relativo al repositorio | Cambio |
|---|---|
| `app/src/main/assets/map/santiago.pmtiles` | 1.870 teselas MVT, zoom 10–15; sobrezoom hasta 18. |
| `app/src/main/assets/map/style.json` | Fuente vectorial `santiago`, capa de origen `santiago`; mismos filtros, grosores, opacidades y colores. |
| `app/src/main/assets/map/hybrid-config.json` | Campo `onlinePmtilesUrl` vacío, pendiente de proveedor. |
| `app/src/main/java/com/rpgahorro/savequest/ui/map/HybridMapLoader.kt` | Sonda HTTP Range, selección, recuperación offline, protección de callbacks y registro de tiempos. |
| `app/src/main/java/com/rpgahorro/savequest/ui/map/OfflineMap.kt` | Conserva MapView y cámara; restaura fuentes dinámicas e iconos al cargar un estilo; evita serializaciones y envíos GeoJSON repetidos. |
| `app/src/main/java/com/rpgahorro/savequest/ui/map/MapScreen.kt` | Limpia error al recuperarse y usa texto de disponibilidad offline válido para ambos modos. |
| `app/src/main/AndroidManifest.xml` | Habilita INTERNET para HTTPS. |
| `app/build.gradle.kts` | PMTiles sin compresión ZIP para acceso aleatorio; excluye solamente `santiago.geojson` del APK. |
| `tools/map/build_pmtiles.py`, `validate_tiles.py`, `requirements-tiles.txt` | Generación y comprobación reproducibles. |
| `app/src/androidTest/.../HybridMapTest.kt`, `OfflineMapTest.kt`, `PlaceMapUiTest.kt` | Conmutación, renderizado, selección, cámara y adaptación al permiso Internet. |

Los colores siguen siendo `#10131E`, `#174B43`, `#3A8AA3`, `#55516F` y `#A494DA`. `places` y `movements` continúan siendo fuentes GeoJSON dinámicas independientes. Iconos y fuentes tipográficas siguen incluidos localmente. No hay cambios de esquema Room, repositorios de gastos, navegación, GPS o selección manual.

Al abrir el mapa se comprueba el endpoint configurado con un GET Range de los primeros 16.384 bytes. Se exige HTTPS, respuesta **206**, Content-Range correspondiente y contenido idéntico al prefijo del archivo local. Se rechazan redirecciones, protocolos distintos, credenciales en la URL y archivos diferentes detectables por ese prefijo. No equivale a verificar criptográficamente todo el objeto remoto: el operador debe subir el archivo exacto.

Si pasa la sonda, se usa online. Un error de carga, 12 segundos sin completar el renderizado tras cargar/mover la cámara, o una sonda periódica fallida activa offline. La sonda periódica compara solamente los primeros 127 bytes cada 15 segundos. Los tiempos de conexión y lectura son de 2,5 segundos cada uno; DNS y planificación del sistema pueden añadir tiempo. Una conexión muy lenta puede provocar una caída conservadora a offline.

La caída se permite una sola vez por instancia del mapa: no se vuelve a online hasta otra apertura. El estilo se recarga dentro del mismo MapView, sin restablecer cámara ni zoom; se reponen comercios, movimientos e iconos desde el estado actual. Se cancelan tareas y listeners al destruir la vista. No se descargan ni reemplazan archivos locales durante el uso.

## Datos y generación

El GeoJSON original permanece intacto en `app/src/main/assets/map/santiago.geojson`, pero no se empaqueta en la app. Conserva la atribución y procedencia existentes en `source.json` y las herramientas de descarga. El PMTiles añade atribución OSM/ODbL y el SHA-256 de su origen en metadatos.

| Medida | Resultado |
|---|---:|
| GeoJSON original | 36.791.236 bytes; 117.853 geometrías |
| PMTiles definitivo | 18.283.792 bytes; 1.870 teselas |
| Reducción respecto al GeoJSON sin comprimir | 50,3 % |
| Identificadores de geometrías presentes en zoom 15 | 117.853 / 117.853 |
| Generación observada | 73,91 s en este equipo |

Se conservan `kind`, `class`, `name` y las demás propiedades. Se proyecta a Web Mercator, se recorta con margen de ocho píxeles y se simplifica a un máximo de 1/8 de píxel en zooms inferiores; en zoom 15 no se simplifica. La cuantización usa extent 16.384. Una primera versión con extent 4.096 omitía un segmento de aproximadamente 15 cm: se corrigió antes de entregar. Los elementos pueden aparecer en más de una tesela al cruzar sus bordes.

El validador decodifica las 1.870 teselas, compara `kind/class/name` con el original y exige que todos los identificadores estén presentes en zoom 15. No sustituye una inspección visual exhaustiva de cada calle o parque.

SHA-256:

- GeoJSON: `3ABF1C35195115566B7F5B5C5C2ED91D868A069A0688F8BCD61CB369B9FF27DA`
- PMTiles: `B99166D57D11E8A6681707E2343C1DECB4742C052D8498CC62EB3E97A6856367`

Desde la raíz del proyecto, usando un entorno Python separado:

```powershell
python -m pip install -r tools/map/requirements-tiles.txt
python tools/map/build_pmtiles.py app/src/main/assets/map/santiago.geojson app/src/main/assets/map/santiago.pmtiles
python tools/map/validate_tiles.py app/src/main/assets/map/santiago.pmtiles app/src/main/assets/map/santiago.geojson
```

Generación comprobada con Python 3.14 y las versiones fijadas. El generador escribe sobre la ruta de destino: generar primero en una ruta temporal si se desea conservar el archivo anterior hasta terminar la validación. Al actualizar OSM, regenerar PMTiles y publicar una nueva URL versionada junto con la app.

## Rendimiento y pruebas reales

Emulador Pixel 8, Android 17, mismo equipo. Línea base GeoJSON: **10.956 ms y 6.084 ms** hasta primer fotograma completo en las dos pruebas de `OfflineMapTest`. La línea base pasó ambas pruebas. Las mediciones se registran con etiqueta `SaveQuestMap`; no representan un benchmark estadístico, memoria máxima ni rendimiento garantizado en teléfonos físicos.

PMTiles definitivo, con el mismo punto de inicio de medición: **4.387 ms y 3.592 ms**, aproximadamente **60 % y 41 % menos**, respectivamente. El ajuste final pasó otra vez las tres pruebas directas de mapa/conmutación y las 13 unitarias. La recuperación simulada online→offline completó el primer fotograma local en 13.138 ms desde apertura, incluidos los 12 segundos de espera de fallo. No se presenta como velocidad de carga offline normal.

Comprobaciones realizadas:

- `testDebugUnitTest`: **13 pruebas aprobadas**.
- `assembleDebug`: compilación correcta.
- Conjunto de instrumentación en **modo avión**: **12 pruebas aprobadas**: HybridMapTest (1), OfflineMapTest (2), PlaceMapUiTest (4), PlacePersistenceTest (3), LocationPersistenceTest (1), PlaceMigrationTest (1).
- La conmutación se prueba forzando que la sonda inicial acepte un endpoint inaccesible, simulando un servidor que cae justo después de la comprobación. El motor falla realmente al cargar online y recupera PMTiles local; se verifica cámara, zoom, marcador, fuente `places` y ausencia de segundo intento tras otro intervalo de espera. Esto no prueba un proveedor HTTPS exitoso.
- Renderizado de carreteras comprobado por colores en captura; marcadores y comercios seleccionables; formularios, restauración de estado y persistencia/migración de lugares cubiertos por las pruebas existentes.
- APK inspeccionado: contiene `assets/map/santiago.pmtiles` sin compresión ZIP y **no contiene** `santiago.geojson`.
- `lintDebug`: **no aprobado**, por `InvalidFragmentVersionForActivityResult` en `MainActivity.kt:35`, código/dependencias ajenos a este cambio. Reportó además 31 advertencias y una sugerencia, incluidas dos recomendaciones de usar Timber para los nuevos registros. No se ocultó el error con una baseline ni se modificó la autenticación para resolverlo.

El APK debug observado mide aproximadamente **99,4 MB**. Aunque el archivo base sin comprimir se reduce, PMTiles almacena varios niveles y necesita acceso directo: el tamaño empaquetado aumenta frente al GeoJSON comprimido (estimación Deflate del original: 6,73 MB; PMTiles empaquetado: 18,28 MB). Se prioriza tiempo de carga y acceso por teselas. No se midió una reducción de RAM ni una comparación exacta del APK anterior.

No se ejecutó toda la suite de pantallas no relacionadas ni se validaron GPS/biometría físicos. No se borraron bases de datos de usuario ni se solicitó desinstalar la app.

## Activar hosting online

1. Elegir almacenamiento/CDN HTTPS con certificado público válido y soporte de solicitudes Range. No hace falta servidor de teselas: se aloja el PMTiles estático completo.
2. Subir **exactamente** `app/src/main/assets/map/santiago.pmtiles` a una URL pública versionada e inmutable. No recomprimir, transformar ni servir el objeto mediante una página de descarga. Evitar redirecciones. Mantener atribución OSM/ODbL y acceso a procedencia/licencia.
3. Verificar `GET` con `Range: bytes=0-16383`: debe devolver `206`, `Content-Range: bytes 0-16383/18283792` y exactamente 16.384 bytes iguales al prefijo local. Content-Encoding debe estar ausente o ser identity. Repetir con rangos del centro/final del archivo. CORS no es necesario para Android nativo; sí si se reutiliza en navegador.
4. Completar `onlinePmtilesUrl` en `app/src/main/assets/map/hybrid-config.json` con la URL HTTPS real, sin el prefijo `pmtiles://`. No guardar contraseñas, tokens privados ni URLs firmadas efímeras en Git.
5. Compilar e instalar conservando datos. Con red activa, verificar el registro `mode=online`, calles, parques, agua, comercios y movimientos en zoom 10–18.
6. Interrumpir red durante la carga y después de desplazarse: debe aparecer `fallback=offline`, conservar cámara/zoom y permitir selección/registro de gastos. Volver a conectar no cambia otra vez durante esa apertura. Reabrir el mapa para intentar online de nuevo.
7. Probar también respuestas 200 sin Range, 403/404, TLS inválido, archivo de otra versión, servidor lento y modo avión desde arranque. Todos deben mantener el mapa local utilizable. Esta validación del proveedor queda pendiente.

Para repetir las pruebas Android relevantes, usar JDK de Android Studio y un emulador:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.rpgahorro.savequest.HybridMapTest,com.rpgahorro.savequest.OfflineMapTest,com.rpgahorro.savequest.PlaceMapUiTest,com.rpgahorro.savequest.PlacePersistenceTest,com.rpgahorro.savequest.LocationPersistenceTest,com.rpgahorro.savequest.PlaceMigrationTest'
```

El modo avión se activó únicamente en el emulador de pruebas y se restauró al finalizar. Próximos pasos: configurar/probar hosting, medir en teléfono físico y resolver separadamente el error de Fragment señalado por lint.
