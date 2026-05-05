# Account Service
orquestar el flujo de transacciones (deposito, retiro, consumo)

## Estructura del proyecto
- 'client/': apis externas
- 'config/': beans, configuraciones, properties, etc
- 'controller/': api rest controller con rxjava, dto
- 'domain/': entidades de negocio
- 'exception/': manejo de excepciones
- 'kafka/': capa de integración con kafka
  - 'event/': eventos
- 'mapper/': mapeo de objetos
- 'repository/': repositorios de base de datos (mongodb, redis)
- 'service/': servicios de negocio
- 'support/': utilidades

## Responsabilidades:
- orquestar el flujo de transacciones (deposito, retiro, consumo)

## Entidades de negocio

## Reglas de Negocio:

## Api Rest: [TransactionOrchestratorController]
- Orquesta flujo de depósito:
  - API: [POST] /v1/transaction-orchestrator/accounts/{accountId}/deposit
  - PathVariable:
    - accountId: id de la cuenta
  - Request Headers:
    - Idempotency-Key
  - Request Body: [AccountTransactionResponseDto]
  - Flujo:
    - Busca operacion por "Idempotency-Key" y OperationType.DEPOSIT [mongodb: idempotency_log]
    - Si existe, retorna el resultado
    - Si no existe:
    - Registra en [mongodb: idempotency_log] con estado "PENDING"
    - Publica evento [DepositRequestedEvent] a [kafka: bank.transaction.deposit.requested]
  - Response Body: [AccountTransactionResponseDto]
  - Response Status:
    - 201 Created
  - Response Status Error:
    - 400 Bad Request
- Orquesta flujo de retiro:
  - API: [POST] /v1/transaction-orchestrator/accounts/{accountId}/withdraw
  - PathVariable:
    - accountId: id de la cuenta
  - Request Headers:
    - Idempotency-Key
  - Request Body: [AccountTransactionResponseDto]
  - Flujo:
    - Busca operacion por "Idempotency-Key" y OperationType.WITHDRAW [mongodb: idempotency_log]
    - Si existe, retorna el resultado
    - Si no existe:
    - Registra en [mongodb: idempotency_log] con estado "PENDING"
    - Publica evento [WithdrawRequestedEvent] a [kafka: bank.transaction.withdraw.requested]
  - Response Body: [AccountTransactionResponseDto]
  - Response Status:
    - 201 Created
  - Response Status Error:
    - 400 Bad Request

## Escucha Eventos:
- [DepositAcceptedConsumer: listen(), evento: DepositAcceptedEvent, topic: bank.deposit.accepted]
  - Buscar si existe registro con "idempotencyKey" y OperationType.DEPOSIT en [mongodb: idempotency_log]
  - Si no existe, da error y log.
  - Si existe y estado es "COMPLETED" o "FAILED, culmina con ack.acknowledge()
  - Si existe y estado es "PENDING:
    - Deserealizamos el evento [DepositAcceptedEvent] de la payload
    - Generar un nuevo responseBody con los valores del evento
    - Actualizar el idempotency log en [mongodb: idempotency_log] con estado "COMPLETED"
    - culminar con ack.acknowledge()
- [DepositRejectedConsumer: listen(), evento: DepositRejectedEvent, topic: bank.deposit.rejected]
  - Buscar si existe registro con "idempotencyKey" y OperationType.DEPOSIT en [mongodb: idempotency_log]
  - Si no existe, da error y log.
  - Si existe y estado es "COMPLETED" o "FAILED, culmina con ack.acknowledge()
  - Si existe y estado es "PENDING:
    - Deserealizamos el evento [DepositRejectedEvent] de la payload
    - Generar un nuevo responseBody con los valores del evento
    - Actualizar el idempotency log en [mongodb: idempotency_log] con estado "FAILED"
    - culminar con ack.acknowledge()
- [WithdrawAcceptedConsumer: listen(), evento: WithdrawAcceptedEvent, topic: bank.withdraw.accepted]
  - Buscar si existe registro con "idempotencyKey" y OperationType.WITHDRAW en [mongodb: idempotency_log]
  - Si no existe, da error y log.
  - Si existe y estado es "COMPLETED" o "FAILED, culmina con ack.acknowledge()
  - Si existe y estado es "PENDING:
    - Deserealizamos el evento [WithdrawAcceptedEvent] de la payload
    - Generar un nuevo responseBody con los valores del evento
    - Actualizar el idempotency log en [mongodb: idempotency_log] con estado "COMPLETED"
    - culminar con ack.acknowledge()
- [WithdrawRejectedConsumer: listen(), evento: WithdrawRejectedEvent, topic: bank.withdraw.rejected]
  - Buscar si existe registro con "idempotencyKey" y OperationType.WITHDRAW en [mongodb: idempotency_log]
  - Si no existe, da error y log.
  - Si existe y estado es "COMPLETED" o "FAILED, culmina con ack.acknowledge()
  - Si existe y estado es "PENDING:
    - Deserealizamos el evento [WithdrawRejectedEvent] de la payload
    - Generar un nuevo responseBody con los valores del evento
    - Actualizar el idempotency log en [mongodb: idempotency_log] con estado "FAILED"
    - culminar con ack.acknowledge()
