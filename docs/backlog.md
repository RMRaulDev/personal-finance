# Backlog del backend

Estado al 2026-10-05: el Core (dominio, aplicación, persistencia) y la capa entry (API REST bajo `/api/v1`, ramas E1–E5, E2b y E4c) están completos. Este documento reúne lo que quedó pendiente, con el contexto necesario para retomarlo sin releer el historial.

Referencias: [`.github/instructions/architecture.md`](../.github/instructions/architecture.md) (capas, API REST, configuración), [`.github/instructions/application.md`](../.github/instructions/application.md) (casos de uso, errores), [`obligations-core-proposal.md`](./obligations-core-proposal.md) (diseño de obligaciones y dashboard), [`dashboard-review-read-models.md`](./dashboard-review-read-models.md) (pantallas Dashboard y Review).

Cada cambio de comportamiento sigue el flujo completo de agentes de `CLAUDE.md` (code-implementer → test-writer → code-reviewer → docs-maintainer → `mvn test`).

---

## 1. Fechas futuras en `RegisterExpense` y `RegisterIncome` (conflicto C6)

**Situación:** `PayOccurrence` rechaza un `operationDate` posterior a hoy (`IllegalArgumentException` → 400), pero `RegisterExpense` y `RegisterIncome` lo aceptan. `RegisterTransfer` tampoco valida.

**Por qué importa:** una operación futura afecta saldos de inmediato y aparece en `recent` del dashboard antes de ocurrir. La UI puede mostrar datos confusos.

**Qué hacer:**
- Decidir la regla: rechazar fechas futuras en las tres altas (recomendado, consistente con `PayOccurrence`), o permitir operaciones programadas como concepto aparte.
- Si se rechazan: necesitan `Clock` (último parámetro del constructor, como el resto) y la validación dentro de la transacción. Cambia comportamiento existente, así que los tests actuales que registren fechas futuras deben actualizarse.
- Actualizar el conflicto C6 en la propuesta y la nota de `recent` (decisión 7 del Paso 4).

## 2. Activar y desactivar cuentas y categorías; renombrar categorías

**Situación:** `ModifyAccount` solo renombra (`ModifyAccountCommand(userId, accountId, name)`). No existe caso de uso para cambiar el estado de una cuenta o categoría, ni para renombrar categorías. Los estados `INACTIVE` existen en el dominio y en el esquema, pero solo se pueden poner con SQL.

**Por qué importa:**
- El dashboard emite `PAYMENT_BLOCKED` y la guía de la propuesta (§3) le sugiere al usuario reactivar la cuenta, pero no hay forma de hacerlo por la API.
- La UI no puede archivar una cuenta que ya no usa.

**Qué hacer:**
- Casos de uso nuevos (por ejemplo `DeactivateAccount`/`ReactivateAccount`, `DeactivateCategory`/`ReactivateCategory`, `RenameCategory`, o un `ModifyCategory` parcial como `ModifyObligation`). Revisar las reglas de dominio de `Account` y `Category` antes de diseñar (qué pasa con obligaciones activas que apuntan a una cuenta que se desactiva: hoy simplemente generan `PAYMENT_BLOCKED`).
- Endpoints sugeridos: `POST /api/v1/accounts/{id}/deactivate` y `/reactivate` (204), `PATCH /api/v1/categories/{id}` y acciones equivalentes; agregar `GET /api/v1/categories/{id}` si se quiere `Location` al crear categorías.
- Corregir la fila C2 de la propuesta cuando exista.

## 3. Idempotencia de los registros

**Situación:** `POST /operations/incomes`, `/expenses` y `/transfers` no son idempotentes: un reintento tras un timeout crea un duplicado. Pagar una ocurrencia sí tiene una guarda natural (`OCCURRENCE_ALREADY_RESOLVED`).

**Por qué importa:** en móvil los reintentos por red inestable son comunes.

**Qué hacer:**
- Header `Idempotency-Key` (UUID generado por el cliente por intento lógico). El servidor guarda la clave con el resultado y, si la ve de nuevo, devuelve la misma respuesta sin registrar otra operación.
- Requiere una tabla nueva (clave, usuario, endpoint, hash del body, respuesta, fecha) y su migración en `db/migrations/`.
- Mientras tanto, la UI debe desactivar el botón de envío hasta recibir respuesta.

