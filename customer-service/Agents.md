# Customer Service
Gestionar clientes

## Estructura del proyecto
- 'application/': capa de aplicación
  - 'port/': in (casos de uso), out (puertos externos)
  - 'service/': implementación de casos de uso
- 'config/': beans, configuraciones, properties, etc
- 'controller/': api rest controller con rxjava, dto, mapper, etc
- 'domain/': entidades de negocio, servicios de negocio, etc
- 'exception/': manejo de excepciones
- 'infrastructure/': capa de infraestructura
  - 'client/': apis externas
  - 'kafka/': capa de integración con kafka
  - 'mongo/': capa de integración con mongodb
  - 'redis/': capa de integración con redis

## Responsabilidades:
- Mantiene una base de datos de los clientes (tablas: customers)
- Se encarga de la creación y consulta de clientes
- El cliente mantiene un estado de "tiene deuda vencida" (para saber si el cliente es moroso si o no)
- El cliente mantiene una variable número de cuentas de ahorro activas
- El cliente mantiene una variable número de cuentas de corriente activas
- El cliente mantiene una variable número de tarjetas de crédito activas

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

## Api Rest: [CustomerController]
- Crear Cliente:
  - API: [POST] /v1/customers
  - Request Body: [CreateCustomerRequestDto]
    - documentNumber, etc
  - Flujo:
    - Validar que el cliente no exista (con documentNumber) en la base de datos  [mongodb: customers]
    - Registrar el cliente [mongodb: customers]
  - Response Body: [CustomerResponseDto]
    - customerId
    - documentNumber, etc
  - Response Status:
    - 201 Created
  - Response Status Error:
    - 400 Bad Request
    - 409 Conflict
    
- Consultar Cliente por customerId
  - API: [GET] /v1/customers/{customerId}
  - PathVariable:
    - customerId: id del cliente
  - Flujo:
    - Buscar el cliente (con customerId) en la base de datos [mongodb: customers]
  - Response Body: [CustomerResponseDto]
    - customerId
    - documentNumber, etc
  - Response Status:
    - 200 OK
  - Response Status Error:
    - 400 Bad Request
    - 404 Not Found

## Escucha Eventos: