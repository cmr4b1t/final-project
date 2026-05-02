# Proyecto Final

Gestionar clientes financieros y sus productos bancarios

## Módulos

* customer-service
* eureka-service
* gateway-service
* config-server

### Guía para levantar el proyecto

1. Compilar todo desde la raíz
    ````bash
    mvn clean package -DskipTests
    ````    
2. Construir imágenes y levantar servicios
    ````bash
    docker compose up -d --build
    ````    
   
### Comandos para ver logs

* Ver logs de todos los servicios
    ````bash
   docker compose logs -f
   ````
* Ver logs de servicios específicos
   ````bash
   docker compose logs -f customer-service
    ````

### Comandos para detener todo el proyecto

* Detener todos los servicios
   ````bash
   docker compose down
    ````
* Detener y eliminar volúmenes (limpieza total)
   ````bash
   docker compose down -v
   ````