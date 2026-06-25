package mk.finki.kiiis.expensetracker;

import mk.finki.kiiis.expensetracker.model.Expense;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpenseModelTest {

    @Test
    void expenseStoresItemAndAmount() {
        Expense expense = new Expense("Coffee", new BigDecimal("3.50"));
        assertEquals("Coffee", expense.getItem());
        assertEquals(new BigDecimal("3.50"), expense.getAmount());
    }

    @Test
    void settersUpdateValues() {
        Expense expense = new Expense();
        expense.setItem("Lunch");
        expense.setAmount(new BigDecimal("12.00"));
        assertEquals("Lunch", expense.getItem());
        assertEquals(new BigDecimal("12.00"), expense.getAmount());
    }
}
