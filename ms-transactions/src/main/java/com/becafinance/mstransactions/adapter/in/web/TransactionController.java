package com.becafinance.mstransactions.adapter.in.web;
import com.becafinance.mstransactions.application.dto.*;
import com.becafinance.mstransactions.application.port.in.*;
import com.becafinance.mstransactions.domain.model.Transaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/transactions")
public class TransactionController {
    private final ProcessTransactionPort purchases;
    private final FindTransactionPort queries;
    private final ManageTransactionsPort management;
    public TransactionController(ProcessTransactionPort purchases, FindTransactionPort queries, ManageTransactionsPort management) {
        this.purchases = purchases; this.queries = queries; this.management = management;
    }
    @PostMapping public ResponseEntity<TransactionResponse> process(@AuthenticationPrincipal UserIdentity user,
            @Valid @RequestBody TransactionInput input) {
        var result = purchases.process(user.id(), input);
        return ResponseEntity.created(URI.create("/transactions/" + result.id())).body(TransactionResponse.from(result));
    }
    @GetMapping("/{id}") public TransactionResponse find(@AuthenticationPrincipal UserIdentity user, @PathVariable @Positive long id) {
        return TransactionResponse.from(queries.find(user.id(), id));
    }
    @GetMapping public List<TransactionResponse> list(@AuthenticationPrincipal UserIdentity user, @Valid @ModelAttribute TransactionFilter filter) {
        return management.list(user.id(), filter).stream().map(TransactionResponse::from).toList();
    }
    @PutMapping("/{id}") public TransactionResponse update(@AuthenticationPrincipal UserIdentity user, @PathVariable @Positive long id,
            @Valid @RequestBody MetadataInput input) { return TransactionResponse.from(management.update(user.id(), id, input)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> cancel(@AuthenticationPrincipal UserIdentity user, @PathVariable @Positive long id) {
        management.cancel(user.id(), id); return ResponseEntity.noContent().build();
    }
    @GetMapping("/analysis") public List<ExpenseSummary> analyze(@AuthenticationPrincipal UserIdentity user,
            @Valid @ModelAttribute TransactionFilter filter,
            @RequestParam(defaultValue = "CATEGORY") @jakarta.validation.constraints.Pattern(regexp = "CATEGORY|DAY|MONTH") String groupBy) {
        return management.analyze(user.id(), filter, groupBy);
    }
    @GetMapping(value = "/export", produces = "text/csv") public ResponseEntity<byte[]> export(@AuthenticationPrincipal UserIdentity user,
            @Valid @ModelAttribute TransactionFilter filter) {
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=transactions.csv")
            .contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(TransactionsCsvWriter.write(management.export(user.id(), filter)));
    }
}
