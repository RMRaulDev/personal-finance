# Consultas de Persistencia

Consultas SQL propuestas para los repositorios de Infrastructure.

Los parámetros se representan como `:nombre`. La implementación JDBC usará `?` posicionales.

Las cláusulas marcadas con `-- opcional` se incluyen dinámicamente en Java según los filtros presentes en el criterio de búsqueda.

---

## 1. Historial general por usuario

Obtiene todas las operaciones de un usuario combinando las tres tablas mediante `UNION ALL`.

`from` y `to` son opcionales.

```sql
SELECT i.id, 'INCOME' AS operation_type,
       i.account_id, i.category_id,
       NULL AS source_account_id, NULL AS target_account_id,
       i.amount, i.operation_date, i.status
FROM income_operations i
JOIN accounts a ON a.id = i.account_id
WHERE a.user_id = :userId
  AND i.operation_date >= :from  -- opcional
  AND i.operation_date <= :to    -- opcional

UNION ALL

SELECT e.id, 'EXPENSE',
       e.account_id, e.category_id,
       NULL, NULL,
       e.amount, e.operation_date, e.status
FROM expense_operations e
JOIN accounts a ON a.id = e.account_id
WHERE a.user_id = :userId
  AND e.operation_date >= :from  -- opcional
  AND e.operation_date <= :to    -- opcional

UNION ALL

SELECT t.id, 'TRANSFER',
       NULL, NULL,
       t.source_account_id, t.target_account_id,
       t.amount, t.operation_date, NULL
FROM transfer_operations t
JOIN accounts a ON a.id = t.source_account_id
WHERE a.user_id = :userId
  AND t.operation_date >= :from  -- opcional
  AND t.operation_date <= :to    -- opcional

ORDER BY operation_date DESC, id DESC
LIMIT :pageSize OFFSET :offset
```

El JOIN de `transfer_operations` usa `source_account_id` porque el dominio garantiza que ambas cuentas pertenecen al mismo usuario. No es necesario unir por `target_account_id` para determinar el usuario.

---

## 2. Historial filtrado por cuenta

No requiere JOIN con `accounts` porque el filtro es directo por `account_id`.

Para transferencias, la cuenta puede ser origen o destino. Se usan dos sub-consultas separadas en lugar de `OR` para que cada una aproveche su índice correspondiente. El `CHECK(source_account_id != target_account_id)` garantiza que no hay duplicados.

```sql
SELECT i.id, 'INCOME' AS operation_type,
       i.account_id, i.category_id,
       NULL AS source_account_id, NULL AS target_account_id,
       i.amount, i.operation_date, i.status
FROM income_operations i
WHERE i.account_id = :accountId
  AND i.operation_date >= :from  -- opcional
  AND i.operation_date <= :to    -- opcional

UNION ALL

SELECT e.id, 'EXPENSE',
       e.account_id, e.category_id,
       NULL, NULL,
       e.amount, e.operation_date, e.status
FROM expense_operations e
WHERE e.account_id = :accountId
  AND e.operation_date >= :from  -- opcional
  AND e.operation_date <= :to    -- opcional

UNION ALL

SELECT t.id, 'TRANSFER',
       NULL, NULL,
       t.source_account_id, t.target_account_id,
       t.amount, t.operation_date, NULL
FROM transfer_operations t
WHERE t.source_account_id = :accountId
  AND t.operation_date >= :from  -- opcional
  AND t.operation_date <= :to    -- opcional

UNION ALL

SELECT t.id, 'TRANSFER',
       NULL, NULL,
       t.source_account_id, t.target_account_id,
       t.amount, t.operation_date, NULL
FROM transfer_operations t
WHERE t.target_account_id = :accountId
  AND t.operation_date >= :from  -- opcional
  AND t.operation_date <= :to    -- opcional

ORDER BY operation_date DESC, id DESC
LIMIT :pageSize OFFSET :offset
```

---

## 3. Historial filtrado por categoría

`Transfer` no tiene categoría. Solo participan `income_operations` y `expense_operations`.

