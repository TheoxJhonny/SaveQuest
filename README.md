# SaveQuest

**Convierte el registro de tus finanzas en una aventura.**

SaveQuest es una aplicación Android de finanzas personales gamificada, desarrollada con **Kotlin, Jetpack Compose y Room/SQLite**. Permite registrar ingresos y gastos, organizar cuentas, reservar dinero para metas de ahorro y compartir compras con contactos, mientras se obtiene experiencia y se completan desafíos.

El proyecto está en desarrollo. La información financiera se guarda localmente y el mapa de Santiago funciona sin conexión.

## Funcionalidades implementadas

| Módulo | Funcionalidad |
| --- | --- |
| Inicio | Resumen de patrimonio, ingresos, gastos, progreso y meta de ahorro. |
| Movimientos | Registro de ingresos y gastos por cuenta, categoría y descripción, con ubicación opcional. |
| Cuentas | Administración de cuentas y consulta de saldos y dinero disponible. |
| Metas de ahorro | Creación de metas y aportes que reservan dinero de una cuenta. |
| Gamificación | Experiencia, niveles y desafío «Mantén el ritmo». Cada registro válido de movimiento otorga 10 XP base. |
| Contactos | Administración de contactos para participar en gastos compartidos. |
| Gastos compartidos | Compras con productos, cantidades, pagador y reparto igual o personalizado; historial y deudas iniciales. |
| Mapa | Cartografía offline de Santiago, búsqueda y filtros sobre 10.809 comercios incluidos. |
| Acceso | Autenticación biométrica o credencial de bloqueo del teléfono: PIN, patrón o contraseña. |

### Registrar un gasto desde el mapa

1. Abre **Mapa** y busca un comercio o selecciona una categoría.
2. Acerca el mapa y toca un ícono o un resultado de búsqueda.
3. Pulsa **Registrar gasto aquí**.
4. Revisa el comercio, la ubicación y la categoría sugerida; completa monto y cuenta.
5. Guarda el gasto. Podrás consultarlo en Movimientos y en el historial de compras del comercio.

Si falta un negocio, mantén pulsado un punto del mapa e introduce su nombre y categoría. El lugar se conserva al guardar el gasto.

El catálogo cubre una zona urbana de Santiago; no incluye toda la Región Metropolitana. Es una copia incluida en la aplicación, no un directorio actualizado en tiempo real. No ofrece rutas, precios, stock ni horarios de atención.

## Criterios financieros

- **Un aporte a una meta es una reserva**, no un gasto: reduce el disponible, pero no descuenta el patrimonio por segunda vez.
- En una compra compartida se distingue el dinero pagado de la parte que corresponde al usuario.
- El patrimonio considera el dinero en cuentas, más lo que otros deben al usuario, menos sus deudas.
- El dinero por cobrar no se considera disponible para reservar en metas hasta que se reciba.
- Los importes se registran en pesos enteros; los repartos evitan cálculos monetarios con decimales de punto flotante.
- El guardado de movimientos y sus recompensas se realiza de forma atómica para mantener los datos consistentes.

## Tecnologías y organización

- **Kotlin** y **Jetpack Compose**, con componentes Material 3.
- **ViewModel**, corrutinas y Flow para el estado y el acceso a datos.
- **Room 2.8.4 / SQLite**, con migraciones y esquemas exportados.
- **Navigation Compose** para la navegación.
- **MapLibre Native 13.7.1** y datos de OpenStreetMap incluidos como recursos locales.
- **AndroidX Biometric** y credenciales del sistema para el acceso.
- **JUnit** y pruebas instrumentadas de Android y Compose.

```text
app/
  schemas/                         Esquemas de Room para validar migraciones
  src/main/
    assets/map/                    Cartografía, comercios, estilo y fuentes offline
    java/com/rpgahorro/savequest/
      data/                        Entidades, DAO, migraciones y repositorios
      domain/                      Reglas y cálculos de negocio
      navigation/                  Destinos de navegación
      ui/                          Pantallas, componentes y ViewModels
  src/test/                        Pruebas unitarias
  src/androidTest/                 Pruebas de persistencia, migración e interfaz

docs/                              Documentación por etapa
tools/map/                         Herramientas para actualizar los recursos del mapa
```

## Requisitos para desarrollar

