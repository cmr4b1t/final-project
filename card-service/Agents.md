# Account Service
gestionar tarjetas de débito y crédito

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
- Mantiene una base de datos de tarjetas de debito y crédito (tablas: cards)
- Se encarga de la creación y consulta de tarjetas de debito y crédito

## Entidades de negocio
- Tipos de Tarjetas:
  - Tarjetas de débito (Debit)
  - Tarjetas de crédito (Credit)

## Reglas de Negocio:


## Api Rest: [CreditController]

## Escucha Eventos:
- [AccountCreatedConsumer: listen]
  - Buscar si existe registro con "idempotencyKey" y OperationType.CREATE_DEBIT_CARD en [mongodb: idempotency_log]
  - Si existe, culminar con ack.acknowledge()
  - Si no existe:
    - Deserealizamos el evento [AccountCreatedEvent] de la payload
    - Creamos la tarjeta de débito
    - Guardamos la tarjeta de débito en [mongodb: cards]
    - Registramos el idempotency log en [mongodb: idempotency_log] con estado "COMPLETED"
    - Gatillamos el evento [DebitCardCreatedEvent] a [kafka: bank.debit.card.created]
      - el "Idempotency-Key" se enviará en los headers hacia kafka
    - culminar con ack.acknowledge()