```sql
SELECT i.id, 'INCOME' AS operation_type,
       i.account_id, i.category_id,
       NULL AS source_account_id, NULL AS target_account_id,
       i.amount, i.operation_date, i.status
FROM income_operations i
WHERE i.category_id = :categoryId
  AND i.operation_date >= :from  -- opcional
  AND i.operation_date <= :to    -- opcional

UNION ALL

SELECT e.id, 'EXPENSE',
       e.account_id, e.category_id,
       NULL, NULL,
       e.amount, e.operation_date, e.status
FROM expense_operations e
WHERE e.category_id = :categoryId
  AND e.operation_date >= :from  -- opcional
  AND e.operation_date <= :to    -- opcional

ORDER BY operation_date DESC, id DESC
LIMIT :pageSize OFFSET :offset
```

---

## 4. Historial filtrado por tipo de operación

Si el criterio especifica `operationType`, solo se consulta la tabla correspondiente. No se ejecuta `UNION ALL`.

### INCOME

```sql
SELECT i.id, 'INCOME' AS operation_type,
       i.account_id, i.category_id,
       NULL AS source_account_id, NULL AS target_account_id,
       i.amount, i.operation_date, i.status
FROM income_operations i
JOIN accounts a ON a.id = i.account_id
WHERE a.user_id = :userId
  AND i.operation_date >= :from  -- opcional
  AND i.operation_date <= :to    -- opcional
ORDER BY i.operation_date DESC, i.id DESC
LIMIT :pageSize OFFSET :offset
```

### EXPENSE

```sql
SELECT e.id, 'EXPENSE' AS operation_type,
       e.account_id, e.category_id,
       NULL AS source_account_id, NULL AS target_account_id,
       e.amount, e.operation_date, e.status
FROM expense_operations e
JOIN accounts a ON a.id = e.account_id
WHERE a.user_id = :userId
  AND e.operation_date >= :from  -- opcional
  AND e.operation_date <= :to    -- opcional
ORDER BY e.operation_date DESC, e.id DESC
LIMIT :pageSize OFFSET :offset
```

### TRANSFER

```sql
SELECT t.id, 'TRANSFER' AS operation_type,
       NULL AS account_id, NULL AS category_id,
       t.source_account_id, t.target_account_id,
       t.amount, t.operation_date, NULL AS status
FROM transfer_operations t
JOIN accounts a ON a.id = t.source_account_id
WHERE a.user_id = :userId
  AND t.operation_date >= :from  -- opcional
  AND t.operation_date <= :to    -- opcional
ORDER BY t.operation_date DESC, t.id DESC
LIMIT :pageSize OFFSET :offset
```

Los filtros `accountId` y `categoryId` pueden combinarse con `operationType`. En ese caso, el `WHERE` adicional se agrega a la consulta de la tabla correspondiente.

---

## 5. Detalle de una operación

Recibe únicamente un `operationId`. Como el ID puede pertenecer a cualquiera de las tres tablas, se consultan todas mediante `UNION ALL`.

```sql
SELECT i.id, 'INCOME' AS operation_type,
       i.account_id, i.category_id,
       NULL AS source_account_id, NULL AS target_account_id,
       i.amount, i.operation_date, i.status
FROM income_operations i
WHERE i.id = :operationId

UNION ALL

SELECT e.id, 'EXPENSE',
       e.account_id, e.category_id,
       NULL, NULL,
       e.amount, e.operation_date, e.status
FROM expense_operations e
WHERE e.id = :operationId

UNION ALL

SELECT t.id, 'TRANSFER',
       NULL, NULL,
       t.source_account_id, t.target_account_id,
       t.amount, t.operation_date, NULL
FROM transfer_operations t
WHERE t.id = :operationId
```

Cada `WHERE id = ?` usa el `PRIMARY KEY`, por lo que las tres búsquedas son O(log n).

Sobre la unicidad de `operationId`:

- Cada tabla garantiza la unicidad de su propio `id` mediante `PRIMARY KEY`.
- No existe una restricción global de unicidad entre las tres tablas.
- La aplicación genera UUIDs mediante `UUID.randomUUID()`, por lo que una colisión entre tablas es extraordinariamente improbable, pero no es una garantía proporcionada por SQLite.
- El modelo actual no utiliza una tabla central `financial_operations`.

---

## 6. Consulta de Reversal con operación original

