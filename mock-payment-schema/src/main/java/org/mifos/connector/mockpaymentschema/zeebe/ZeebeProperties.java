package org.mifos.connector.mockpaymentschema.zeebe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Where the Zeebe broker is and how hard to poll it.
 *
 * <p>
 * The property names are exactly the ones the deployment sets, including the hyphens inside the environment variable
 * names ({@code ZEEBE_CLIENT_MAX-EXECUTION-THREADS}, {@code ZEEBE_CLIENT_POLL-INTERVAL}).
 * {@code DeploymentEnvironmentBindingTest} binds from those exact spellings so a rename cannot slip through.
 * </p>
 *
 * <p>
 * Every value is required, as it was when it was a bare {@code @Value} field. The record is only bound when
 * {@code zeebe.enabled} is not false, because only ZeebeClientConfiguration enables it.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "zeebe")
public record ZeebeProperties(@NotNull @Valid Broker broker, @NotNull @Valid Client client) {

    public record Broker(@NotNull String contactpoint) {
    }

    public record Client(@NotNull Integer maxExecutionThreads, @NotNull Integer pollInterval, @NotNull Integer evenlyAllocatedMaxJobs) {
    }
}
