package org.mifos.connector.mockpaymentschema.zeebe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * mockInitiateTransfer and getMockStatus correlate the mockTransferResponse message on transactionId. A missing one
 * used to fail the job with a NullPointerException that named nothing.
 */
class RequiredTransactionIdTest {

    @Test
    @DisplayName("a missing transactionId fails with a message that names it")
    void missingTransactionIdIsNamed() {
        Map<String, Object> variables = new HashMap<>();

        assertThatThrownBy(() -> ZeebeeWorkers.requiredTransactionId(variables)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'transactionId' is missing");
    }

    @Test
    @DisplayName("a transactionId of any type becomes the correlation key as text")
    void transactionIdIsReturnedAsText() {
        assertThat(ZeebeeWorkers.requiredTransactionId(Map.of("transactionId", "abc-123"))).isEqualTo("abc-123");
        assertThat(ZeebeeWorkers.requiredTransactionId(Map.of("transactionId", 42))).isEqualTo("42");
    }
}
