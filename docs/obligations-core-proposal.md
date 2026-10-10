# Obligations Core: propuesta de diseño

Estado: **aprobada (v2)**. Implementación por pasos (sección 10).

Objetivo: modelar los pagos comprometidos del usuario para que el backend calcule `committedAmount`, `availableToSpend`, `shortfall`, `upcomingCommitments` y `attention` (ver [`dashboard-review-read-models.md`](./dashboard-review-read-models.md)).

### Cambios durante el paso 1

Decisiones del desarrollador al implementar el dominio; prevalecen sobre el texto original donde haya diferencias.

1. **Vencidas en O(R)**: vencidas = fechas del calendario desde `startDate` hasta ayer, menos las fechas resueltas que **están en** el calendario vigente. Se descarta el puerto `countByObligationIdAndDueDateBetween`; los casos de uso de comandos (pasos 2 y 3) cargan las resoluciones por obligación (`findByObligationId`) y el paso 4 carga todas las resoluciones de las obligaciones activas del usuario en una sola consulta.
2. **Nuevo código** `OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION` (409): "Start date cannot be after a resolved upcoming occurrence", "End date cannot be before a resolved upcoming occurrence" y "Upcoming resolution is not on the new calendar". Al re-anclar, toda resolución con `dueDate >= hoy` debe estar en el calendario nuevo (evita el doble pago). "Start date cannot be before today" y "End date cannot be before yesterday" siguen siendo `IllegalArgumentException`.
3. **`changeRecurrence`** rechaza todo cambio cuyo calendario resultante tenga vencidas sin resolver (`OBLIGATION_HAS_OVERDUE_OCCURRENCES`, "Change would create overdue occurrences"), incluido quitar o extender un `endDate` ya pasado. El orden completo de comprobaciones está en la sección 3.
4. **`Recurrence`** contiene frecuencia, `startDate` y `endDate`; `Obligation` no tiene campos de fecha propios. Los métodos de edición `changeAmount`, `changePaymentSource(account, category)`, `changeRecurrence` y `archive` reciben `hoy` y todas las resoluciones; `rename` recibe solo el nombre. `rename` y `archive` sobre una obligación archivada lanzan `OBLIGATION_ARCHIVED`. "Nombre: siempre" aplica solo a la regla de vencidas.
5. **`CommitmentCalculator`** recibe snapshots de entrada (`ObligationSnapshot`, `ResolutionSnapshot`, `AccountSnapshot`, `CategorySnapshot`), no agregados, para que la lectura del dashboard no reconstruya agregados. El horizonte es configurable (`DEFAULT_HORIZON_DAYS = 14`) y la attention se ordena por el orden de declaración de `AttentionType` (contrato de prioridad). Salidas: `CommitmentSummary`, `ObligationCommitment`, `AccountCommitment` y `Attention` (sealed) con `AttentionType`.
6. **`OperationReferences.validate`** comparte las comprobaciones de registro de `Income`, `Expense` y `Obligation`, con el mismo orden y los mismos mensajes que antes.

### Cambios durante el paso 2

Decisiones al implementar la persistencia; prevalecen sobre el texto original donde haya diferencias.

1. **`existsByUserIdAndNameAndIdNot`** se agrega a `ObligationRepository` (con SQL en el adaptador) para que `ModifyObligation` valide el renombrado contra otras obligaciones del usuario.
2. **Migración transaccional**: `001-obligations-core.sql` va envuelto en `BEGIN`/`COMMIT`, de modo que corre como una sola transacción y, si una sentencia falla, no se aplica nada.
3. **Solo se traducen dos `UNIQUE`**: `(user_id, name)` en `obligations` y `(obligation_id, due_date)` en `occurrence_resolutions`. Las violaciones de `UNIQUE (expense_id)`, `CHECK` y claves foráneas no se traducen y siguen como fallo técnico.

### Cambios durante el paso 3

Decisiones al implementar los casos de uso; prevalecen sobre el texto original donde haya diferencias.

1. **El paso 3 se divide en 3a, 3b y 3c.** 3a (implementado): `CreateObligation`, `ModifyObligation` y `ArchiveObligation`. 3b (implementado): `SkipOccurrence`, `SkipOverdueOccurrences` y `ReopenOccurrence`. 3c (implementado): `PayOccurrence`, `ExpenseRegistration` y el ajuste de `CancelOperation`. Con 3c el paso 3 queda completo. El paso 4 (lado de lectura del dashboard) está implementado y `GET /api/v1/dashboard` existe (modo de un solo usuario).
2. **`ModifyObligationCommand` es una modificación parcial**: `name`, `amount`, `accountId`, `categoryId` y `recurrence` son anulables y `null` conserva el valor actual. Si todos son `null`, lanza `IllegalArgumentException`. La `recurrence` reemplaza el calendario completo (frecuencia, `startDate` y `endDate`).
3. **Cuenta o categoría sueltas**: si solo se envía `accountId` o solo `categoryId`, el otro se toma de la obligación actual y ambos se vuelven a validar.
4. **Orden de `CreateObligation`**: propiedad de cuenta y categoría (404) → nombre duplicado (409) → reglas de dominio.
5. **Orden de `ModifyObligation`**: obligación (404) → cuenta y categoría nuevas (404) → resoluciones → renombrar → comprobación de nombre duplicado (así archivada gana al 409) → monto → cuenta y categoría → recurrencia → un único `update`. Los valores iguales a los actuales también pasan por las comprobaciones de archivada y de vencidas.
6. **`Clock`** se inyecta como último parámetro del constructor y `hoy` se calcula dentro de la transacción. Excepción: `ReopenOccurrence` no recibe `Clock`, porque no necesita `hoy`.
7. **Decisiones del paso 3b**:
   * `SkipOverdueOccurrences` sin vencidas no hace nada y devuelve una lista vacía; una obligación archivada falla con `OBLIGATION_ARCHIVED` aunque no haya nada pendiente (se comprueba primero).
   * `ReopenOccurrence` sin resolución para la fecha lanza `ResourceNotFoundException` ("Occurrence resolution not found for obligation <id> on <fecha>").
   * Orden de `SkipOccurrence`: obligación (404) → archivada → fecha fuera del calendario (`OCCURRENCE_NOT_SCHEDULED`) → ya resuelta (`OCCURRENCE_ALREADY_RESOLVED`, comprobación previa más la traducción del `UNIQUE` en el adaptador). Se pueden omitir fechas futuras del calendario.
   * Orden de `SkipOverdueOccurrences`: obligación (404) → archivada → omitir las vencidas.
   * Orden de `ReopenOccurrence`: obligación (404) → archivada → resolución inexistente (404) → `PAID` (`OCCURRENCE_PAID_NOT_REOPENABLE`) → borrar. No comprueba el calendario, así que se puede reabrir una omisión que quedó fuera del calendario tras un re-anclaje.
   * Valores de retorno: `SkipOccurrence` y `ReopenOccurrence` devuelven el id de la obligación; `SkipOverdueOccurrences` devuelve las fechas omitidas en orden ascendente.
   * `Obligation.ensureActive()` es público y lo usan los casos de uso y `OccurrenceResolution` (mismo código, mensaje y orden).
   * `Obligation.overdueDates(hoy, resoluciones)` está en el dominio: devuelve las vencidas (sin resolver, desde `startDate` hasta ayer) en orden ascendente, con el mismo rango que `overdueCount`.
