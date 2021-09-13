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

Nesse cenário, o OrderTransactionReceiver obtém mensagens de OrderTransactionQueue e as salva no banco de dados usando o transactionRepository. O nome do método que recebe a mensagem é irrelevante e pode ser chamado como você quiser, embora receiveMessage seja bastante apropriado - mas deve ter a anotação @JmsListener com propriedade de destino definindo o nome da fila. Incluída nesta anotação está a propriedade containerFactory, que não é necessária se você estiver satisfeito com o DefaultJmsListenerContainerFactory padrão fornecido pelo Spring Boot.

Portanto, agora que podemos pegar mensagens da fila, provavelmente é uma boa ideia saber como colocá-las lá em primeiro lugar.

```
@RestController
@RequestMapping("/transaction")
public class OrderTransactionController {

  @Autowired private JmsTemplate jmsTemplate;

  @PostMapping("/send")
  public void send(@RequestBody OrderTransaction transaction) {
    System.out.println("Sending a transaction.");
    // Post message to the message queue named "OrderTransactionQueue"
    jmsTemplate.convertAndSend("OrderTransactionQueue", transaction);
  }
}
```
Há muito ruído neste exemplo, pois há um código que não está relacionado à postagem na fila de mensagens. Há apenas uma linha que é necessária para enviar a mensagem e caso não tenha ficado clara, adicionei um comentário ao exemplo. Na verdade, essa declaração anterior é uma mentira; são duas linhas de código, mas isso é apenas se você incluiu injetar no JmsTemplate no controlador. O motivo pelo qual escrevi este exemplo dentro de um @RestController é para demonstrar um possível uso da fila de mensagens - um usuário faz uma solicitação por meio da API REST, que enviará uma mensagem para a fila para ser executada em algum ponto. Enquanto isso acontece, o usuário continua com o que estava fazendo, pois não precisa esperar o término da execução da solicitação.

A peça final desse quebra-cabeça simples é o aplicativo principal definido pela classe com @SpringBootApplication.

```
@EnableJms
@ComponentScan(basePackages = "lankydan.tutorial")
@EnableMongoRepositories(basePackages = "lankydan.tutorial")
@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  // Only required due to defining myFactory in the receiver
  @Bean
  public JmsListenerContainerFactory<?> myFactory(
      ConnectionFactory connectionFactory,
      DefaultJmsListenerContainerFactoryConfigurer configurer) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    return factory;
  }

  // Serialize message content to json using TextMessage
  @Bean
  public MessageConverter jacksonJmsMessageConverter() {
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    converter.setTargetType(MessageType.TEXT);
    converter.setTypeIdPropertyName("_type");
    return converter;
  }

}
```

Vamos começar com a anotação @EnableJms, que dá uma indicação clara para que serve, mas para ser um pouco mais preciso, ela aciona a descoberta de métodos marcados com @JmsListener e cria os próprios ouvintes nos bastidores. Portanto, se você se lembrar, este será o método recieveMessage definido em OrderTransactionReceiver. As próximas duas anotações, @ComponentScan e @EnableMongoRepositories, não são necessárias para configurar o JMS, mas devido à forma como as classes neste exemplo estão espalhadas, elas devem ser adicionadas para que OrderTransactionController e OrderTransactionRepository possam ser encontrados.

Indo além das anotações na classe, lembre-se de que myFactory que foi especificada em @JmsListener é o código que a define. Esta implementação corresponde ao que o DefaultJmsListenerContainerFactory padrão seria se decidíssemos não especificar uma fábrica dentro do @JmsListener. Um MessageConverter deve ser definido como a implementação padrão e só pode converter tipos básicos, o que o objeto OrderTransaction não é. Esta implementação usa JSON para passar as mensagens de e para a fila. Spring Boot é gentil o suficiente para detectar MessageConverter e fazer uso dele no JmsTemplate e JmsListenerContainerFactory.

Agora que temos tudo reunido, pode ser testado para verificar se realmente funciona por meio do uso de algumas linhas de impressão bem posicionadas que você pode ver nos exemplos, podemos ver como isso vai de OrderTransactionController e para OrderTransactionReceiver.

