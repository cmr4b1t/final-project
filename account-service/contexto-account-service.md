# Account Service
gestionar cuentas bancarias

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
- Mantiene una base de datos de cuentas bancarias (tablas: accounts)
- Se encarga de la creación y consulta de cuentas bancarias
- Aplica las reglas de negocio para cuentas bancarias
- Gatilla la creación de tarjeta de débito al crear una cuenta bancaria (se envía evento a kafka)
- Gestiona el saldo disponible (balance) de la cuenta bancaria

## Entidades de negocio
- Tipos de Cuentas Bancarias:
  - Cuenta de Ahorro (Savings)
  ~~- Cuenta de Corriente (Checking)
  - Cuenta de Plazo Fijo (Fixed Term)

## Reglas de Negocio:

- Cliente Personal sólo puede tener como máximo estas cuentas bancarias:
    - 1 Cuenta de Ahorro (Savings)
    - 1 Cuenta de Corriente (Checking)
    - Muchas Cuentas de Plazo Fijo (Fixed Term)

- Cliente Empresarial sólo puede tener como máximo estas cuentas bancarias:~~
    - Muchas Cuentas de Corriente (Checking)

- Cliente Empresarial puede tener:
    - 1 o más titulares
    - 0 o más firmantes autorizados

- Si el cliente tiene deuda vencida en algún producto de credito, no podra adquirir ningun producto nuevo.

- Se permite crear cuentas bancaria con mínimo monto de apertura de Cero
- Al crear una cuenta bancaria, se le creará automáticamente una tarjeta de débito asociada
- Todas las cuentas bancarias tendrán un número límite máximo de transacciones (deposito/retiro) donde no se cobrará comisión, luego de ello si se cobrará comisión por cada transacción adicional.

- Cliente Personal o Empresarial con perfil Standard:
  - Reglas para Cuenta de Ahorro:
    - La cuenta no tiene comisión de mantenimiento
    - La cuenta tiene un límite máximo de movimientos mensuales
  - Reglas para Cuenta Corriente:
    - La cuenta posee comisión de mantenimiento
    - La cuenta no tiene límite de movimientos mensuales
  - Reglas para Cuenta de Plazo Fijo:
    - La cuenta no tiene comisión de mantenimiento
    - Sólo permite un movimiento de retiro/depósito en un día específico del mes
    - 
- Cliente Personal con perfil VIP:
  - Reglas para Cuenta de Ahorro:
    - El cliente debe tener al menos 1 tarjeta de crédito al momento de crear la cuenta
    - El saldo de la cuenta no debe ser menor a la minima permita según configuración (es decir, no se permitirá retirar dinero si eso hiciera que el saldo sea menor al saldo minimo permitido)

- Cliente Empresarial con perfil PYME:
  - Reglas para Cuenta Corriente:
    - El cliente debe tener al menos 1 tarjeta de crédito al momento de crear la cuenta
    - La cuenta no tiene comisión de mantenimiento

## Api Rest: [AccountController]
- Crear cuenta bancaria:
  - API: [POST] /v1/accounts
  - Request Headers:
    - Idempotency-Key
  - Request Body: [CreateAccountRequestDto]
    - customerId, accountType, accountSubType, etc
  - Flujo:
    - Busca operacion por "Idempotency-Key" y OperationType.CREATE_ACCOUNT [mongodb: idempotency_log]
    - Si existe, retorna el resultado
    - Si no existe:
    - Buscar el cliente (con customerId) [customer-service: [GET] /v1/customers/{customerId}]
    - Validar que el cliente no tenga deuda vencida (usar el campo "hasOverdueDebts" del response de customer-service)
    - Validar que el cliente cumpla con las reglas de negocio para el tipo de cuenta
    - Registrar nueva cuenta bancaria [mongodb: accounts]
    - Registra el resultado en [mongodb: idempotency_log]
    - Gatilla el evento [AccountCreatedEvent] de creación de tarjeta de débito [kafka: bank.account.created]
      - el "Idempotency-Key" y el operationType se enviarán en los headers hacia kafka
    - En la respuesta final, el "Idempotency-Key" se enviará en los headers
  - Response Body: [CreateAccountResponseDto]
    - status
    - createdAt
    - accountId
    - customerId
    - accountType
    - accountSubType, etc
  - Response Status:
    - 201 Created
  - Response Status Error:
    - 400 Bad Request
    - 404 Not Found
    - 409 Conflict