## 4. Autenticación real

**Situación:** modo de usuario único. `ConfiguredSingleUserProvider` devuelve el UUID de `personal-finance.single-user-id`; cualquiera que alcance el puerto tiene acceso total. No hay forma de crear usuarios salvo con SQL.

**Qué hacer:**
- Elegir mecanismo (por ejemplo Spring Security con JWT/OIDC, o sesión).
- Reemplazar solo el bean `CurrentUserProvider` en `SecurityConfiguration`; borrar `SingleUserProperties`. Controladores, DTOs y Core no cambian.
- Caso de uso de registro/alta de usuario (`UserRepository.create` ya existe) y su endpoint.
- La regla "Current user" de `architecture.md` describe el reemplazo esperado.

## 5. Pantalla Review

**Situación:** planificada en `dashboard-review-read-models.md`, sin implementar: `ReviewQueryPort`, read models (`period`, `summary` de ingresos/gastos/neto, `spending.byCategory`, `notableOperations`, `upcomingCommitments`, `availableToSpend`) y `GET /api/v1/review`.

**Qué hacer:** diseño previo como el del Paso 4 (agente `Plan` → aprobación → flujo completo). Definir el periodo (mes calendario vs rango), qué es una "operación notable" y si las transferencias aparecen.

**Diseño:** el dashboard tiene un botón "Revisión" en el encabezado; un cliente debe omitirlo hasta que exista este endpoint.

## 6. Resolución `PAID` ligada a un gasto de otro usuario

**Situación:** la base acepta una fila en `occurrence_resolutions` que ligue la obligación de un usuario con el gasto de otro; ninguna restricción lo impide. Hoy no ocurre porque `PayOccurrence` carga todo por usuario, y las consultas del dashboard y de obligaciones limitan sus JOINs al usuario (una referencia cruzada se ignora o falla como dato corrupto).

**Qué hacer (opcional, defensa en profundidad):** un trigger en SQLite que valide que `expense_operations` → cuenta → `user_id` coincide con `obligations.user_id`, o redundar `user_id` en `occurrence_resolutions` con una FK compuesta. Requiere migración.

## 7. Contrato formal de la API (OpenAPI)

**Situación:** el contrato está descrito en `architecture.md` → Entry Layer → REST API, que no se versiona.

**Por qué importa:** el cliente móvil necesita un contrato estable; con OpenAPI se puede generar código del cliente y validar cambios.

**Qué hacer:** elegir entre springdoc-openapi (generado desde los controladores, agrega una dependencia) o un `openapi.yaml` escrito a mano y versionado. En ambos casos documentar los `code` de los 409, el formato problem+json, los montos en centavos y las fechas ISO.

## 8. Mejoras menores anotadas

- **Números como texto:** Jackson todavía acepta `"amountCents": "1050"` y lo convierte. Rechazarlo requiere un `JsonMapperBuilderCustomizer` con configuración de coerción.
- **Cancelar una transferencia responde 404** (el Core no las permite cancelar). Si se quiere distinguir de "no existe", un código como `OPERATION_NOT_CANCELLABLE` (409) es un cambio del Core.
- **Concurrencia SQLite:** `busy_timeout = 5000` cubre la espera de un escritor detrás de un lector; dos transacciones que leen y luego escriben a la vez pueden fallar con `SQLITE_BUSY` (500). Solución futura: `BEGIN IMMEDIATE` (`transaction_mode=IMMEDIATE`, verificar el comportamiento de sqlite-jdbc). WAL descartado por ahora (rompe el respaldo de un solo archivo).
- **Orden de los listados:** cuentas, categorías y obligaciones se ordenan con la intercalación BINARY de SQLite (distingue mayúsculas; acentos después de la Z). Los clientes ordenan por idioma para mostrar.
- **Validar `type` en `AttentionItemResponse`:** los records aceptan cualquier string en `type`; en la práctica `from()` siempre lo toma del Core.
- **Historial sin total:** `GET /operations` no devuelve total ni `hasNext`; el cliente detecta la última página cuando recibe menos de `pageSize`.

## 9. Huecos entre el diseño del dashboard y la API

**Situación:** algunas partes del diseño "Dashboard Finanzas" de Claude Design no tienen soporte en la API; un cliente tendría que omitirlas o simplificarlas. Review ya está en el punto 5.

### 9.1 Horizonte configurable

- **Diseño:** un chip "Próximos 14 días · 21 oct" abre una hoja para elegir 7, 14 o 30 días y recalcula Compromisos y Disponible.
- **Hoy:** `GetDashboard` usa `HORIZON_DAYS = CommitmentCalculator.DEFAULT_HORIZON_DAYS` (14) fijo y `GET /api/v1/dashboard` no recibe parámetros.
- **Qué hacer:** un parámetro opcional `horizonDays` (query) con valores permitidos acotados (por ejemplo 7, 14, 30, o un rango 1..60) que llegue a `GetDashboardQuery` y a `CommitmentCalculator`. Decidir si `attention` (`SHORTFALL`, `ACCOUNT_SHORTFALL`, `PAYMENT_BLOCKED`) usa el mismo horizonte o se queda en 14. Un valor fuera de rango responde 400.

### 9.2 Nota en ingresos y gastos

- **Diseño:** la hoja de registro tiene "Nota (opcional)" ("Comida con Laura"), y Reciente la usa como título de la fila.
- **Hoy:** `RegisterExpenseRequest`/`RegisterIncomeRequest` solo tienen `accountId`, `categoryId`, `amountCents` y `operationDate`; no hay columna en el esquema.
- **Qué hacer:** campo opcional `note` (texto con longitud máxima, por ejemplo 140, sin espacios sobrantes; vacío equivale a null) en el dominio de las operaciones, columna nueva con migración en `db/migrations/`, y exponerlo en `OperationResponse` y en `recent` del dashboard. Decidir si `PayOccurrence` acepta nota y si las transferencias la tienen.

### 9.3 Hora de las operaciones en Reciente

- **Diseño:** Reciente muestra "Hoy, 14:10 · Débito".
- **Hoy:** las operaciones solo guardan `operationDate` (fecha).
- **Qué hacer (baja prioridad):** guardar el instante de registro (`createdAt`, UTC) y exponerlo en `recent`; la hora se formatea en la zona del usuario. Requiere migración; las filas existentes quedan sin hora.

### 9.4 Transferencia sugerida en `ACCOUNT_SHORTFALL`

- **Diseño:** el aviso "Tu cuenta Nómina podría no cubrir sus compromisos…" trae un botón "Transferir $5,500 a Nómina" que hace la transferencia en un toque.
- **Hoy:** `ACCOUNT_SHORTFALL` trae la cuenta, el faltante y la fecha más cercana, pero no de qué cuenta conviene sacar el dinero.
- **Qué hacer:** opción A, que el dashboard sugiera una cuenta origen (por ejemplo la cuenta activa con más saldo libre después de sus propios compromisos) en el ítem; opción B, que el cliente pida la cuenta origen en una hoja de transferencia y use `POST /operations/transfers`, que ya existe. La B no requiere backend; la A es una regla de producto que hay que definir.

### 9.5 Monto por ocurrencia en `OVERDUE_OCCURRENCE`

- **Hoy:** el ítem trae `overdueCount` y `overdueAmountCents` (el total). Pagar salda una sola ocurrencia (`oldestOverdue`), así que el monto de esa ocurrencia solo se puede estimar como total ÷ cantidad.
- **Qué hacer:** agregar `amountCents` por ocurrencia al ítem (hoy todas las vencidas comparten el monto actual de la obligación, sección 3 de la propuesta), o devolver el monto pagado en la respuesta de `pay` (`{operationId, amountCents}`).

### 9.6 Fecha de "hoy" del servidor

- **Hoy:** si el cliente manda como `operationDate` la fecha del dispositivo, y el backend calcula el dashboard con `personal-finance.time-zone`. Cerca de medianoche, o con el teléfono en otra zona, pueden no coincidir.
- **Qué hacer:** incluir `today` en la respuesta del dashboard (el cliente ya recibe `horizon.from`, que hoy coincide con hoy, pero no es un contrato explícito), o permitir omitir `operationDate` en los registros para que el servidor use su fecha. Se relaciona con el punto 1 (fechas futuras).
