# Dashboard y Review: read models planificados

Estado: **planificado, NO implementado**. Este documento fija los límites previstos para que el trabajo futuro los respete.

## Pantallas

### Dashboard ("¿qué hago ahora?")

* `availableToSpend`
* `horizon`
* `attention`
* `upcomingCommitments` (pocos elementos)
* `recent`

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

* El dinero se expresa en centavos enteros. El backend es la autoridad de los cálculos.
* `available = max(0, balance - committedAmount)`.
* `shortfall = max(0, committedAmount - balance)`.
* Horizonte inicial de 14 días. Las fechas las controla el servidor.
* Las transferencias no cuentan ni como ingreso ni como gasto.
* Los gastos sin categoría aparecen como "Sin categoría" en el desglose.

## Prerrequisitos y estado

* El Core de Obligations debe completarse antes de poder calcular compromisos, Available-to-Spend, faltantes (shortfalls) y attention.
* Los endpoints `GET /dashboard` y `GET /review` no se publican a propósito: sin stubs, datos falsos, valores por defecto ni respuestas "not implemented".
* El usuario autenticado provendrá de `CurrentUserProvider` cuando exista un mecanismo de autenticación.
