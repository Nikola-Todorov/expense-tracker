package mk.finki.kiiis.expensetracker.repository;

import mk.finki.kiiis.expensetracker.model.Expense;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends MongoRepository<Expense, String> {
}
