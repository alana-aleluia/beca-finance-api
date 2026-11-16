package com.becafinance.mstransactions.adapter.in.web;
import com.becafinance.mstransactions.domain.model.Transaction;
import java.util.List;
import java.nio.charset.StandardCharsets;
public final class TransactionsCsvWriter {
    private TransactionsCsvWriter() {}
    public static byte[] write(List<Transaction> transactions) {
        var csv = new StringBuilder("id,type,amount,currency,amountBrl,exchangeRate,exchangeDate,category,status,rejectionReason,balanceBefore,balanceAfter,destinationUserId,createdAt,cancelledAt,description\r\n");
        for (var t : transactions) {
            Object[] values = {t.id(), t.type(), t.amount(), t.currency(), t.amountBrl(), t.exchangeRate(), t.exchangeDate(),
                t.category(), t.status(), t.rejectionReason(), t.balanceBefore(), t.balanceAfter(), t.destinationUserId(), t.createdAt(), t.cancelledAt(), t.description()};
            for (int i = 0; i < values.length; i++) { if (i > 0) csv.append(','); csv.append(escape(values[i])); }
            csv.append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    private static String escape(Object value) {
        String text = value == null ? "" : value.toString();
        String trimmed = text.stripLeading();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
