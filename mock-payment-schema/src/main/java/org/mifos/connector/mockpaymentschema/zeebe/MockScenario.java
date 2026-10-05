package org.mifos.connector.mockpaymentschema.zeebe;

/**
 * The steps the mock scheme can pretend to fail, and the Zeebe variable that says so for each.
 *
 * <p>
 * This used to be five string literals passed into a switch. A typo at a call site fell through the switch, the
 * variables came back untouched, and the job completed as a success - so the mock silently stopped mocking. As an enum
 * a typo is a compile error.
 * </p>
 */
public enum MockScenario {

    BLOCK(ZeebeVariables.TRANSFER_PREPARE_FAILED), BOOK(ZeebeVariables.TRANSFER_CREATE_FAILED), RELEASE(
            ZeebeVariables.TRANSFER_RELEASE_FAILED), PAYEE_LOOKUP(
                    ZeebeVariables.PARTY_LOOKUP_FAILED), TRANSACTION(ZeebeVariables.TRANSACTION_FAILED);

    private final String failureVariable;

    MockScenario(String failureVariable) {
        this.failureVariable = failureVariable;
    }

    /** The Zeebe variable this scenario sets to true when it pretends to fail. */
    public String failureVariable() {
        return failureVariable;
    }
}
