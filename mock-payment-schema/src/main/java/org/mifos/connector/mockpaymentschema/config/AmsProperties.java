package org.mifos.connector.mockpaymentschema.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the local AMS behaviour of the mock scheme.
 *
 * <p>
 * {@code serverCertCheck} was already in application.yml, set to false, and read by nothing. It now does what its name
 * says: it decides whether the outgoing callback client verifies the server certificate. The default stays false, so
 * the behaviour is the same as before.
 * </p>
 *
 * <p>
 * These two are the only defaults in the module's records, and both match what the code did before: {@code enabled} was
 * read as {@code @Value("${ams.local.enabled:false}")}, and {@code serverCertCheck} was not read at all, so
 * certificates were never verified.
 * </p>
 */
@ConfigurationProperties(prefix = "ams")
public record AmsProperties(@DefaultValue Local local) {

    public record Local(@DefaultValue("false") boolean enabled, @DefaultValue("false") boolean serverCertCheck) {
    }
}