8. **Decisiones del paso 3c**:
   * `PayOccurrence(ObligationRepository, OccurrenceResolutionRepository, ExpenseRegistration, TransactionManager, Clock)` recibe `PayOccurrenceCommand(userId, obligationId, dueDate, amount?, operationDate?, accountId?, categoryId?)`; los opcionales son anulables y un `amount` indicado debe ser mayor que cero (`IllegalArgumentException`, "Amount must be greater than zero").
   * **Devuelve el id del gasto** (decisión del desarrollador): el cliente lo necesita para deshacer el pago cancelando el gasto.
   * **Orden de comprobaciones** (difiere de los pasos de la sección 4): comando → `operationDate` posterior a hoy (`IllegalArgumentException`, "Operation date cannot be after today"; se comprueba primero) → obligación (404) → `OccurrenceResolution.validateNewResolution` (`OBLIGATION_ARCHIVED`, luego `OCCURRENCE_NOT_SCHEDULED`; se permiten fechas futuras del calendario) → resolución existente (`OCCURRENCE_ALREADY_RESOLVED`) → `ExpenseRegistration` con los valores por defecto (cuenta, categoría y monto de la obligación, hoy) o las alternativas indicadas (cuenta 404, categoría 404, tipo de categoría `IllegalArgumentException`, `ACCOUNT_INACTIVE`, `CATEGORY_INACTIVE`, `INSUFFICIENT_BALANCE`) → crear la resolución `PAID` con el id del gasto.
   * **`OccurrenceResolution.validateNewResolution(Obligation, LocalDate)` es público y estático**: `PayOccurrence` lo usa antes de que exista el gasto; `paid(...)` y `skipped(...)` lo repiten al crear.
   * **Carrera**: una resolución concurrente la traduce el adaptador a `OCCURRENCE_ALREADY_RESOLVED` y la transacción completa (gasto y saldo) se revierte. Se registra el monto real.
   * **`ExpenseRegistration(AccountRepository, CategoryRepository, ExpenseOperationRepository)`** expone `Expense register(RegisterExpenseCommand)`: sin transacción propia; carga la cuenta y luego la categoría por usuario (`ResourceNotFoundException`), llama `Expense.register`, debita la cuenta y guarda el gasto y luego la cuenta. `RegisterExpense` conserva su constructor público, construye `ExpenseRegistration` internamente y ejecuta `transactionManager.execute(() -> expenseRegistration.register(command).id())` con el mismo comportamiento.
   * **`CancelOperation`** recibe `(AccountRepository, IncomeOperationRepository, ExpenseOperationRepository, ReversalRepository, OccurrenceResolutionRepository, TransactionManager, Clock)` y usa `Instant.now(clock)`, lo que **resuelve el conflicto C1**. Al cancelar un gasto, tras el `Reversal` y las actualizaciones y en la misma transacción, ejecuta `findByExpenseId(expense.id()).ifPresent(r -> delete(r.id()))`. El gasto se carga con `findByIdAndUserId` antes de la búsqueda de la resolución (que no filtra por usuario), así que la propiedad se comprueba primero. Los ingresos nunca tocan resoluciones.
   * **C6 sigue abierto**: `RegisterExpense` acepta fechas de operación futuras y `PayOccurrence` las rechaza.

### Paso 4: diseño aprobado e implementado

Diseño del lado de lectura del Dashboard, aprobado por el desarrollador el 2026-10-04 e **implementado** (el endpoint `GET /api/v1/dashboard` existe; ver "Fuera de alcance"); prevalece sobre el texto original donde haya diferencias (secciones 6 y 10). Es una sola rama y un solo flujo, sin división 4a/4b (decisión 10). Los nombres de los accesores se ajustan a los registros de dominio reales.

**Caso de uso** `application.usecase.GetDashboard`, con `GetDashboardQuery(UUID userId)` (`userId` no nulo):

* Constructor `GetDashboard(DashboardQueryPort, TransactionManager, Clock)`; ninguna dependencia puede ser nula.
* Construye internamente `new CommitmentCalculator(HORIZON_DAYS)`, con `HORIZON_DAYS = CommitmentCalculator.DEFAULT_HORIZON_DAYS` (14). Constantes privadas: `UPCOMING_COMMITMENTS_LIMIT = 5` y `RECENT_ACTIVITY_LIMIT = 5`.
* **Corre dentro de `transactionManager.execute(...)`**, a diferencia de los demás casos de uso de consulta, para que las cinco lecturas vean una instantánea consistente (SQLite mantiene una sola instantánea de lectura por transacción) y usen una sola conexión.
* Flujo: `hoy = LocalDate.now(clock)` (equivalente a `LocalDate.ofInstant(Instant.now(clock), clock.getZone())`) → las cinco lecturas del puerto → `calculator.calculate(hoy, obligaciones, resoluciones, cuentas, categorías)` → mapeo a read models con los nombres y las banderas de cuentas, categorías y obligaciones por id.
* **Decisión 6**: si el calculador lanza `IllegalArgumentException` (referencia de cuenta o categoría ausente, solo posible con datos corruptos o de otro usuario), `GetDashboard` la relanza como `IllegalStateException` con un mensaje claro y la original como causa, para que se mapee a 500 y no a 400.

**Puerto** `application.port.out.DashboardQueryPort`: cinco métodos, todos por `userId` (no nulo):

| Método | Carga |
|---|---|
| `findActiveObligations(userId)` | `ObligationSnapshot` de las obligaciones `ACTIVE`. |
| `findResolutionsOfActiveObligations(userId)` | `ResolutionSnapshot` (`PAID` y `SKIPPED`) de **todas** las obligaciones `ACTIVE` del usuario, en una sola consulta. |
| `findAccounts(userId)` | `AccountSnapshot` de todas las cuentas, `ACTIVE` e `INACTIVE`. |
| `findCategories(userId)` | `CategorySnapshot` de todas las categorías, `ACTIVE` e `INACTIVE`. |
| `findRecentActivity(userId, limit)` | `RecentActivityItem` de las operaciones `ACTIVE`; `limit >= 1` o `IllegalArgumentException`. |

El filtrado de estado ocurre en SQL solo para obligaciones `ACTIVE` y operaciones `ACTIVE`; el estado de cuentas y categorías no se filtra, porque el calculador lo necesita (`PAYMENT_BLOCKED`, balance).

**Adaptador** `infrastructure.persistence.JdbcDashboardQueryAdapter(SQLiteConnectionProvider, TransactionConnectionHolder)`, `public final`, con la conexión en dos modos como `JdbcAccountQueryAdapter`. Un `SQLException` se envuelve en `RuntimeException("Failed to query dashboard <parte>")`; un fallo al mapear una fila lanza `CorruptedPersistedDataException`. Ordenes: obligaciones por `name, id`; resoluciones por `obligation_id, due_date` (con `JOIN obligations`); cuentas por `name, id`. La actividad reciente es un `UNION ALL` de ingresos, gastos y transferencias (mismo alcance por usuario y relleno con `NULL` que `JdbcFinancialOperationQueryAdapter`), solo `status = 'ACTIVE'`, con `ORDER BY operation_date DESC, created_at DESC, id DESC LIMIT ?`. Los gastos hacen `LEFT JOIN` a `occurrence_resolutions` y `obligations` para vincular un gasto pagado con su obligación (decisión 8); el join a `obligations` se acota por usuario (`AND ob.user_id = a.user_id`), de modo que una obligación de otro usuario nunca se vincula.

