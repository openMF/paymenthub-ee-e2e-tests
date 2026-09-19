package org.mifos.connector.mockpaymentschema.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The amount above which the mock scheme treats a transfer as needing authorization.
 *
 * <p>
 * It used to be read as a String and parsed with {@code Long.valueOf} on every request, inside an {@code @Async}
 * method. A value with decimals - which is an ordinary thing to write for a money amount - made that parse throw after
 * the controller had already answered 202, so the caller never got a callback and never found out. Binding it as a
 * BigDecimal keeps the amount an amount, and the parse happens once.
 * </p>
 */
@ConfigurationProperties(prefix = "threshold")
public record ThresholdProperties(@DefaultValue("20000") BigDecimal amount) {
}
