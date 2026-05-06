# Diagrama de secuencia - Registro de cliente

Flujo de alta de cliente. No usa Kafka; la operación se resuelve de forma síncrona contra MongoDB.

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant Gateway
    participant Customer as customer-service
    participant MongoDB

    Usuario->>Gateway: POST /v1/customers
    Gateway->>Customer: Registrar cliente
    Customer->>MongoDB: Buscar documento de identidad

    alt Documento no existe
        Customer->>MongoDB: Guardar cliente ACTIVE
        MongoDB-->>Customer: Cliente creado
        Customer-->>Gateway: 201 Created
        Gateway-->>Usuario: Cliente registrado
    else Documento duplicado
        MongoDB-->>Customer: Cliente existente
        Customer-->>Gateway: 409 Conflict
        Gateway-->>Usuario: ApiErrorResponse
    else Datos invalidos
        Customer-->>Gateway: 400 Bad Request
        Gateway-->>Usuario: ApiErrorResponse
    end
```

## Consulta de cliente desde otro servicio

`account-service` consulta `customer-service` para validar la existencia y perfil del cliente antes de crear una cuenta.

```mermaid
sequenceDiagram
    autonumber
    participant Account as account-service
    participant Customer as customer-service
    participant MongoDB

    Account->>Customer: GET /v1/customers/{customerId}
    Customer->>MongoDB: Buscar cliente

    alt Cliente existe
        MongoDB-->>Customer: Cliente
        Customer-->>Account: 200 OK + datos del cliente
    else Cliente no existe
        MongoDB-->>Customer: Sin resultado
        Customer-->>Account: 404 Not Found
    else Timeout/error tecnico
        Customer--x Account: Error propagado
    end
```