Obtiene un reversal y la información de su operación original. Utiliza `LEFT JOIN` contra ambas tablas de operaciones porque `original_operation_id` puede referenciar `income_operations` o `expense_operations`.

SQLite no puede garantizar mediante una Foreign Key convencional que `original_operation_id` exista en exactamente una de esas dos tablas. Esa integridad es responsabilidad de Application.

```sql
SELECT r.id,
       r.original_operation_id,
       r.cancelled_at,
       COALESCE(i.amount, e.amount) AS amount,
       COALESCE(i.operation_date, e.operation_date) AS operation_date,
       CASE
           WHEN i.id IS NOT NULL THEN 'INCOME'
           WHEN e.id IS NOT NULL THEN 'EXPENSE'
       END AS operation_type
FROM reversals r
LEFT JOIN income_operations i ON r.original_operation_id = i.id
LEFT JOIN expense_operations e ON r.original_operation_id = e.id
WHERE r.id = :reversalId
```

Para buscar si una operación ya tiene reversal:

```sql
SELECT r.id, r.cancelled_at
FROM reversals r
WHERE r.original_operation_id = :operationId
```

El `UNIQUE(original_operation_id)` garantiza que esta consulta retorna a lo sumo una fila y el índice implícito la resuelve por index seek.

---

## Análisis de índices

### Índices eliminados

| Índice | Razón de eliminación |
|--------|---------------------|
| `idx_accounts_user_id` | Redundante. `UNIQUE(user_id, name)` en `accounts` crea un índice implícito con `user_id` como prefijo, suficiente para los JOINs por `user_id`. |
| `idx_categories_user_id` | Redundante. `UNIQUE(user_id, name)` en `categories` crea un índice implícito con `user_id` como prefijo. |

### Índices confirmados

| Índice | Tabla | Columnas | Consultas que optimiza | Motivo |
|--------|-------|----------|----------------------|--------|
| `idx_income_operations_account_date` | `income_operations` | `account_id, operation_date DESC, id DESC` | 1, 2, 4 (INCOME) | Filtro por cuenta + rango de fechas. El prefijo `account_id` resuelve el filtro directo (consulta 2) y el JOIN desde `accounts` (consultas 1, 4). Las columnas `operation_date DESC, id DESC` permiten recorrer el rango de fechas en el orden requerido sin sort adicional dentro de cada sub-consulta. |
| `idx_income_operations_category` | `income_operations` | `category_id` | 3 | Filtro por categoría. Reduce el conjunto antes de aplicar el rango de fechas. |
| `idx_expense_operations_account_date` | `expense_operations` | `account_id, operation_date DESC, id DESC` | 1, 2, 4 (EXPENSE) | Mismo razonamiento que el índice equivalente en `income_operations`. |
| `idx_expense_operations_category` | `expense_operations` | `category_id` | 3 | Mismo razonamiento que el índice equivalente en `income_operations`. |
| `idx_transfer_operations_source_date` | `transfer_operations` | `source_account_id, operation_date DESC, id DESC` | 1, 2 (source), 4 (TRANSFER) | Filtro por cuenta origen + rango de fechas. También soporta el JOIN por `source_account_id` para determinar el usuario en consultas 1 y 4. |
| `idx_transfer_operations_target_date` | `transfer_operations` | `target_account_id, operation_date DESC, id DESC` | 2 (target) | Filtro por cuenta destino en el historial por cuenta (consulta 2, segunda sub-consulta de transferencias). |

### Índices implícitos (creados por constraints)

| Constraint | Tabla | Columnas del índice implícito | Uso |
|-----------|-------|------------------------------|-----|
| `PRIMARY KEY` | Todas | `id` | Consulta 5 (detalle por ID), JOINs por ID, consulta 6 (LEFT JOIN por `original_operation_id = id`). |
| `UNIQUE(user_id, name)` | `accounts` | `user_id, name` | JOINs por `user_id` en consultas 1 y 4. Reemplaza `idx_accounts_user_id`. |
| `UNIQUE(user_id, name)` | `categories` | `user_id, name` | Búsqueda de categorías por usuario. Reemplaza `idx_categories_user_id`. |
| `UNIQUE(original_operation_id)` | `reversals` | `original_operation_id` | Consulta 6 (búsqueda de reversal por operación original). |
