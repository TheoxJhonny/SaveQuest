# SaveQuest: registro de gastos compartidos

## Qué incorpora esta etapa

- Acceso desde **+ → Gasto compartido** o **Perfil → Gastos compartidos**.
- Compra con título y varios productos o servicios; cada línea tiene cantidad y precio unitario en pesos enteros.
- Participación propia y al menos un contacto activo, un pagador y, cuando pagas tú, una cuenta de origen.
- Reparto igual o montos personalizados. La suma debe coincidir exactamente con el total. Los pesos sobrantes se distribuyen de forma estable, empezando por la participación propia.
- Vista previa del total, las partes individuales, el dinero pagado y las deudas.
- Historial con detalle de productos y reparto; resumen de lo que cada contacto debe o tiene por cobrar.
- Borrador restaurable al recrear la pantalla, bloqueo del botón durante el guardado e identificador de operación para evitar duplicados al reintentar.
- Una recompensa base de 10 XP por registro y avance del desafío «Mantén el ritmo», dentro de la misma transacción de base de datos.

## Cómo se evita contar dos veces el gasto

| Compra de $30.000; tu parte es $10.000 | Pagas tú | Paga un contacto |
|---|---:|---:|
| Sale de tu cuenta | $30.000 | $0 |
| Tu gasto personal | $10.000 | $10.000 |
| Te deben | $20.000 | $0 |
| Debes al pagador | $0 | $10.000 |
| Disminución de tu patrimonio | $10.000 | $10.000 |

El patrimonio se calcula como dinero en cuentas más cuentas por cobrar menos cuentas por pagar. El disponible para aportar a metas usa solamente dinero en cuentas menos lo reservado: el dinero que otras personas te deben no se puede reservar como si ya lo hubieras recuperado.

Las compras compartidas se guardan en su propio historial; no generan además un gasto ordinario en `transactions`. Inicio suma únicamente la parte propia al indicador de gastos. «Movimientos recientes» conserva por ahora su historial de movimientos ordinarios.

Como se registran compras ya realizadas, se permite registrar un pago que supere el disponible, mostrando una advertencia antes de guardar. Esto puede dejar una cuenta en negativo o por debajo de lo reservado; no libera aportes a metas automáticamente.

## Base de datos y responsabilidades

Room pasa de **v6 a v7** con tres tablas nuevas:

- `shared_expenses`: cabecera, pagador, cuenta y cantidades financieras.
- `shared_items`: productos y servicios de la compra.
- `shared_shares`: parte de cada participante y nombre al momento del registro.

Las relaciones con cuentas, contactos y cabeceras tienen claves foráneas. La migración agrega tablas e índices; no elimina ni reescribe movimientos, contactos, metas, aportes o progreso anteriores. Archivar un contacto no elimina sus compras ni deudas.

`SharedExpensePlan` valida el reparto. `SharedExpenseRepository` comprueba cuentas y contactos y guarda cabecera, líneas, participaciones y recompensa en una única transacción. Si falla cualquier inserción, se revierte todo. Las reglas de XP están compartidas con el repositorio de movimientos ordinarios.

Rutas principales dentro del proyecto `C:/Users/Gamer/AndroidStudioProjects/SaveQuest/`:

- `app/src/main/java/com/rpgahorro/savequest/data/local/SharedExpenseMigrations.kt`
- `app/src/main/java/com/rpgahorro/savequest/data/local/entity/SharedExpenseEntity.kt`
- `app/src/main/java/com/rpgahorro/savequest/data/repository/SharedExpenseRepository.kt`
- `app/src/main/java/com/rpgahorro/savequest/ui/shared/SharedExpensesViewModel.kt`
- `app/src/main/java/com/rpgahorro/savequest/ui/shared/SharedExpensesScreen.kt`

## Prueba manual recomendada

Validación del 18 de octubre de 2026: compilación correcta, **13 pruebas unitarias y 27 pruebas de Android aprobadas entre la ejecución general y las repeticiones dirigidas**. La primera ejecución detectó un selector incorrecto en el test de interfaz; se corrigió y la prueba pasó. Se volvieron a comprobar los cinco casos de persistencia/migración tras reforzar los reintentos. La prueba especial de PIN no se repitió porque requiere una credencial ficticia; permanece la validación de la etapa anterior.

Las pruebas cubren migraciones desde v3/v4/v5/v6, preservación de datos, pago propio y por contacto, deudas, disponible para metas, guardados simultáneos, reintentos, reversión ante fallos, validaciones y restauración del formulario. Las capturas usan datos ficticios de un emulador separado.

1. Ejecutar la actualización desde Android Studio sobre la instalación actual, sin desinstalar ni borrar datos.
2. Crear dos contactos. Registrar una compra de tres unidades a $10.000, incluyéndote con ambos contactos y seleccionándote como pagador.
3. Elegir una cuenta y reparto igual. Revisar: total $30.000, parte propia $10.000, salida $30.000 y por cobrar $20.000.
4. Guardar. Verificar un solo registro, 10 XP base y el descuento de $30.000 en esa cuenta. Completar el desafío puede agregar su recompensa de 100 XP.
5. Registrar otra compra cuyo pagador sea un contacto. Verificar que la cuenta propia no se descuenta y que aparece la deuda por tu parte.
6. Probar un reparto personalizado cuya suma sea incorrecta: debe impedir guardar. Corregirlo y comprobar el detalle.

## Alcance pendiente

Esta etapa registra las compras y las deudas iniciales. Aún falta registrar **abonos, cobros y liquidaciones**; esos movimientos deberán cambiar dinero y deuda sin generar otro ingreso o gasto personal. Tampoco se implementan todavía edición/anulación de compras compartidas, varios pagadores en una misma compra, ubicación de compras compartidas ni su incorporación al historial unificado.

Después corresponde compras planificadas con fecha y monto estimado, revisión integral de pantallas y documentación final.
