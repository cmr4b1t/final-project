# Diagrama de secuencia - Depósito y retiro de cuenta bancaria

Los depósitos y retiros se inician en `transaction-orchestrator`, se ejecutan en `account-service`, se registran en `transaction-service` y se cierran en el orquestador por Kafka.

## Depósito

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant Gateway
    participant Orchestrator as transaction-orchestrator
    participant Account as account-service
    participant Transaction as transaction-service
    participant Kafka
    participant MongoDB

    Usuario->>Gateway: POST /deposit + Idempotency-Key
    Gateway->>Orchestrator: Solicitar deposito
    Orchestrator->>MongoDB: Buscar operacion DEPOSIT

    alt Operacion repetida
        MongoDB-->>Orchestrator: Estado guardado
        Orchestrator-->>Gateway: 201 Created + estado guardado
        Gateway-->>Usuario: Resultado existente
    else Nueva operacion
        Orchestrator->>MongoDB: Guardar DEPOSIT PENDING
        Orchestrator->>Kafka: Publicar bank.transaction.deposit.requested
        Orchestrator-->>Gateway: 201 Created + PENDING
        Gateway-->>Usuario: Deposito en proceso

        Kafka-->>Account: bank.transaction.deposit.requested
        Account->>MongoDB: Validar cuenta, saldo/reglas e idempotencia
        Account->>Transaction: Consultar movimientos del mes
        Transaction->>MongoDB: Buscar movimientos
        MongoDB-->>Transaction: Movimientos
        Transaction-->>Account: Conteo/lista
        Account-->>Account: Reglas de negocio

        alt Deposito valido
            Account->>MongoDB: Actualizar saldo y log COMPLETED
            Account->>Transaction: Registrar transaccion DEPOSIT
            Transaction->>MongoDB: Guardar movimiento
            Account->>Kafka: Publicar bank.transaction.deposit.accepted
            Kafka-->>Orchestrator: bank.transaction.deposit.accepted
            Orchestrator->>MongoDB: Marcar DEPOSIT COMPLETED
        else Deposito rechazado
            Account->>MongoDB: Guardar log FAILED
            Account->>Kafka: Publicar bank.transaction.deposit.rejected
            Kafka-->>Orchestrator: bank.transaction.deposit.rejected
            Orchestrator->>MongoDB: Marcar DEPOSIT FAILED
        end
    end
```

## Retiro

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant Gateway
    participant Orchestrator as transaction-orchestrator
    participant Account as account-service
    participant Transaction as transaction-service
    participant Kafka
    participant MongoDB

    Usuario->>Gateway: POST /withdraw + Idempotency-Key
    Gateway->>Orchestrator: Solicitar retiro
    Orchestrator->>MongoDB: Buscar operacion WITHDRAW
    Orchestrator->>MongoDB: Guardar WITHDRAW PENDING
    Orchestrator->>Kafka: Publicar bank.transaction.withdraw.requested
    Orchestrator-->>Gateway: 201 Created + PENDING
    Gateway-->>Usuario: Retiro en proceso

    Kafka-->>Account: bank.transaction.withdraw.requested
    Account->>MongoDB: Validar cuenta, saldo, saldo minimo, reglas e idempotencia
    Account->>Transaction: Consultar movimientos del mes
    Transaction->>MongoDB: Buscar movimientos
    Transaction-->>Account: Conteo/lista
    Account-->>Account: Reglas de negocio

    alt Retiro valido
        Account->>MongoDB: Debitar saldo y guardar log COMPLETED
        Account->>Transaction: Registrar transaccion WITHDRAW
        Transaction->>MongoDB: Guardar movimiento
        Account->>Kafka: Publicar bank.transaction.withdraw.accepted
        Kafka-->>Orchestrator: bank.transaction.withdraw.accepted
        Orchestrator->>MongoDB: Marcar WITHDRAW COMPLETED
    else Retiro rechazado
        Account->>MongoDB: Guardar log FAILED
        Account->>Kafka: Publicar bank.transaction.withdraw.rejected
        Kafka-->>Orchestrator: bank.transaction.withdraw.rejected
        Orchestrator->>MongoDB: Marcar WITHDRAW FAILED
    end
```

## Motivos de rechazo

- Cuenta inexistente o inactiva.
- Monto menor o igual a cero.
- Moneda distinta a la cuenta.
- Cuenta de plazo fijo fuera del día permitido.
- Límite mensual de movimientos excedido.
- Retiro sin saldo suficiente, comisión incluida.
- Retiro que deja saldo por debajo del mínimo permitido.
- Error o timeout al consultar/registrar movimientos en `transaction-service`.
