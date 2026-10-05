package org.mifos.connector.mockpaymentschema;

import org.mifos.connector.mockpaymentschema.config.AmsProperties;
import org.mifos.connector.mockpaymentschema.config.MockFailureProperties;
import org.mifos.connector.mockpaymentschema.config.ThresholdProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("org.mifos.connector.mockpaymentschema")
@EnableConfigurationProperties({ AmsProperties.class, MockFailureProperties.class, ThresholdProperties.class })
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class MockPaymentSchemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockPaymentSchemaApplication.class, args);
    }

}