**Read models** (`application.readmodel`, registros inmutables; usan `Money`, no centavos; las listas se copian con `List.copyOf`):

| Registro | Campos y reglas |
|---|---|
| `Dashboard` | `horizon`, `availableToSpend`, `attention`, `upcomingCommitments`, `recent`. |
| `DashboardHorizon` | `LocalDate from`, `LocalDate to`; `from` no posterior a `to`. Es `[hoy, hoy + 13]`. |
| `AvailableToSpend` | `Money balance`, `committed`, `available`, `shortfall`. Regla de consistencia: `available` y `shortfall` no son ambos positivos y `balance + shortfall == committed + available`. |
| `ObligationSummary` | `UUID id`, `String name` (como `AccountSummary`). |
| `AttentionItem` (sealed, con `AttentionType type()`) | Cuatro registros, en el orden de prioridad de la sección 5: |
| `AttentionItem.OverdueOccurrence` | `obligation`, `oldestOverdue`, `long overdueCount` (> 0), `overdueAmount`. |
| `AttentionItem.Shortfall` | `Money shortfall`. |
| `AttentionItem.PaymentBlocked` | `obligation`, `account`, `nearestDueDate`, `committed`, `accountInactive`, `categoryInactive` (al menos una verdadera). |
| `AttentionItem.AccountShortfall` | `account`, `shortfall`, `nearestDueDate`. |
| `UpcomingCommitment` | `obligation`, `dueDate`, `Money amount` (el de la obligación), `account`, `boolean paymentBlocked`. |
| `RecentActivityItem` | `operationId`, `operationType`, `amount`, `operationDate`, `account`, `category`, `transfer`, `obligation` (opcional). `INCOME` y `EXPENSE` requieren cuenta y categoría y no llevan `transfer`; `TRANSFER` requiere `transfer` y no lleva cuenta ni categoría; `obligation` solo en `EXPENSE` ("Only expense operations can reference an obligation"). Los mensajes reflejan los de `FinancialOperationHistoryItem`. Sin campo de estado (las canceladas se excluyen). |

Todos los campos de los registros de attention son no nulos y no se validan positividades de montos. Se reutilizan `AccountSummary`, `CategorySummary`, `TransferDetails` y `OperationType`.

**Mapeo**:

* `attention`: `switch` exhaustivo sobre el `Attention` sellado del dominio, conservando el orden del calculador (lista completa).
* `upcomingCommitments`: aplana los `pendingDates()` de cada `ObligationCommitment`; ordena por `dueDate`, nombre de obligación e id; límite 5. Excluye las vencidas (ya están en attention) y solo incluye PENDING dentro del horizonte; el caso de uso no recalcula el calendario.
* `recent`: tal como lo devuelve el puerto, límite 5.

**Decisiones aprobadas**:

1. `attention` es la lista completa y ordenada; el cliente muestra la primera y puede mostrar "+N".
2. `recent` excluye operaciones canceladas.
3. `upcomingCommitments` excluye las vencidas y solo incluye PENDING dentro del horizonte.
4. Límites 5 y 5, como constantes en `GetDashboard`.
5. El horizonte (14 días) es una constante en `GetDashboard`: sin propiedad de configuración ni accesor en el calculador.
6. `IllegalArgumentException` del calculador se relanza como `IllegalStateException` (500, no 400).
7. `recent` no filtra operaciones con fecha futura (se revisa con C6).
8. `recent` vincula un gasto pagado con su obligación (`LEFT JOIN`).
9. Orden de `recent`: `operation_date DESC, created_at DESC, id DESC`.
10. Una sola rama y un solo flujo (sin 4a/4b).

**Notas de implementación**:

* Mensajes de los read models (`IllegalArgumentException`): "Horizon start cannot be after its end", "Available and shortfall cannot both be positive", "Available and shortfall are inconsistent with balance and committed", "Overdue count must be greater than zero", "A blocked payment requires an inactive account or category" y "Only expense operations can reference an obligation". `findRecentActivity` con `limit < 1` lanza "Limit must be greater than zero".
* El mensaje de `IllegalStateException` de `GetDashboard` es "Dashboard data is inconsistent: <mensaje del calculador>", con la `IllegalArgumentException` original como causa.
* `<parte>` de "Failed to query dashboard <parte>" es `obligations`, `resolutions`, `accounts`, `categories` o `recent activity`.
* La consulta de categorías no lleva `ORDER BY` (solo se indexa por id).
* El `LEFT JOIN` de la actividad reciente a `obligations` se acota por usuario (`AND ob.user_id = a.user_id`): un gasto pagado solo se vincula con una obligación del mismo usuario.

**Fuera de alcance del paso 4**: la capa `entry` (en el paso 4 no había bean `Clock`; hoy existe, ver sección 6; sin beans de adaptador ni de caso de uso, sin DTO web con centavos y sin `GET /dashboard`; hoy `GET /api/v1/dashboard` existe, en modo de un solo usuario mediante `ConfiguredSingleUserProvider` hasta que exista autenticación real), la pantalla Review y cambios de esquema o de dominio.

---

## 0. Cambios respecto a la v1

* **Ediciones**: se elige la opción (c). No se puede cambiar la programación mientras haya occurrences vencidas sin resolver (sección 3).
* **Recurrencia**: cambiar la recurrencia o `startDate` re-ancla el calendario en una fecha `>= hoy`. Las resoluciones históricas quedan válidas fuera del calendario, y `OCCURRENCE_NOT_SCHEDULED` solo se valida al crear resoluciones (sección 3).
* **Cuenta o categoría inactiva**:
  * `ModifyObligation` las rechaza.
  * `PayOccurrence` acepta una cuenta o categoría alternativa para destrabar pagos.
  * La obligación sigue contando en `committedAmount` y genera attention `PAYMENT_BLOCKED` (secciones 3, 4 y 5).
* **FK**: `obligations.account_id` y `category_id` pasan de `RESTRICT` a `NO ACTION`, y `occurrence_resolutions.obligation_id` pasa a `CASCADE`. Borrar un usuario sin operaciones sigue funcionando, verificado en SQLite (sección 7).
* **`balance` y `ACCOUNT_SHORTFALL`**: ahora tienen definición precisa, con una regla de prioridad para la attention que muestra el dashboard (sección 5).
* **Rendimiento**: el calendario es aritmético desde el ancla. Las vencidas se calculan como "calendario entre `startDate` y ayer, menos las resueltas", y la ventana histórica tiene límite al crear (sección 2).
* **Calendario**: reglas explícitas de fin de mes, bisiesto, WEEKLY y BIWEEKLY (sección 2).
* **`PayOccurrence`**:
  * Una sola transacción; la lógica de registro de gasto se comparte extrayendo `ExpenseRegistration`.
  * El `UNIQUE (obligation_id, due_date)` se traduce a `OCCURRENCE_ALREADY_RESOLVED`.
  * `operationDate` y el monto tienen reglas definidas (sección 4).
