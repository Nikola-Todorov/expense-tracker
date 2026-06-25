package mk.finki.kiiis.expensetracker.controller;

import jakarta.validation.Valid;
import mk.finki.kiiis.expensetracker.dto.ExpenseRequest;
import mk.finki.kiiis.expensetracker.model.Expense;
import mk.finki.kiiis.expensetracker.repository.ExpenseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseRepository repository;

    public ExpenseController(ExpenseRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Expense> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Expense> getOne(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Expense create(@Valid @RequestBody ExpenseRequest request) {
        Expense expense = new Expense(request.getItem(), request.getAmount());
        return repository.save(expense);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> update(@PathVariable Long id,
                                          @Valid @RequestBody ExpenseRequest request) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setItem(request.getItem());
                    existing.setAmount(request.getAmount());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