- Android Studio compatible con la configuración Gradle del proyecto.
- Android SDK **API 37** instalado.
- Un dispositivo o emulador con **Android 8.0 / API 26 o superior**.
- Un JDK compatible con el wrapper y Android Gradle Plugin; en el entorno de desarrollo se utiliza el JDK integrado de Android Studio.
- Conexión a Internet para descargar las dependencias en la primera sincronización.

El proyecto utiliza **Gradle 9.5.0** mediante su wrapper y **Android Gradle Plugin 9.3.3**. Las versiones están definidas en los archivos del repositorio; utiliza el wrapper incluido.

## Instalación y ejecución

1. Clona el repositorio:

   ```powershell
   git clone https://github.com/TheoxJhonny/SaveQuest.git
   cd SaveQuest
   ```

2. Abre la carpeta del proyecto desde Android Studio y espera a que termine la sincronización de Gradle.
3. Instala los componentes del SDK solicitados. La ruta local del SDK se configura en `local.properties`, que no se versiona.
4. Selecciona un emulador o conecta un dispositivo con depuración USB habilitada.
5. Configura un bloqueo seguro en el dispositivo de prueba para utilizar el acceso mediante PIN, patrón o contraseña. Para probar biometría, registra una huella en un dispositivo o emulador compatible.
6. Ejecuta el módulo **app** desde Android Studio.

También puedes compilar desde PowerShell en la raíz del proyecto, con `JAVA_HOME` configurado al JDK correspondiente:

```powershell
.\gradlew.bat :app:assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

La aplicación no requiere una clave de API para el mapa. La cartografía, los comercios y sus etiquetas están incluidos en la instalación.

## Pruebas

Ejecuta las pruebas unitarias:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Para las pruebas instrumentadas, inicia un **emulador dedicado a pruebas**, ya que algunos casos crean y modifican datos de la aplicación:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.notClass=com.rpgahorro.savequest.DeviceCredentialTest"
```

La prueba especial `DeviceCredentialTest` necesita un PIN ficticio configurado en un emulador desechable. Su preparación está explicada en la [guía de acceso y emulador](docs/acceso-pin-y-emulador.md). No utilices tu PIN personal como argumento de pruebas.

Las pruebas cubren reglas de reparto, integridad de los registros, migraciones, conservación de datos, acceso, restauración de formularios y selección de comercios. Consulta los resultados y límites de cada validación en las guías de `docs/`.

## Datos y acceso

La base de datos actual utiliza el esquema **Room v8**. Las migraciones permiten actualizar instalaciones anteriores conservando sus registros; no es necesario desinstalar ni borrar datos para actualizar la aplicación.

SaveQuest utiliza la autenticación de Android y no almacena un PIN propio. El bloqueo de acceso no implica cifrado adicional de la base de datos. La sesión se mantiene en memoria y se vuelve a solicitar autenticación al regresar según el flujo de bloqueo de la aplicación.

La ubicación de los movimientos es opcional. La aplicación no necesita permiso de Internet para su funcionamiento actual y no incorpora sincronización en la nube ni integración bancaria.

## Trabajo pendiente

- Registrar abonos, cobros y liquidaciones de deudas compartidas.
- Implementar compras planificadas con fecha y monto estimado.
- Incorporar edición o anulación de compras compartidas y varios pagadores por compra.
- Unificar el historial de movimientos ordinarios y compras compartidas.
- Incorporar ubicación a compras compartidas y filtros financieros por período en el mapa.
- Continuar la revisión visual, las pruebas en dispositivos físicos y la documentación final.

## Documentación

- [Acceso con PIN y pruebas de biometría en el emulador](docs/acceso-pin-y-emulador.md).
- [Contactos y cálculos de reparto](docs/contactos-y-repartos.md).
- [Gastos compartidos y criterios financieros](docs/gastos-compartidos.md).
- [Comercios offline, migración y guía de prueba](docs/comercios-offline.md).
- [Herramientas de cartografía](tools/map/README.md).
- [Registro de avance inicial](docs/avance-2026-10-17.md).

## Atribuciones

La cartografía y los comercios proceden de **OpenStreetMap contributors**, bajo [ODbL](https://www.openstreetmap.org/copyright). Los recursos de fuentes incluyen sus avisos de licencia en `app/src/main/assets/map/fonts/`: Noto Sans bajo SIL Open Font License y los avisos de distribución de MapLibre demotiles.

Las fuentes de datos, fechas de descarga y metadatos se conservan junto a los recursos del mapa. Estas licencias corresponden a los recursos de terceros; este repositorio todavía no declara una licencia general para el código de SaveQuest.