* **`CancelOperation`**: se define el contrato `findByExpenseId`/`delete`, que corre dentro de la misma transacción (sección 4).
* **Dinero**: el dominio usa `Money`; los centavos enteros son solo la representación SQL. `available` y `shortfall` se calculan sin restas negativas (sección 5).
* **Ownership**: reglas explícitas y `ResourceNotFoundException` para recursos ajenos (sección 4).
* **Migración**: script manual aditivo aparte (sección 7).
* **Preguntas abiertas**: todas cerradas y registradas como decisiones (sección 9).
* **Conflictos** con el código o las guías y la alternativa elegida para cada uno: sección 8.

---

## 1. Conceptos

| Concepto | Qué es | Ejemplo |
|---|---|---|
| **Obligation** | Compromiso de pago del usuario, único o recurrente. | Renta, 8 500.00 MXN, cada mes el día 1. |
| **Occurrence** | Una fecha concreta en la que la obligación vence. Se calcula, no se guarda. | Renta del 2026-10-01. |
| **Resolución** | Lo que pasó con una occurrence: se **pagó** (con un `Expense`) o se **omitió**. Se guarda. | La renta de octubre se pagó con el gasto X. |

Solo modela **salidas de dinero**. Los ingresos esperados quedan fuera del MVP.

---

## 2. Modelo de dominio

### Obligation (Aggregate Root)

```text
Obligation
├── id: UUID
├── userId: UUID
├── name: String                 único por usuario
├── amount: Money                monto esperado, > 0
├── accountId: UUID              cuenta desde la que se paga
├── categoryId: UUID             categoría EXPENSE del gasto al pagar
├── recurrence: Recurrence       value object: frequency + startDate (ancla) + endDate (opcional)
└── status: ObligationStatus     ACTIVE | ARCHIVED
```

Invariantes:

* `name` no vacío.
* `amount` positivo.
* `endDate >= startDate` si existe (lo valida `Recurrence`).
* No se archiva automáticamente. Una obligación ONCE ya resuelta, o con `endDate` pasado y todo resuelto, simplemente no aporta occurrences. Archivar siempre es explícito.

### Recurrence (Value Object) y reglas de calendario

```text
Recurrence
├── frequency: ONCE | WEEKLY | BIWEEKLY | MONTHLY | YEARLY
├── startDate: LocalDate         ancla del calendario vigente (primera fecha de vencimiento)
└── endDate: LocalDate | null    última fecha posible, inclusiva (opcional)
```

La fecha `n` (n = 0, 1, 2, …) se calcula **siempre desde el ancla** (`startDate`), nunca desde la fecha anterior ya ajustada:

| Frecuencia | Fecha `n` | Notas |
|---|---|---|
| ONCE | `startDate` (solo `n = 0`) | |
| WEEKLY | `startDate + 7·n días` | No depende del mes. |
| BIWEEKLY | `startDate + 14·n días` | No depende del mes. La quincena por fechas fijas (15 y último) se modela con dos obligaciones MONTHLY. |
| MONTHLY | `startDate.plusMonths(n)` | Si el día no existe, se usa el último día del mes. Ancla 31: 31 ene → 28/29 feb → 31 mar → 30 abr → 31 may. |
| YEARLY | `startDate.plusYears(n)` | Ancla 29 feb: 28 feb en años no bisiestos y 29 feb en bisiestos. |

`plusMonths` y `plusYears` de `java.time` ya ajustan al último día válido. Como siempre se calcula desde el ancla, el ajuste no se acumula.

Ninguna fecha es anterior a `startDate` ni posterior a `endDate`, si existe; `endDate` es inclusivo.

**Operaciones puras y aritméticas** (sin reloj ni I/O, sin iterar la historia):

* `countBetween(from, to)`: calcula en O(1) el primer y el último índice `n` dentro de `[from, to]`. Para WEEKLY y BIWEEKLY es división entera de días; para MONTHLY y YEARLY se estima con `ChronoUnit.MONTHS/YEARS.between` desde el ancla y se ajusta ±1.
* `datesBetween(from, to)`: parte del primer índice calculado aritméticamente y recorre solo las fechas del rango. Es O(k), con k = fechas devueltas.
* `isScheduled(date)`: comprueba en O(1) que la fecha pertenezca al calendario vigente.

### Occurrences: calculadas; solo se persisten resoluciones

```text
OccurrenceResolution
├── id: UUID
├── obligationId: UUID
├── dueDate: LocalDate          identifica la occurrence (única por obligación)
├── status: PAID | SKIPPED
├── expenseId: UUID | null      obligatorio si PAID, nulo si SKIPPED
└── resolvedAt: Instant
```

Estado de una occurrence del calendario vigente, dado `hoy`:

* **PAID / SKIPPED**: existe una resolución con esa `dueDate`.
* **OVERDUE**: no existe resolución y `dueDate < hoy`.
* **PENDING**: no existe resolución y `dueDate >= hoy`.

**Vencidas** = fechas del calendario entre `startDate` y `hoy − 1`, menos las fechas resueltas que **están en** el calendario vigente (una resolución fuera del calendario no resta). Con la decisión de la sección 3, todas las vencidas sin resolver comparten el monto, la cuenta y la categoría actuales de la obligación. Por eso:

* `overdueCount = countBetween(startDate, hoy − 1) − (fechas resueltas en ese rango que pertenecen al calendario)`, en O(R), con R = resoluciones de la obligación (`Recurrence.countUnresolvedBetween`). No hay consulta de conteo: los casos de uso de comandos cargan las resoluciones por obligación y el dashboard (paso 4) carga en una sola consulta las de todas las obligaciones activas del usuario.
* `overdueAmount = amount × overdueCount`, con `Money.multiply(long factor)`, `factor >= 0` (sección 8).
* `oldestOverdue` (para ordenar attention) se obtiene recorriendo el calendario desde `startDate` y saltando las fechas resueltas. Visita a lo sumo R + 1 fechas.

**Ventana histórica**:

* `CreateObligation` rechaza un `startDate` anterior a `hoy − 31 días` (`IllegalArgumentException`). Una obligación nueva no puede arrastrar años de vencidas.
* Las vencidas **nunca se descartan en silencio**: siguen contando hasta que se resuelven.
* Si alguien las ignora mucho tiempo, `SkipOverdueOccurrences` las omite todas de una vez; inserta una fila por fecha, a lo sumo unas cientos (10 años semanales ≈ 522).

### Nuevos `BusinessRuleCode`

| Código | Cuándo |
|---|---|
| `OBLIGATION_NAME_ALREADY_EXISTS` | Nombre repetido para el usuario, incluida la carrera contra `UNIQUE (user_id, name)`, traducida en el adaptador como en cuentas. |
| `OBLIGATION_ARCHIVED` | Modificar, pagar, omitir o reabrir en una obligación archivada. |
| `OBLIGATION_HAS_OVERDUE_OCCURRENCES` | Cambiar la programación o archivar mientras haya vencidas sin resolver, o cambiar la recurrencia si el calendario resultante tendría vencidas. |
| `OCCURRENCE_ALREADY_RESOLVED` | Pagar u omitir una occurrence con resolución, incluida la carrera contra `UNIQUE (obligation_id, due_date)`. |
| `OCCURRENCE_PAID_NOT_REOPENABLE` | Reabrir una occurrence `PAID`: solo se reabre cancelando su gasto ("Paid occurrence can only be reopened by cancelling its expense"). |
| `OCCURRENCE_NOT_SCHEDULED` | Solo al **crear** una resolución: la fecha no pertenece al calendario vigente. |
| `OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION` | `changeRecurrence` chocaría con una resolución con `dueDate >= hoy`: el nuevo `startDate` es posterior, el nuevo `endDate` es anterior, o la resolución no está en el calendario nuevo. |

Estado tras el paso 2: `JdbcObligationRepository` lanza `OBLIGATION_NAME_ALREADY_EXISTS` y `JdbcOccurrenceResolutionRepository` lanza `OCCURRENCE_ALREADY_RESOLVED` (carreras contra los `UNIQUE`). Tras el paso 3a, `CreateObligation` y `ModifyObligation` también lanzan `OBLIGATION_NAME_ALREADY_EXISTS`; tras el paso 3b, `SkipOccurrence` también lanza `OCCURRENCE_ALREADY_RESOLVED` y `ReopenOccurrence` lanza `OCCURRENCE_PAID_NOT_REOPENABLE`; tras el paso 3c, `PayOccurrence` también lanza `OCCURRENCE_ALREADY_RESOLVED`.

Al pagar se reutilizan `ACCOUNT_INACTIVE`, `CATEGORY_INACTIVE` e `INSUFFICIENT_BALANCE` de `Expense.register`.

---

## 3. Ediciones, resoluciones y calendario

### Opción elegida: (c) bloquear la programación mientras haya vencidas

"Programación" es todo menos el nombre: monto, recurrencia, `startDate`, `endDate`, cuenta y categoría.

* `ModifyObligation` con cambios de programación exige **cero vencidas sin resolver**; si hay alguna, lanza `OBLIGATION_HAS_OVERDUE_OCCURRENCES`.
* El usuario las resuelve antes: las paga, las omite o hace `SkipOverdueOccurrences`.
* Cambiar solo el nombre está permitido aunque haya vencidas (no en una obligación archivada).

**Por qué (c) y no (a) o (b)**:

* Con (c), toda occurrence no resuelta tiene `dueDate >= hoy` en el momento de editar. El cambio "solo afecta vencimientos con `dueDate >= hoy`" por construcción, y las vencidas conservan los datos con los que vencieron sin guardar nada extra.
* (a), versiones con `effective_from`, añade una tabla de versiones y resolver qué versión aplica a cada fecha. Es más esquema y más casos de prueba para un MVP.
* (b), snapshot al vencer, necesita un proceso que "congele" al llegar la fecha, y el proyecto no tiene scheduler.
* Costo de (c): fricción cuando hay vencidas. Se mitiga con `SkipOverdueOccurrences` y con el pago con cuenta alternativa (decisión 3).

### Cambios de recurrencia y resoluciones existentes

* Las resoluciones **pasadas** (`dueDate < hoy`) **siguen siendo válidas** aunque el calendario nuevo ya no genere su fecha. El dominio no exige que la `dueDate` de una resolución histórica pertenezca al calendario actual. Las resoluciones futuras (`dueDate >= hoy`) sí deben estar en el calendario nuevo (regla de `ModifyObligation`, más abajo).
* `OCCURRENCE_NOT_SCHEDULED` se valida **solo al crear una resolución nueva**, contra el calendario vigente.

Reglas de `ModifyObligation`:

| Cambio | Permitido si… |
|---|---|
| Nombre | Obligación no archivada (y único). No depende de las vencidas. |
| Monto, cuenta, categoría | No archivada y sin vencidas. Aplica a todas las no resueltas, que son todas `>= hoy`. |
| Recurrencia o `startDate` | Sin vencidas **y** el nuevo `startDate >= hoy` **y** el nuevo `startDate` no es posterior a ninguna resolución con `dueDate >= hoy` (pagos adelantados) **y** toda resolución con `dueDate >= hoy` está en el calendario nuevo. |
| `endDate` | Sin vencidas **y** `endDate >= hoy − 1` **y** `endDate` no es anterior a ninguna resolución con `dueDate >= hoy`. |
| Cualquier cambio de recurrencia | El calendario resultante no tiene vencidas sin resolver (cubre quitar o extender un `endDate` ya pasado). |

Todos los métodos de edición (`rename`, `changeAmount`, `changePaymentSource`, `changeRecurrence`, `archive`) fallan con `OBLIGATION_ARCHIVED` si la obligación está archivada. Todos menos `rename` exigen además cero vencidas (`OBLIGATION_HAS_OVERDUE_OCCURRENCES`, "Obligation has overdue occurrences").

**Orden de comprobaciones de `changeRecurrence`** (`Obligation.changeRecurrence`):

1. Obligación no archivada (`OBLIGATION_ARCHIVED`).
2. Sin vencidas en el calendario actual (`OBLIGATION_HAS_OVERDUE_OCCURRENCES`).
3. Si cambian la frecuencia o el `startDate` (re-anclaje): `startDate >= hoy` (`IllegalArgumentException`, "Start date cannot be before today") y `startDate` no posterior a una resolución con `dueDate >= hoy` (`OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION`).
4. Si hay `endDate` y cambia: `endDate >= hoy − 1` (`IllegalArgumentException`, "End date cannot be before yesterday") y `endDate` no anterior a una resolución con `dueDate >= hoy` (`OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION`). Quitar el `endDate` no pasa por esta comprobación.
5. Si hubo re-anclaje: toda resolución con `dueDate >= hoy` está en el calendario nuevo (`OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION`, "Upcoming resolution is not on the new calendar").
6. El calendario resultante no tiene vencidas sin resolver (`OBLIGATION_HAS_OVERDUE_OCCURRENCES`, "Change would create overdue occurrences").

**Por qué el nuevo `startDate` debe ser `>= hoy`**: `startDate` es el ancla del calendario vigente. Si se cambia la recurrencia dejando el ancla en el pasado, el calendario nuevo genera fechas pasadas sin resolución, que aparecerían como vencidas falsas. Re-anclar en `hoy` o después evita eso; las resoluciones anteriores quedan como historia fuera del calendario. Ver en la sección 8 la diferencia con el ejemplo de la revisión.

### Cuenta o categoría desactivada después de crear la obligación

* `CreateObligation` y `ModifyObligation` rechazan una cuenta o categoría inactiva con `ACCOUNT_INACTIVE` / `CATEGORY_INACTIVE`, y una categoría que no sea `EXPENSE` con `IllegalArgumentException`. Son las mismas reglas y el mismo orden que `Expense.register`.
* Pagar sigue exigiendo cuenta y categoría activas, porque se usa `Expense.register`.
* **Recuperación**, cualquiera de estas:
  1. reactivar la cuenta (hoy ningún caso de uso cambia el estado de una cuenta, `ModifyAccount` solo la renombra; es una brecha planificada);
  2. pagar con `PayOccurrence` indicando una cuenta o categoría alternativa activa;
  3. omitir las vencidas y luego `ModifyObligation` para apuntar a otra cuenta o categoría;
  4. resolver las vencidas y archivar.
* **Cálculos**: la obligación sigue contando en `committedAmount` porque sigue siendo un compromiso, y genera la attention `PAYMENT_BLOCKED`. Queda fuera de `ACCOUNT_SHORTFALL` (sección 5).

