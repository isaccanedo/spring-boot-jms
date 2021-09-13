package isaccanedo.tutorial.repositories;

import isaccanedo.tutorial.documents.OrderTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderTransactionRepository extends MongoRepository<OrderTransaction, String> {}
