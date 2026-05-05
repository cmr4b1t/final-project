# Transaction Service
registrar transacciones (deposito, retiro, consumo, pagos)

## Estructura del proyecto
- 'client/': apis externas
- 'config/': beans, configuraciones, properties, etc
- 'controller/': api rest controller con rxjava, dto
- 'domain/': entidades de negocio
- 'exception/': manejo de excepciones
- 'kafka': capa de integración con kafka
- 'mapper/': mapeo de objetos
- 'repository/': repositorios de base de datos (mongodb, redis)
- 'service/': servicios de negocio
- 'support/': utilidades

## Responsabilidades:
- Mantiene una base de datos de las transacciones (tablas: transactions)
- Se encarga de registrar y consultar transacciones (deposito, retiro, consumo, desembolso, pago de préstamos, pago de tarjetas de crédito)
- Consultar movimientos de cuenta bancaria, préstamos y tarjetas de crédito

## Api Rest: [TransactionController]
- Registrar transacción:
    - API: [POST] /v1/transactions
    - Request Headers:
        - Idempotency-Key
    - Request Body: [TransactionRequestDto]
    - Flujo:
        - Busca operacion por "Idempotency-Key" (transactionId) y transactionType en [mongodb: transactions]
        - Si existe, retorna resultado 409 Conflict
        - Si no existe:
        - Registrar la transacción en [mongodb: transactions]
    - Response Body: [TransactionResponseDto]
    - Response Status:
        - 201 Created
    - Response Status Error:
        - 400 Bad Request
        - 409 Conflict
- Consultar movimientos por accountId:
    - API: [GET] /v1/transactions/{accountId}
    - PathVariable:
      - accountId: id del la cuenta origen
    - Flujo:
        - Buscar (con accountId) las transacciones en [mongodb: transactions]
    - Response Body: [TransactionResponseDto]
    - Response Status:
        - 200 OK
    - Response Status Error:
        - 400 Bad Request
