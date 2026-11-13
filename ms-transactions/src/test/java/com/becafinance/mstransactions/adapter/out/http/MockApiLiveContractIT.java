package com.becafinance.mstransactions.adapter.out.http;
import com.becafinance.mstransactions.adapter.out.mockapi.MockApiAccountAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "MOCKAPI_LIVE_URL", matches = ".+")
class MockApiLiveContractIT {
    @Test void configuredRealMockApiContainsAccountForUser2() {
        var result = new MockApiAccountAdapter(System.getenv("MOCKAPI_LIVE_URL"), 10).load(2L);
        assertThat(result.userId()).isEqualTo(2L);
        assertThat(result.id()).isNotBlank();
        assertThat(result.initialBalance()).isNotNegative();
        assertThat(result.purchaseLimit()).isNotNegative();
    }
}
