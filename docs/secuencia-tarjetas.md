# Diagrama de secuencia - Tarjetas de débito y crédito

Estos diagramas cubren los endpoints directos de `card-service`. La creación automática de tarjeta de débito al crear una cuenta está en [creación de cuenta y tarjeta de débito](./secuencia-creacion-cuenta-tarjeta-debito.md).

## Crear tarjeta

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant Gateway
    participant Card as card-service
    participant MongoDB

    Usuario->>Gateway: POST /v1/cards/debit o /v1/cards/credit
    Gateway->>Card: Crear tarjeta
    Card->>MongoDB: Guardar tarjeta ACTIVE

    alt Tarjeta creada
        MongoDB-->>Card: Tarjeta
        Card-->>Gateway: 201 Created
        Gateway-->>Usuario: Datos de tarjeta
    else Datos invalidos
        Card-->>Gateway: 400 Bad Request
        Gateway-->>Usuario: ApiErrorResponse
    end
```

## Consultar tarjetas de un cliente

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant Gateway
    participant Card as card-service
    participant MongoDB

    Usuario->>Gateway: GET /v1/cards/customers/{customerId}
    Gateway->>Card: Consultar tarjetas del cliente
    Card->>MongoDB: Buscar tarjetas por customerId
    MongoDB-->>Card: Tarjetas
    Card-->>Gateway: 200 OK
    Gateway-->>Usuario: Lista de tarjetas
```

## Consultar, actualizar o eliminar una tarjeta

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant Gateway
    participant Card as card-service
    participant MongoDB

    Usuario->>Gateway: GET/PUT/DELETE /v1/cards/{type}/{cardId}
    Gateway->>Card: Operar tarjeta
    Card->>MongoDB: Buscar tarjeta por cardId y tipo

    alt Tarjeta existe
        Card->>MongoDB: Leer, actualizar o eliminar
        MongoDB-->>Card: Resultado
        Card-->>Gateway: 200 OK
        Gateway-->>Usuario: Resultado
    else Tarjeta no existe
        MongoDB-->>Card: Sin resultado
        Card-->>Gateway: 404 Not Found
        Gateway-->>Usuario: ApiErrorResponse
    end
```

## Alcance actual

El código actual crea tarjetas de crédito directamente en `card-service`. No se ve todavía un flujo implementado que consulte `overdue-debt-service`, cree una cuenta en `credit-card-account-service` o publique eventos para ciclo de facturación.
