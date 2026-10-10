# Dashboard y Review: read models planificados

Estado: el lado de lectura del **Dashboard** está **implementado** (paso 4 de [`obligations-core-proposal.md`](./obligations-core-proposal.md); ver "Paso 4: diseño aprobado e implementado"), y `GET /api/v1/dashboard` existe (modo de un solo usuario). **Review sigue planificado, NO implementado**. Este documento fija los límites previstos para que el trabajo futuro los respete.

## Pantallas

### Dashboard ("¿qué hago ahora?")

Read model `Dashboard`:

* `horizon` (`DashboardHorizon`: `from` y `to`, `[hoy, hoy + 13]`)
* `availableToSpend` (`AvailableToSpend`: `balance`, `committed`, `available`, `shortfall`)
* `attention` (lista completa y ordenada de `AttentionItem`: `OverdueOccurrence`, `Shortfall`, `PaymentBlocked`, `AccountShortfall`)
* `upcomingCommitments` (`UpcomingCommitment`, máximo 5, solo PENDING dentro del horizonte)
* `recent` (`RecentActivityItem`, máximo 5, solo operaciones activas)

### Review ("¿qué está pasando?")

* `period`
* `summary` (ingresos, gastos y neto)
* `spending.byCategory`
* `notableOperations`
* `upcomingCommitments`
* `availableToSpend`

## Límites de arquitectura

* Contratos propios en `application.port.out`: `DashboardQueryPort` y `ReviewQueryPort`.
* Read models propios en `application.readmodel`.
* Adaptadores de lectura JDBC en `infrastructure.persistence`.
* Controllers en `entry.web`.
* No se componen invocando otros casos de uso.
* No se reutiliza `FinancialOperationQueryPort` como contrato.
* Para la actividad reciente se prefiere un read model de UX separado (por ejemplo `RecentActivityItem`) en lugar de extender `FinancialOperationHistoryItem`.

## Reglas

* En el contrato de lectura (formato de la API) el dinero se expresa en centavos enteros. El dominio y la aplicación usan `Money`; los centavos enteros también son la representación SQL (ver [`obligations-core-proposal.md`](./obligations-core-proposal.md), sección 5). El backend es la autoridad de los cálculos.
* `available = max(0, balance - committedAmount)`.
* `shortfall = max(0, committedAmount - balance)`.
* Horizonte inicial de 14 días. Las fechas las controla el servidor.
* Las transferencias no cuentan ni como ingreso ni como gasto.
* Todo gasto tiene categoría (`expense_operations.category_id` es `NOT NULL`), así que el desglose por categoría no necesita un grupo "Sin categoría".

## Prerrequisitos y estado

* El Core de Obligations está completo y el cálculo de compromisos, Available-to-Spend, faltantes (shortfalls) y attention existe (`CommitmentCalculator`, usado por `GetDashboard`).
* `GET /api/v1/dashboard` existe (modo de un solo usuario mediante `ConfiguredSingleUserProvider`, hasta que exista autenticación real); el usuario siempre proviene de `CurrentUserProvider`.
* `GET /review` sigue planificado y no se publica a propósito: sin stubs, datos falsos, valores por defecto ni respuestas "not implemented".
