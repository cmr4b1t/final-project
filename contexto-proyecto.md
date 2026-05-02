# Proyecto General Banco

## Objetivo
Gestionar clientes financieros y sus productos bancarios.

## Especificaciones Técnicas
- Versión de java: 17
- Versión de spring boot: 3.x
- Versión de spring cloud: 2025.x
- Versión de RxJava: 3.x
- Usar Maven como gestor de dependencias
- Usar RxJava para manejar la programación reactiva
- Usar reactor-adapter para convertir entre RxJava y Reactor
- Usar Lombok para reducir el código boilerplate
- Usar MapStruct para el mapeo de objetos
- Usar MongoDB como base de datos
- Usar MongoDB reactive para manejar la base de datos
- Usar Redis reactive para manejar la caché (opcional)
- Usar WebClient para manejar las llamadas externas
- Usar arquitectura orientada a eventos (en caso requiera)
- Usar Kafka para el manejo de eventos
- Usar Spring Kafka para los consumidores y productores
- Usar resilience4j para manejar las llamadas externas (usar timeout 2 seg.)
- Usar patrones de diseño de software (strategy, factory, etc)
- Usar patrones SOLID y buenas practicas de desarrollo de software
- Usar Docker para containerización
- Seguir el enfoque database per service
- Los microservicios deben obtener su configuración (técnica y funcional) de un config-server
- Los microservicios deben usar programación funcional
- Se usará un eureka-server como service registry
- Se usará un spring gateway como api gateway

## Entidades de negocio
- Tipos de Clientes:
  - Personal (Personal)
    - Perfiles:
      - Standard
      - VIP
  - Empresarial (Business)
    - Perfiles:
      - Standard
      - PYME

- Tipos de Cuentas Bancarias:
  - Cuenta de Ahorro (Savings)
  - Cuenta de Corriente (Checking)
  - Cuenta de Plazo Fijo (Fixed Term)

- Tipos de Cuentas de Crédito:
  - Préstamo (Loan)
  - Tarjeta de Crédito (Credit Card)

# Reglas de Negocio:

- Cliente Personal sólo puede tener como máximo estas cuentas bancarias:
  - 1 Cuenta de Ahorro (Savings)
  - 1 Cuenta de Corriente (Checking)
  - Muchas Cuentas de Plazo Fijo (Fixed Term)

- Cliente Empresarial sólo puede tener como máximo estas cuentas bancarias:
  - Muchas Cuentas de Corriente (Checking)

- Cliente Empresarial puede tener:
  - 1 o más titulares
  - 0 o más firmantes autorizados

- Un cliente puede solicitar crédito sin necesitad de tener cuenta bancaria (es decir, el desembolso se haría en efectivo)
- Se permite crear cuentas con mínimo monto de apertura de Cero
- Al crear una cuenta bancaria, se le creará automáticamente una tarjeta de décbito asociada
- Si el cliente tiene deuda vencida en algún producto de credito, no podra adquirir ningun producto nuevo.
- Todas las cuentas bancarias tendrán un número máximo de transacciones (deposito/retiro) donde no se cobrará comisión, luego de ello si se cobrará comisión por cada transacción adicional.

- Cliente VIP:
  - Reglas para Cuenta de Ahorro:
    - El cliente debe tener al menos 1 tarjeta de crédito al momento de crear la cuenta
    - La cuenta debe mantener un monto minimo en todo momento
- Cliente PYME:
  - Reglas para Cuenta Corriente:
    - El cliente debe tener al menos 1 tarjeta de crédito al momento de crear la cuenta
    - La cuenta no tiene comisión de mantenimiento

- Un Cliente puede hacer pago de crédito de terceros.

## Funcionalidades
- Registrar Cliente nuevo con documento de identidad
- Crear Cuenta Bancaria
- Crear Tarjeta de Débito
- Solicitar Crédito
- Realizar transacción de deposito
- Realizar transacción de retiro
- Realizar transferencia entre cuentas del mismo cliente
- Realizar transferencia entre cuentas de diferentes clientes
- Realizar transacción de consumo con tarjeta de crédito
- Realizar pago de deuda de préstamo
- Realizar pago de deuda de tarjeta de crédito
- Consultar saldo disponible de cuenta bancaria
- Consultar saldo disponible de tarjeta de crédito
- Consultar movimientos de cuenta bancaria
- Consultar movimientos de tarjeta de crédito
- Consultar deuda de préstamos
- Consultar deuda de tarjetas de crédito
- Reporte por producto bancario en intervalo de tiempo indicado por el usuario
- Reporte con lo ultimos 10 movimientos de tarjeta de débito
- Reporte con lo ultimos 10 movimientos de tarjeta de crédito

## microservicios
- customer-service: gestionar clientes
- account-service: gestionar cuentas bancarias
- transaction-service: realizar transacciones (deposito, retiro, consumo)
- loan-service: gestionar cuentas de préstamos
- credit-card-account-service: gestionar cuentas de tarjetas de crédito
- card-service: gestionar tarjetas de débito y crédito
- payment-service: realizar pagos de deuda de préstamos y tarjetas de crédito
- statement-service: gestionar estados de cuenta de tarjetas de crédito
- overdue-debt-service: gestionar deuda vencida
- transfer-service: realizar transferencias (entre cuentas del mismo cliente, entre cuentas de diferentes clientes)
- reporting-service: generar reportes
- config-server
- eureka-server
- api-gateway
