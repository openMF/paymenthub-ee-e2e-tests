package org.mifos.connector.mockpaymentschema.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * How often the mock scheme should pretend a step failed, as a percentage.
 *
 * <p>
 * The prefix is the canonical form of the {@code mockFailure} key that application.yml and the deployment already use.
 * Property names are case-insensitive once bound, so both {@code mockFailure.percentage} in the yaml and the
 * {@code mockFailure_percentage} environment variable the operator sets still reach this record -
 * {@code DeploymentEnvironmentBindingTest} pins both spellings, because the deployment sets 0 and a value of 50 would
 * make half the demo payments fail at random.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "mockfailure")
public record MockFailureProperties(@NotNull Integer percentage) {
}