Fazendo uma solicitação POST para:

```
localhost:8080/transaction/send
```

... com o corpo da solicitação de:

```
{
  "from":"you",
  "to":"me",
  "amount":200
}
```

E olhando para o console, podemos ver:

```
Sending a transaction.
Received <OrderTransaction(from=you, to=me, amount=200)>
```

Então, provamos que funciona, mas o que acontece se a transação falhar devido à ocorrência de uma exceção? Conforme mencionado no início desta postagem, as filas de mensagens fornecem redundância, pois a transação será repetida se falhar. Para testar isso, lancei uma exceção e adicionei um contador ao método receiveMessage em OrderTransactionReceiver.

```
Sending a transaction.
<1> Received <OrderTransaction(from=you, to=me, amount=200)>
2017-06-17 19:12:59.748  WARN 2352 --- [enerContainer-1] o.s.j.l.DefaultMessageListenerContainer  : Execution of JMS message listener failed, and no ErrorHandler has been set.
...
<7> Received <OrderTransaction(from=you, to=me, amount=200)>
```

Obviamente, removi as exceções e mensagens reais da saída do console, mas isso exibe o que acontece quando a transação falha de forma bastante clara. Como podemos ver, a transação falha cada vez que a mensagem é reenviada até que uma tentativa máxima de 7 tentativas tenha sido feita (1 tentativa inicial e 6 tentativas).

Uma série de novas entregas podem ser configuradas, mas isso requer um pouco mais de configuração. Para poder alterar isso, precisamos instalar o Apache ActiveMQ, que permite configuração extra além do que é fornecido pelo Spring Boot por padrão. Quando o ActiveMQ é instalado e o serviço está instalado e funcionando (informações extras de instalação encontradas aqui), apenas uma pequena alteração no código real é necessária. Na verdade, não é realmente uma alteração de código, mas uma alteração de propriedade feita no arquivo application.properties, que deve ser colocado na pasta de recursos, caso ainda não exista.

```
spring.activemq.user=admin
spring.activemq.password=admin
spring.activemq.broker-url=tcp://localhost:61616?jms.redeliveryPolicy.maximumRedeliveries=1
```

Como podemos ver no snippet acima, a quantidade máxima de novas entregas agora será limitada a 1; as outras propriedades são o nome de usuário e a senha padrão do ActiveMQ. No caso de você começar a se perguntar sobre qual porta está sendo usada por aqui pelo broker-url, esta é a porta padrão em que o ActiveMQ está sendo executado, então deve (espero ...) funcionar imediatamente se você tentar.

Voltando à saída do console, ele também mencionou não ter um ErrorHandler definido, então vamos configurar um adicionando algum código extra à fábrica que foi criado anteriormente.

```
@Bean
public JmsListenerContainerFactory<?> myFactory(
    ConnectionFactory connectionFactory,
    DefaultJmsListenerContainerFactoryConfigurer configurer) {
  DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();

  // anonymous class
  factory.setErrorHandler(
      new ErrorHandler() {
        @Override
        public void handleError(Throwable t) {
          System.err.println("An error has occurred in the transaction");
        }
      });

  // lambda function
  factory.setErrorHandler(t -> System.err.println("An error has occurred in the transaction"));

  configurer.configure(factory, connectionFactory);
  return factory;
}
```

Agora, quando ocorre um erro, o horrível rastreio da pilha não afetará o log do console - a menos que você queira, é claro. Incluí a classe anônima e as versões de função lambda da implementação do ErrorHandler para que fique um pouco claro o que ele está fazendo.

Configurando as entregas máximas e adicionando o ErrorHandler, a saída do console agora será semelhante a:

```
Sending a transaction.
<1> Received <OrderTransaction(from=you, to=me, amount=200)>
An error has occurred in the transaction
<2> Received <OrderTransaction(from=you, to=me, amount=200)>
An error has occurred in the transaction
```

# Conclusão
Configuramos um JMS simples usando Spring Boot e Apache ActiveMQ e obtivemos uma pequena introdução sobre por que filas de mensagens como JMS podem ser úteis como fornecer redundância, mensagens assíncronas e acoplamento flexível.