---

## 4. Casos de uso (commands)

Todos cargan la obligación con `findByIdAndUserId`; una obligación ajena o inexistente lanza `ResourceNotFoundException`, no una violación de regla. `hoy` se obtiene de un `java.time.Clock` inyectado.

| Caso de uso | Efecto y reglas |
|---|---|
| `CreateObligation` | Carga cuenta y categoría con `findByIdAndUserId(…, userId)` (ajenas: `ResourceNotFoundException`). Aplica las reglas de la sección 3 y el límite de `startDate >= hoy − 31 días`. Crea la obligación `ACTIVE`. |
| `ModifyObligation` | Mismas cargas por usuario y las reglas de la sección 3. |
| `ArchiveObligation` | Exige cero vencidas (`OBLIGATION_HAS_OVERDUE_OCCURRENCES`). Pasa a `ARCHIVED`. |
| `PayOccurrence` | Ver abajo. Devuelve el id del gasto (paso 3, decisión 8). |
| `SkipOccurrence(obligationId, dueDate)` | Obligación `ACTIVE`, fecha en el calendario, sin resolución. Guarda `SKIPPED`. |
| `SkipOverdueOccurrences(obligationId)` | Guarda `SKIPPED` para todas las vencidas, en una transacción. |
| `ReopenOccurrence(obligationId, dueDate)` | Solo para `SKIPPED`: borra la resolución. Una `PAID` se reabre únicamente cancelando su gasto. |

### `PayOccurrence(userId, obligationId, dueDate, amount?, operationDate?, accountId?, categoryId?)`

Una **única** llamada a `TransactionManager.execute`, sin anidar `RegisterExpense`. Pasos (el orden implementado, con `operationDate` primero, está en "Cambios durante el paso 3", decisión 8):

1. Cargar la obligación por id y usuario (`ResourceNotFoundException`).
2. `ACTIVE` (`OBLIGATION_ARCHIVED`).
3. `isScheduled(dueDate)` (`OCCURRENCE_NOT_SCHEDULED`). Se permite pagar fechas futuras del calendario (pago adelantado).
4. Sin resolución previa (`OCCURRENCE_ALREADY_RESOLVED`).
5. Registrar el gasto con `ExpenseRegistration` (ver abajo). Usa la cuenta y la categoría de la obligación, o las alternativas indicadas; ambas se cargan por usuario y pasan por `Expense.register`.
6. Crear la resolución `PAID` con el `expenseId`.

Valores del gasto:

* **`operationDate`**: por defecto `hoy`. Si se indica, no puede ser posterior a `hoy` (`IllegalArgumentException`).
* **`amount`**: por defecto el monto esperado de la obligación. Si se indica, debe ser positivo. **Se registra el monto real**. La occurrence queda `PAID` y deja de contar en `committedAmount`, sea cual sea la diferencia.

Si otra petición resolvió la misma fecha entre el paso 4 y el 6, el `UNIQUE (obligation_id, due_date)` falla en el paso 6:

* `JdbcOccurrenceResolutionRepository` lo traduce a `OCCURRENCE_ALREADY_RESOLVED`, con el `SQLiteException` como causa, usando `SqliteConstraintViolations` con las columnas `occurrence_resolutions.obligation_id, occurrence_resolutions.due_date`.
* El `UNIQUE (expense_id)` es otra restricción y no se confunde con esta.
* La transacción revierte el gasto y el cambio de saldo. **Prueba requerida**: provocar la carrera insertando la resolución por fuera y verificar que no queda gasto ni cambio de saldo.

**Registro de gasto compartido**: se elige **extraer** `ExpenseRegistration`, un componente de aplicación sin transacción propia que:

* carga cuenta y categoría por usuario;
* llama `Expense.register`;
* debita la cuenta;
* guarda el gasto y la cuenta.

`RegisterExpense` pasa a ser `transactionManager.execute(() -> expenseRegistration.register(…))` con el mismo comportamiento, y `PayOccurrence` lo usa dentro de su transacción.

* Por qué no duplicar: `java.md` pide no duplicar lógica que representa el mismo concepto, y son dos llamadores reales. Eso cumple "no introducir abstracciones sin una necesidad concreta" de `application.md`.
* Por qué no llamar a `RegisterExpense`: abriría una transacción anidada, que `JdbcTransactionManager` rechaza.

### Integración con `CancelOperation`

Contrato de `OccurrenceResolutionRepository` (entre otros métodos):

```text
Optional<OccurrenceResolution> findByExpenseId(UUID expenseId)
void delete(UUID resolutionId)
```

Al cancelar un `Expense`, en la **misma** `transactionManager.execute` que ya usa `CancelOperation`, después de revertir el saldo y registrar el `Reversal`:

```text
findByExpenseId(expense.id()).ifPresent(r -> delete(r.id()))
```

La occurrence vuelve a PENDING, o a OVERDUE si ya venció. **Prueba requerida**: si la cancelación falla (por ejemplo, un fallo del repositorio), la resolución permanece.

---

## 5. Cálculos

### Dinero

* El dominio y la aplicación usan siempre `Money` (`BigDecimal`, escala 2, nunca negativo). Los **centavos enteros solo existen en SQL y JDBC** (`INTEGER`), igual que en `accounts.balance`.
* `Money.subtract` rechaza resultados negativos. Por eso `available` y `shortfall` **comparan primero y restan en el sentido no negativo**:

```text
si balance >= committed:  available = balance.subtract(committed);  shortfall = Money.ofCents(0)
si no:                    available = Money.ofCents(0);            shortfall = committed.subtract(balance)
```

(Equivale a `available = max(0, balance − committed)` y `shortfall = max(0, committed − balance)`.)

### Horizonte y montos

* `hoy = LocalDate.now(clock)`, con un `Clock` en zona `America/Mexico_City`.
* Horizonte de 14 días: `[hoy, hoy + 13]`, ambos inclusivos.

```text
committed(o)    = amount × (# PENDING de o con dueDate en el horizonte) + overdueAmount(o)
committedAmount = Σ committed(o) de las obligaciones ACTIVE, incluidas las de cuenta o categoría inactiva
balance         = Σ balance de las cuentas ACTIVE del usuario
available, shortfall: como arriba, con committedAmount y balance
```

* Una obligación ARCHIVED no aporta. `startDate` futura y `endDate` pasada se respetan.
* Las transferencias no afectan `committedAmount` ni el `balance` total.

### Por cuenta (`ACCOUNT_SHORTFALL`)

Para cada cuenta **ACTIVE** `a`:

```text
committed(a) = Σ committed(o) de las obligaciones ACTIVE con accountId = a, cuenta ACTIVE y categoría ACTIVE
shortfall(a) = committed(a) − balance(a), si committed(a) > balance(a)
```

* Cuenta inactiva: su saldo no cuenta en `balance` ni se evalúa aquí.
* Sus obligaciones siguen en `committedAmount` y generan `PAYMENT_BLOCKED`.

### Attention y prioridad

`CommitmentCalculator` devuelve la lista **completa y ordenada**; el dashboard muestra la primera.

