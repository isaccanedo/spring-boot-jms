# spring-boot-jms
:computer: # Configurando JMS com Spring Boot

JMS (Java Message Service) é um Java Message Oriented Middleware usado para enviar mensagens entre clientes e funciona enviando mensagens para uma fila de mensagens que são então levadas quando possível para executar uma transação. Esta postagem se concentrará na implementação de JMS com Spring Boot, que não leva muito tempo para configurar.

### JMS e filas de mensagens, em geral, trazem algumas vantagens sobre o uso de serviços RESTful, como:

- Redundância. Uma mensagem deve confirmar que concluiu sua transação e que agora pode ser removida da fila, mas se a transação falhar, ela pode ser reprocessada. As mensagens também podem ser armazenadas em um banco de dados, permitindo que continuem mais tarde, mesmo se o servidor parar.
- Mensagens assíncronas. Como o tempo de processamento da mensagem não pode ser garantido, o cliente que a enviou pode prosseguir de forma assíncrona até a conclusão da transação. Devido a isso, a fila deve ser usada para gravar dados (POST se você estiver pensando em uma mentalidade RESTful).
- Acoplamento solto. Os serviços não interagem diretamente e só sabem onde está a fila de mensagens, onde um serviço envia mensagens e o outro as recebe.

Agora, vamos começar a implementá-lo de fato. Conforme mencionado anteriormente, usaremos Spring Boot, que torna tudo fácil e agradável de configurar, e Apache ActiveMQ para criar e gerenciar a fila de mensagens.

As dependências do Maven necessárias para configurar o JMS são mostradas abaixo (algumas dependências extras não relacionadas ao JMS foram usadas e não são mostradas no snippet de código):

```
<dependencies>
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-activemq</artifactId>
  </dependency>
  <dependency>
      <groupId>org.apache.activemq</groupId>
      <artifactId>activemq-broker</artifactId>
  </dependency>
  <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
  </dependency>
  <!-- unrelated dependencies -->
</dependencies>
```
A primeira coisa que veremos é o receptor, que pegará uma mensagem do início da fila e executará uma transação.

```
@Component
public class OrderTransactionReceiver {

  @Autowired
  private OrderTransactionRepository transactionRepository;

  @JmsListener(destination = "OrderTransactionQueue", containerFactory = "myFactory")
  public void receiveMessage(OrderTransaction transaction) {
    System.out.println("Received <" + transaction + ">");
    transactionRepository.save(transaction);
  }
}
```

