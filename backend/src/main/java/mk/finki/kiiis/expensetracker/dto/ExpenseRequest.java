package mk.finki.kiiis.expensetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class ExpenseRequest {

    @NotBlank(message = "Item name must not be blank")
    private String item;

    @NotNull(message = "Amount must not be null")
    @PositiveOrZero(message = "Amount must be zero or positive")
    private BigDecimal amount;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