| Prioridad | Tipo | Condición | Orden y desempate dentro del tipo |
|---|---|---|---|
| 1 | `OVERDUE_OCCURRENCE` | Una por obligación con vencidas (lleva `oldestOverdue` y `overdueCount`). | `oldestOverdue` más antigua → mayor `overdueAmount` → nombre de obligación → id. |
| 2 | `SHORTFALL` | `shortfall > 0` (global). | Única. |
| 3 | `PAYMENT_BLOCKED` | Obligación con compromisos en el horizonte (o vencidas) cuya cuenta o categoría está inactiva. | Fecha comprometida más próxima (la vencida más antigua o la PENDING más cercana) → mayor `committed(o)` → nombre → id. |
| 4 | `ACCOUNT_SHORTFALL` | `shortfall(a) > 0` en una cuenta ACTIVE. | Mayor `shortfall(a)` → fecha comprometida más próxima de la cuenta → nombre de cuenta → id. |

Razón del orden:
1. Lo vencido ya está atrasado.
2. El faltante global significa que no alcanza en ningún caso.
3. El pago bloqueado requiere que el usuario cambie la configuración.
4. El faltante por cuenta se resuelve con una transferencia.

---

## 6. Encaje en la arquitectura

* **Domain**:
  * `Obligation`, `ObligationStatus`, `Recurrence`, `Frequency`, `OccurrenceResolution`, `ResolutionStatus`.
  * `CommitmentCalculator`: puro; recibe snapshots (`ObligationSnapshot`, `ResolutionSnapshot`, `AccountSnapshot`, `CategorySnapshot`), `hoy` y, por construcción, el horizonte; devuelve `CommitmentSummary` (montos, `ObligationCommitment`, `AccountCommitment` y attention).
  * `OperationReferences` (paquete privado): validación compartida de `Income`, `Expense` y `Obligation`.
  * `Money.multiply(long)`.
  * Los nuevos códigos.
  * Ninguna clase de dominio llama a `now()`: `hoy` entra como parámetro.
* **Application**:
  * Los casos de uso de la sección 4 y `ExpenseRegistration`.
  * Puertos `ObligationRepository` (`create`, `update`, `findByIdAndUserId`, `existsByUserIdAndName…`) y `OccurrenceResolutionRepository` (`create`, `delete`, `findByObligationId`, `findByObligationIdAndDueDate`, `findByExpenseId`).
  * `java.time.Clock` inyectado por constructor. Nunca `LocalDate.now()` sin reloj.
* **Infrastructure**:
  * `JdbcObligationRepository`, que traduce el `UNIQUE (user_id, name)` a `OBLIGATION_NAME_ALREADY_EXISTS`.
  * `JdbcOccurrenceResolutionRepository`, que traduce el `UNIQUE (obligation_id, due_date)` a `OCCURRENCE_ALREADY_RESOLVED`.
  * Ambos siguen las reglas de mappers (`CorruptedPersistedDataException`).
* **Entry**: **sin cambios** en esta fase del Core (texto histórico). Hoy la capa entry ya cablea todos los casos de uso de obligaciones y el dashboard (`ObligationUseCaseConfiguration`, `DashboardUseCaseConfiguration`) con el bean `Clock.system(...)` de `entry.config.ClockConfiguration` (`personal-finance.time-zone`, por defecto `America/Mexico_City`); en pruebas se usa `Clock.fixed`.
* **Dashboard (paso 4, implementado; `GET /api/v1/dashboard` existe)**: `DashboardQueryPort` lee con SQL las obligaciones activas, una sola consulta con todas las resoluciones de las obligaciones activas del usuario, las cuentas, las categorías y la actividad reciente (ver "Paso 4: diseño aprobado e implementado"). La consulta de aplicación usa `CommitmentCalculator`. No invoca otros casos de uso ni reutiliza `FinancialOperationQueryPort`.

---

## 7. Esquema, borrado de usuarios y migración

```sql
CREATE TABLE obligations (
    id          TEXT    PRIMARY KEY NOT NULL,
    user_id     TEXT    NOT NULL,
    name        TEXT    NOT NULL,
    amount      INTEGER NOT NULL CHECK (amount > 0),        -- centavos (solo en SQL)
    account_id  TEXT    NOT NULL,
    category_id TEXT    NOT NULL,
    frequency   TEXT    NOT NULL,
    start_date  TEXT    NOT NULL,
    end_date    TEXT,
    status      TEXT    NOT NULL,
    created_at  TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    FOREIGN KEY (user_id)     REFERENCES users (id)      ON DELETE CASCADE,
    FOREIGN KEY (account_id)  REFERENCES accounts (id),     -- NO ACTION
    FOREIGN KEY (category_id) REFERENCES categories (id),   -- NO ACTION
    UNIQUE (user_id, name),
    CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE occurrence_resolutions (
    id            TEXT PRIMARY KEY NOT NULL,
    obligation_id TEXT NOT NULL,
    due_date      TEXT NOT NULL,
    status        TEXT NOT NULL,
    expense_id    TEXT UNIQUE,
    resolved_at   TEXT NOT NULL,
    created_at    TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    FOREIGN KEY (obligation_id) REFERENCES obligations (id)        ON DELETE CASCADE,
    FOREIGN KEY (expense_id)    REFERENCES expense_operations (id) ON DELETE RESTRICT,
    UNIQUE (obligation_id, due_date),
    CHECK ((status = 'PAID' AND expense_id IS NOT NULL) OR (status = 'SKIPPED' AND expense_id IS NULL))
);

CREATE INDEX idx_obligations_user_status ON obligations (user_id, status);
CREATE INDEX idx_obligations_account     ON obligations (account_id);
CREATE INDEX idx_obligations_category    ON obligations (category_id);
```

### Borrado de usuarios

Comportamiento actual: `users → accounts/categories` es `CASCADE`, y las operaciones apuntan a cuentas y categorías con `RESTRICT`. Borrar un usuario con operaciones falla, como cubre `JdbcUserRepositoryTest.failsToDeleteUserWhoseAccountIsReferencedByOperation`.

Comportamiento con esta propuesta:

| Caso | Resultado |
|---|---|
| Usuario sin operaciones, con obligaciones y resoluciones `SKIPPED` | Se borra todo en cascada: usuario → cuentas, categorías y obligaciones → resoluciones. |
| Usuario con un pago (`PAID`) | Falla, igual que hoy con cualquier gasto: el `Expense` bloquea el borrado de la cuenta. No se borra nada. |
| Borrado directo de una cuenta o categoría referenciada por una obligación | Falla (`NO ACTION`). |
| Borrado directo de un `Expense` referenciado por una resolución | Falla (`RESTRICT` en `expense_id`). |

* **Por qué `NO ACTION` y no `RESTRICT`** en `obligations.account_id`/`category_id`:
  * `RESTRICT` falla en cuanto se borra la fila padre, aunque la fila hija también vaya a borrarse en la misma sentencia. Si SQLite procesa la cascada de `accounts` antes que la de `obligations`, el borrado del usuario falla.
  * `NO ACTION` se verifica al final de la sentencia, así que no depende del orden.
* **Lo que se comprobó en SQLite** (sqlite-jdbc 3.53.4.0, `PRAGMA foreign_keys = ON`):
  * Con `NO ACTION`, el usuario se borra y el borrado directo de la cuenta sigue fallando.
  * Con `RESTRICT` el borrado también pasó en esa prueba, pero depende del orden interno de la cascada, que SQLite no garantiza.
