package com.becafinance.mstransactions.adapter.in.web;
import com.becafinance.mstransactions.domain.model.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class TransactionsCsvWriterTest {
    @Test void exportsHeadersAndEscapesQuotesNewlinesAndFormulas() {
        var row = new Transaction(1L,2L,1L,"PURCHASE",BigDecimal.TEN,"BRL",BigDecimal.TEN,BigDecimal.ONE,
            " =HYPERLINK(\"x\")\n",BigDecimal.TEN,BigDecimal.TEN,BigDecimal.ZERO,TransactionStatus.APPROVED,null,OffsetDateTime.now());
        var csv=new String(TransactionsCsvWriter.write(List.of(row)),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).startsWith("id,type,amount,currency,amountBrl").contains("\"' =HYPERLINK(\"\"x\"\")\n\"");
        assertThat(csv).endsWith("\r\n");
    }
    @Test void emptyReportStillContainsHeader() {
        assertThat(new String(TransactionsCsvWriter.write(List.of()),java.nio.charset.StandardCharsets.UTF_8)).contains("cancelledAt");
    }
}
