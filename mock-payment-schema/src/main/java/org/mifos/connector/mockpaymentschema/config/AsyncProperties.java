package org.mifos.connector.mockpaymentschema.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Sizing of the executor that runs the batch authorization callbacks. */
@ConfigurationProperties(prefix = "async")
public record AsyncProperties(@DefaultValue("10") int corePoolSize, @DefaultValue("10") int maxPoolSize,
        @DefaultValue("100") int queueCapacity) {
}
