# SaveQuest: contactos y base de gastos compartidos

## Contactos

La entrada está en **Perfil → Contactos**. Permite crear y editar nombre, teléfono opcional y nota; buscar por nombre o teléfono; archivar y restaurar personas. Los contactos pertenecen únicamente a SaveQuest: no se solicita acceso a la agenda del teléfono ni se envían datos a servicios externos.

Se admiten personas con el mismo nombre. Cada contacto tiene un identificador independiente. Editar conserva ese identificador, la fecha de creación y el estado de archivo. Archivar conserva la fila para que las futuras referencias de compras o deudas puedan mantenerse.

## Base de datos

Room pasa de la versión 5 a la 6 mediante una migración que agrega `contacts`. No reconstruye ni elimina las tablas existentes. El esquema exportado queda versionado junto con el código.

Archivos principales dentro de `app/src/main/java/com/rpgahorro/savequest/`:

- `data/local/entity/ContactEntity.kt`: datos del contacto.
- `data/local/dao/ContactDao.kt`: consultas y actualizaciones de campos concretos.
- `data/local/ContactMigrations.kt`: migración 5 → 6.
- `data/repository/ContactRepository.kt`: validación de nombre, teléfono y nota.
- `ui/contacts/ContactsViewModel.kt` y `ContactsScreen.kt`: estado de guardado, errores y pantalla.

## Preparación de los gastos compartidos

`domain/shared/SharedExpensePlan.kt` calcula un plan a partir de productos o servicios, cantidades, precios, participantes y una persona pagadora. Tiene reparto igual y personalizado.

El cálculo usa pesos enteros. Si hay un resto, distribuye un peso adicional por participante en orden estable de identificador, comenzando por la persona usuaria. Por ejemplo, $10.000 entre tres da $3.334, $3.333 y $3.333; cambiar el orden de selección no cambia a quién corresponde el peso adicional.

Valida participantes sin duplicados, presencia de la persona usuaria, pagador perteneciente al grupo, productos válidos y suma exacta de las partes. Rechaza operaciones que exceden el rango numérico antes de producir un resultado.

| Caso: compra de $30.000 entre tres | Sale de tu cuenta | Gasto propio | Por cobrar | Por pagar |
| --- | ---: | ---: | ---: | ---: |
| Tú pagas todo | $30.000 | $10.000 | $20.000 | $0 |
| Paga un contacto | $0 | $10.000 | $0 | $10.000 |

El patrimonio debe considerar dinero en cuentas más cuentas por cobrar menos deudas por pagar. El disponible para gastar o reservar en metas sigue dependiendo del dinero real de cada cuenta. Cuando alguien devuelve un adelanto, cambia dinero por cobrar a dinero en la cuenta; no representa un ingreso nuevo.

## Alcance de esta etapa

Los contactos ya se guardan en Room. El motor de reparto es una preparación validada: **todavía no existe una pantalla para guardar gastos compartidos, ni se persisten sus deudas o pagos**. El cálculo no modifica saldos ni otorga XP. La versión 6 solo agrega contactos; las tablas de gastos se añadirán con la integración contable completa, evitando guardar operaciones que todavía no se reflejen correctamente en Inicio y Cuentas.

El próximo paso es conectar este cálculo con la persistencia atómica de gastos, productos, participaciones y deudas, y construir la pantalla de registro. Luego corresponden abonos/liquidaciones y compras planificadas.

## Pruebas incorporadas

Validación final: compilación correcta, **13 pruebas unitarias y 21 pruebas de Android aprobadas**, sin fallos. La batería de Android se ejecutó en un AVD independiente. La prueba especial del PIN del sistema, ya validada en la etapa anterior, se excluyó de esta ejecución porque requiere configurar una credencial ficticia.

- `ContactDatabaseTest`: migración desde el esquema real v5 comparando todas las filas existentes, edición con identidad estable, archivo/restauración y ausencia de cambios en movimientos o XP.
- `ContactsUiTest`: validación del formulario, conservación de campos al recrearse, creación, edición, archivo, restauración y búsqueda.
- `SharedExpensePlanTest`: reparto igual y personalizado, pagador propio o externo, restos, productos con cantidades y desbordamiento numérico.

Prueba manual sugerida: crea un contacto, edítalo, archívalo y restáuralo desde Perfil. Confirma que tus saldos, metas y XP permanecen iguales.
