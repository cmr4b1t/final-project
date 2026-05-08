# Diagrama de secuencia - Creación de cuenta bancaria y tarjeta de débito

La cuenta se crea inicialmente como `PENDING/INACTIVE`. Luego `card-service` crea la tarjeta de débito por Kafka, `account-service` activa la cuenta y `customer-service` actualiza sus contadores.

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant Gateway
    participant Account as account-service
    participant Customer as customer-service
    participant Card as card-service
    participant Redis
    participant Kafka
    participant MongoDB

    Usuario->>Gateway: POST /v1/accounts + Idempotency-Key
    Gateway->>Account: Crear cuenta bancaria
    Account->>MongoDB: Buscar operacion idempotente

    alt Idempotency-Key repetido
        MongoDB-->>Account: Respuesta guardada
        Account-->>Gateway: 201 Created + respuesta previa
        Gateway-->>Usuario: Resultado existente
    else Nueva solicitud
        Account->>Customer: Validar cliente
        Customer->>Redis: Buscar cliente
        Redis-->>Customer: Cliente
        Customer-->>Account: Datos del cliente
        Account-->>Account: Reglas de negocio
        Account->>MongoDB: Guardar cuenta INACTIVE y log PENDING
        Account->>Kafka: Publicar bank.account.created
        Account-->>Gateway: 201 Created + cuenta PENDING
        Gateway-->>Usuario: Cuenta en proceso

        Kafka-->>Card: bank.account.created
        Card->>MongoDB: Validar idempotencia y guardar tarjeta DEBIT ACTIVE
        Card->>Kafka: Publicar bank.debit.card.created

        Kafka-->>Account: bank.debit.card.created
        Account->>MongoDB: Activar cuenta y completar log
        Account->>Kafka: Publicar bank.account.activated

        Kafka-->>Customer: bank.account.activated
        Customer->>MongoDB: Actualizar contador de cuentas del cliente
        Customer->>Redis: Actualizar contador de cuentas del cliente
    end
```

## Flujos alternativos

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant Gateway
    participant Account as account-service
    participant Customer as customer-service
    participant Card as card-service
    participant Kafka
    participant MongoDB

    Usuario->>Gateway: POST /v1/accounts
    Gateway->>Account: Crear cuenta

    alt Cliente no existe
        Account->>Customer: Validar cliente
        Customer->>MongoDB: Buscar cliente
        MongoDB-->>Customer: Sin resultado
        Customer-->>Account: 404 Not Found
        Account-->>Gateway: 404 Not Found
        Gateway-->>Usuario: ApiErrorResponse
    else Regla de negocio incumplida
        Account->>Customer: Validar cliente
        Customer-->>Account: Datos del cliente
        Account-->>Account: Reglas de negocio
        Account-->>Gateway: 409 Conflict
        Gateway-->>Usuario: ApiErrorResponse
    else Evento duplicado
        Kafka-->>Card: bank.account.created repetido
        Card->>MongoDB: Buscar log de idempotencia
        MongoDB-->>Card: Log existente
        Card-->>Kafka: Ack sin crear otra tarjeta
    end
```
