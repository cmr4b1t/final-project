# Diagramas de secuencia

Documentación de flujos principales del sistema bancario. Los diagramas usan Mermaid y muestran la interacción entre `Usuario`, `Gateway`, microservicios, Kafka, MongoDB y flujos alternativos.

## Archivos

- [Registro de cliente](./secuencia-registro-cliente.md)
- [Creación de cuenta y tarjeta de débito](./secuencia-creacion-cuenta-tarjeta-debito.md)
- [Depósito y retiro](./secuencia-deposito-retiro.md)
- [Consultas de saldo y movimientos](./secuencia-consultas-cuenta-movimientos.md)
- [Tarjetas de débito y crédito](./secuencia-tarjetas.md)

## Topics Kafka usados

| Topic | Productor | Consumidor | Propósito |
| --- | --- | --- | --- |
| `bank.account.created` | `account-service` | `card-service` | Solicitar creación automática de tarjeta de débito. |
| `bank.debit.card.created` | `card-service` | `account-service` | Confirmar tarjeta de débito creada y activar cuenta. |
| `bank.account.activated` | `account-service` | `customer-service` | Actualizar contadores de cuentas del cliente. |
| `bank.transaction.deposit.requested` | `transaction-orchestrator` | `account-service` | Solicitar validación y ejecución de depósito. |
| `bank.transaction.deposit.accepted` | `account-service` | `transaction-orchestrator` | Completar depósito exitoso. |
| `bank.transaction.deposit.rejected` | `account-service` | `transaction-orchestrator` | Marcar depósito fallido con descripción. |
| `bank.transaction.withdraw.requested` | `transaction-orchestrator` | `account-service` | Solicitar validación y ejecución de retiro. |
| `bank.transaction.withdraw.accepted` | `account-service` | `transaction-orchestrator` | Completar retiro exitoso. |
| `bank.transaction.withdraw.rejected` | `account-service` | `transaction-orchestrator` | Marcar retiro fallido con descripción. |

## Bases de datos por servicio

| Servicio | Base MongoDB | Uso principal |
| --- | --- | --- |
| `customer-service` | `customer-db` | Clientes, contadores de cuentas e idempotencia de activación. |
| `account-service` | `account-db` | Cuentas, saldo, reglas de movimientos e idempotencia. |
| `card-service` | `card-db` | Tarjetas de débito/crédito e idempotencia de creación. |
| `transaction-service` | `account-db` según configuración actual | Movimientos financieros registrados. Por database-per-service debería revisarse si corresponde separar a `transaction-db`. |
| `transaction-orchestrator` | `transaction-orchestrator-db` | Estado final de operaciones de depósito/retiro por idempotencia. |

## Notas de lectura

- Los endpoints síncronos entran por `Gateway` y llegan al microservicio dueño del recurso.
- Los flujos de creación de cuenta y transacciones usan respuesta inicial `PENDING` cuando la operación continúa por Kafka.
- Los microservicios guardan logs de idempotencia para evitar reprocesar solicitudes o eventos repetidos.
- Las llamadas HTTP internas se muestran solo a nivel de microservicio para evitar detalles internos de clases.
- `loan-service`, `payment-service`, `statement-service`, `overdue-debt-service` y `credit-card-account-service` no muestran flujos de negocio completos en el código actual, por eso no se generaron diagramas detallados para esos casos.
