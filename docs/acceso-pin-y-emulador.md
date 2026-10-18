# SaveQuest: PIN del teléfono y prueba de huella

## Acceso

La pantalla de bloqueo ofrece **Usar biometría** y **Entrar con PIN del teléfono**. El segundo botón abre el diálogo de Android, que verifica el PIN, patrón o contraseña configurado en ese dispositivo. SaveQuest no crea ni guarda otro PIN.

Si todavía no hay un bloqueo seguro, aparece **Configurar bloqueo del teléfono**. Se debe configurar desde Android y después volver a la app. Cancelar la verificación mantiene la sesión cerrada; salir de SaveQuest vuelve a bloquearla.

La sesión solo vive en memoria. El diálogo de credenciales puede cubrir la actividad: durante esa espera SaveQuest continúa bloqueado y solo acepta el resultado del intento vigente. Si el proceso se reinicia, hay que autenticarse de nuevo.

En Android 11 o superior se utiliza BiometricPrompt con DEVICE_CREDENTIAL. En Android 8–10 se utiliza el diálogo compatible de KeyguardManager, porque ese tipo de autenticador no está soportado por BiometricPrompt en esas versiones. La base de datos no cambia.

## Probar huella en Android Studio

Sí: el emulador permite simular un sensor de huellas. No es una lectura de tu dedo físico.

1. Inicia un AVD compatible, por ejemplo Pixel 8. Dentro del Android emulado abre **Ajustes → Seguridad** o **Seguridad y privacidad → Desbloqueo del dispositivo**. Los nombres varían según la imagen de Android.
2. Configura un PIN de prueba y entra a registrar una huella.
3. Abre los controles extendidos del emulador (**⋮ / More / Extended Controls**) y entra a **Fingerprint**. En el emulador integrado de Android Studio, busca ese control en la barra de Running Devices.
4. Selecciona **Finger 1** y pulsa **Touch Sensor** las veces que solicite Android hasta terminar el registro.
5. Abre SaveQuest, pulsa **Usar biometría** y vuelve a pulsar **Touch Sensor** con el mismo dedo virtual. Debe abrirse Inicio.
6. Prueba un dedo virtual no registrado o cancela: no debe abrirse Inicio. Sal de SaveQuest y vuelve para comprobar que solicita autenticación otra vez.
7. Prueba también **Entrar con PIN del teléfono**: introduce el PIN del Android emulado, no el de tu teléfono físico.

Referencia oficial: [controles extendidos del emulador](https://developer.android.com/studio/run/emulator-extended-controls#fingerprint) y [autenticación biométrica y credenciales del dispositivo](https://developer.android.com/identity/sign-in/biometric-auth).

## Pruebas de desarrollo

Validación realizada: compilación correcta, siete pruebas unitarias aprobadas y tres pruebas de acceso en Android aprobadas (pantalla de opciones, recreación de actividad y diálogo real de PIN). La prueba del sistema se ejecutó en un AVD independiente con Android 17, usando un PIN ficticio. Se verificaron cancelación, entrada correcta y bloqueo al salir. La compatibilidad de Android 8–10 está implementada, pero no se ejecutó esa rama en un emulador antiguo durante esta etapa.

`CredentialSessionTest` cubre espera del diálogo, confirmación al regresar, cancelación, resultados atrasados y reinicio del proceso. `AccessViewModelTest` mantiene las pruebas de biometría y bloqueo anteriores.

`DeviceCredentialTest` se ejecuta únicamente cuando se entrega el argumento de instrumentación `testDevicePin` con un PIN ficticio de seis dígitos, en un emulador desechable. Sin ese argumento se omite para no interactuar con el bloqueo de un dispositivo personal. Comprueba cancelación, desbloqueo mediante el diálogo real de Android y bloqueo al salir. Nunca se debe poner el PIN personal en código ni en argumentos de pruebas.

La validación del emulador comprueba el flujo de la aplicación. La compatibilidad y el comportamiento del sensor real se revisan también en el teléfono de la demostración.
