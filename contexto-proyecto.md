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
- Usar patrones de diseño de software
- Usar Docker para containerización
- Seguir el enfoque database per service
- Los microservicios deben obtener su configuración (técnica y funcional) de un config-server
- Los microservicios deben usar programación funcional
- Se usará un eureka-server como service registry
- Se usará un spring gateway como api gateway
- 