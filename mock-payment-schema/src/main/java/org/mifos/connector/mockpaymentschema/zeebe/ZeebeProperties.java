package org.mifos.connector.mockpaymentschema.zeebe;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Where the Zeebe broker is and how hard to poll it.
 *
 * <p>
 * The property names are exactly the ones the deployment sets, including the hyphens inside the environment variable
 * names ({@code ZEEBE_CLIENT_MAX-EXECUTION-THREADS}, {@code ZEEBE_CLIENT_POLL-INTERVAL}).
 * {@code DeploymentEnvironmentBindingTest} binds from those exact spellings so a rename cannot slip through.
 * </p>
 *
 */
@ConfigurationProperties(prefix = "zeebe")
public record ZeebeProperties(@DefaultValue Broker broker, @DefaultValue Client client) {

    public record Broker(@DefaultValue("127.0.0.1:26500") String contactpoint) {
    }

    public record Client(@DefaultValue("50") int maxExecutionThreads, @DefaultValue("10") int pollInterval,
            @DefaultValue("1000") int evenlyAllocatedMaxJobs) {
    }
}
