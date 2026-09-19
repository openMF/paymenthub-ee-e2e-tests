package org.mifos.connector.mockpaymentschema.zeebe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.mockpaymentschema.config.AmsProperties;
import org.mifos.connector.mockpaymentschema.config.MockFailureProperties;

/**
 * The five scenarios each own the Zeebe variable they set, so a call site cannot name one that does not exist.
 *
 * <p>
 * Before, the scenario was a string matched in a switch with no default: a typo fell through, the variables came back
 * untouched and the job completed as a success, so the mock quietly stopped mocking. The variable names themselves are
 * the contract with the BPMN deployed in gazelle and are pinned here.
 * </p>
 */
class MockScenarioTest {

    private static ZeebeeWorkers workersWithFailurePercentage(int percentage) {
        return new ZeebeeWorkers(null, null, new AmsProperties(new AmsProperties.Local(true, false)),
                new MockFailureProperties(percentage));
    }

    @Test
    @DisplayName("each scenario carries the variable name the workflows read")
    void scenariosCarryTheirWireVariableNames() {
        assertThat(MockScenario.BLOCK.failureVariable()).isEqualTo("transferPrepareFailed");
        assertThat(MockScenario.BOOK.failureVariable()).isEqualTo("transferCreateFailed");
        assertThat(MockScenario.RELEASE.failureVariable()).isEqualTo("transferReleaseFailed");
        assertThat(MockScenario.PAYEE_LOOKUP.failureVariable()).isEqualTo("partyLookupFailed");
        assertThat(MockScenario.TRANSACTION.failureVariable()).isEqualTo("transactionFailed");
    }

    @Test
    @DisplayName("at 0 percent - what the deployment sets - nothing ever fails")
    void zeroPercentNeverFails() {
        ZeebeeWorkers workers = workersWithFailurePercentage(0);

        for (MockScenario scenario : MockScenario.values()) {
            for (int attempt = 0; attempt < 500; attempt++) {
                Map<String, Object> variables = workers.setSuccessOrFailure(scenario, new HashMap<>());
                assertThat(variables).containsEntry(scenario.failureVariable(), false);
            }
        }
    }

    @Test
    @DisplayName("at 100 percent everything fails except the one draw of 0, exactly as before")
    void hundredPercentFailsOnEveryDrawAboveZero() {
        ZeebeeWorkers workers = workersWithFailurePercentage(100);
        int succeeded = 0;

        for (int attempt = 0; attempt < 2000; attempt++) {
            if (Boolean.FALSE.equals(workers.setSuccessOrFailure(MockScenario.TRANSACTION, new HashMap<>()).get("transactionFailed"))) {
                succeeded++;
            }
        }

        // the old code excluded a draw of exactly 0 from failing, and that quirk is preserved: roughly 1 in 100 gets
        // through
        assertThat(succeeded).isBetween(1, 100);
    }

    @Test
    @DisplayName("the scenario only touches its own variable")
    void otherVariablesAreLeftAlone() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("transactionId", "txn-1");

        Map<String, Object> result = workersWithFailurePercentage(0).setSuccessOrFailure(MockScenario.BLOCK, variables);

        assertThat(result).containsEntry("transactionId", "txn-1").containsEntry("transferPrepareFailed", false).hasSize(2);
    }
}
