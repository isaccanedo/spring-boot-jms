# spring-boot-jms
:computer: # Configurando JMS com Spring Boot

JMS (Java Message Service) é um Java Message Oriented Middleware usado para enviar mensagens entre clientes e funciona enviando mensagens para uma fila de mensagens que são então levadas quando possível para executar uma transação. Esta postagem se concentrará na implementação de JMS com Spring Boot, que não leva muito tempo para configurar.

### JMS e filas de mensagens, em geral, trazem algumas vantagens sobre o uso de serviços RESTful, como:

- Redundância. Uma mensagem deve confirmar que concluiu sua transação e que agora pode ser removida da fila, mas se a transação falhar, ela pode ser reprocessada. As mensagens também podem ser armazenadas em um banco de dados, permitindo que continuem mais tarde, mesmo se o servidor parar.