* **Pruebas requeridas** en `JdbcUserRepositoryTest`:
  * el usuario con obligación y resolución `SKIPPED` se borra por completo;
  * el usuario con resolución `PAID` falla y no se borra nada;
  * el borrado directo de una cuenta con obligación falla.

### Migración

* `schema.sql` recibe las tablas nuevas, porque es el esquema completo para bases nuevas y para las pruebas. Hoy **solo las pruebas** lo cargan; la aplicación no inicializa el esquema (`architecture.md`), y eso no cambia.
* Script manual aparte, **aditivo** (solo `CREATE TABLE` e `CREATE INDEX`, sin `DROP`, `ALTER` ni `DELETE`): `src/main/resources/db/migrations/001-obligations-core.sql`. Es para bases existentes y se aplica una vez, a mano, con la aplicación detenida. Va envuelto en `BEGIN`/`COMMIT`: corre como una sola transacción.
* Documentación operativa: la sección "Run the app" de `CLAUDE.md` indicará que, en una base existente, se aplican en orden los scripts de `db/migrations/` que falten.

---

## 8. Conflictos con el código o las guías

| # | Conflicto | Alternativa elegida |
|---|---|---|
| C1 | `CancelOperation` usaba `Instant.now()` directo. Chocaba con "nunca `now()` sin `Clock` en aplicación". | **Resuelto en el paso 3c**: `CancelOperation` recibe `Clock` como último parámetro del constructor y usa `Instant.now(clock)`. |
| C2 | La opción (c) más "pagar exige cuenta activa" podía dejar al usuario sin salida: no puede pagar la vencida con la cuenta inactiva ni cambiar la cuenta mientras haya vencidas. | `PayOccurrence` acepta `accountId`/`categoryId` alternativos, cargados por usuario y validados por `Expense.register`. También se puede omitir la ocurrencia; reactivar la cuenta será posible cuando exista un caso de uso para cambiar su estado (hoy no existe). |
| C3 | El ejemplo de la revisión ("`startDate` no puede moverse después de una occurrence resuelta") choca con re-anclar el calendario en `>= hoy`: las resoluciones pasadas siempre quedarían antes del ancla nueva. | La regla aplica a las resoluciones con `dueDate >= hoy` (pagos adelantados). Las pasadas son historia válida fuera del calendario (decisión 2). Sin re-anclar aparecerían vencidas falsas. |
| C4 | La v1 proponía `RESTRICT` en `obligations.account_id`/`category_id`. | `NO ACTION` (sección 7). |
| C5 | `Money` no tiene multiplicación. | Agregar `Money.multiply(long factor)`, que rechaza `factor < 0`. |
| C6 | `RegisterExpense` no valida que `operationDate` no sea futura; `PayOccurrence` sí lo haría. | Se acepta la asimetría en esta fase. Aplicar la misma regla a `RegisterExpense` y `RegisterIncome` es un cambio aparte, porque altera un comportamiento existente. |
| C7 | No existe un caso de uso para desactivar categorías: `CATEGORY_INACTIVE` al pagar solo ocurre con datos cambiados fuera de la app o con un caso de uso futuro. | Se implementa igual (el dominio ya lo valida) y la recuperación por categoría queda cubierta por la categoría alternativa. |
| C8 | `application.md`: "no introducir abstracciones sin una necesidad concreta". | `ExpenseRegistration` tiene dos llamadores reales y evita una transacción anidada (sección 4). |

---

## 9. Decisiones tomadas (antes preguntas abiertas)

| # | Decisión |
|---|---|
| P1 | Solo salidas de dinero; los ingresos esperados quedan fuera del MVP. |
| P2 | Nombre único por usuario (`OBLIGATION_NAME_ALREADY_EXISTS`). |
| P3 | ONCE, WEEKLY, BIWEEKLY (14 días), MONTHLY y YEARLY, calculadas desde el ancla. La quincena por fechas fijas se modela con dos MONTHLY. |
| P4 | Occurrences calculadas; solo se persisten resoluciones. |
| P5 | Se puede pagar un monto distinto; se registra el real y la occurrence queda PAID. |
| P6 | Cancelar el gasto de un pago borra su resolución en la misma transacción. |
| P7 | `hoy` sale de un `java.time.Clock` inyectado por constructor en los casos de uso, con zona `America/Mexico_City`. Es configurable en `entry` (`personal-finance.time-zone`, bean `Clock` en `ClockConfiguration`). Nunca `LocalDate.now()` sin reloj en dominio ni aplicación. |
| P8 | Las vencidas cuentan en `committedAmount`. |
| P9 | `balance` = suma de cuentas ACTIVE. Las obligaciones con cuenta inactiva cuentan en `committedAmount`; `ACCOUNT_SHORTFALL` solo evalúa cuentas ACTIVE (sección 5). |
| P10 | Attention: `OVERDUE_OCCURRENCE`, `SHORTFALL`, `PAYMENT_BLOCKED` y `ACCOUNT_SHORTFALL`, en ese orden de prioridad y con los desempates de la sección 5. |
| P11 | Script manual aditivo en `db/migrations/` y `schema.sql` actualizado para bases nuevas (sección 7). |
| P12 | No se vinculan gastos existentes a occurrences en el MVP; se paga con `PayOccurrence`. |

---

## 10. Plan de implementación (cada paso con el flujo completo de agentes)

1. **Dominio**: `Recurrence`, `Obligation`, `OccurrenceResolution`, códigos, `Money.multiply` y `CommitmentCalculator`. Pruebas de calendario:
   * año bisiesto y ancla 29 feb;
   * ancla 31 ene → feb → mar;
   * `endDate` exacto e inclusivo;
   * ONCE;
   * obligación archivada;
   * `startDate` futura;
   * WEEKLY y BIWEEKLY cruzando meses;
   * rangos largos (10 años semanales, conteo exacto sin iterar).

   Pruebas de cálculo:
   * vencidas, horizonte y dinero sin restas negativas;
   * `PAYMENT_BLOCKED`, `ACCOUNT_SHORTFALL`;
   * orden de attention.
2. **Persistencia**:
   * tablas en `schema.sql` y el script `001-obligations-core.sql`;
   * puertos y repositorios JDBC con traducción de `UNIQUE`;
   * pruebas de borrado de usuario (sección 7) contra SQLite real.
3. **Casos de uso**: Create, Modify, Archive, Pay, Skip, SkipOverdue y Reopen, más `ExpenseRegistration` y el ajuste de `CancelOperation` (resoluciones y `Clock`). Pruebas:
   * ownership (`ResourceNotFoundException`);
   * carrera en `PayOccurrence` con rollback del gasto;
   * cancelación que falla y conserva la resolución.
4. **Dashboard** (implementado): `DashboardQueryPort`, read models (incluido `RecentActivityItem`), adaptador JDBC y consulta de aplicación (`GetDashboard`). `GET /api/v1/dashboard` existe (modo de un solo usuario mediante `ConfiguredSingleUserProvider` hasta que exista autenticación real); Review sigue planificado.

Fuera de alcance: la pantalla Review y su endpoint (el endpoint del Dashboard existe), notificaciones, ingresos esperados, tarjetas de crédito, montos estimados variables y moneda distinta de MXN.
