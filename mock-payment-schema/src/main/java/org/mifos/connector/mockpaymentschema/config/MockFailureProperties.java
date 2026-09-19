package org.mifos.connector.mockpaymentschema.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

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
@ConfigurationProperties(prefix = "mockfailure")
public record MockFailureProperties(@DefaultValue("50") int percentage) {
}
