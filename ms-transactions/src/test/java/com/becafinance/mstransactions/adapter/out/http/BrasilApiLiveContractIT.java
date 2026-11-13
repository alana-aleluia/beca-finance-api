package com.becafinance.mstransactions.adapter.out.http;
import com.becafinance.mstransactions.adapter.out.brasilapi.BrasilApiExchangeAdapter;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;
@EnabledIfEnvironmentVariable(named = "BRASILAPI_LIVE_URL", matches = ".+")
class BrasilApiLiveContractIT {
    @Test void realUsdQuoteHasPositiveRateAndHistoricalDate() {
        var quote = new BrasilApiExchangeAdapter(System.getenv("BRASILAPI_LIVE_URL"),15,Clock.systemUTC()).quote("USD");
        assertThat(quote.rate()).isPositive();
        assertThat(quote.date()).isBefore(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        assertThat(quote.bulletin()).isNotBlank();
    }
}
