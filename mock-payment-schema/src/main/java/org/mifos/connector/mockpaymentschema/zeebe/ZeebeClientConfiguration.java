package org.mifos.connector.mockpaymentschema.zeebe;

import io.camunda.zeebe.client.ZeebeClient;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("${zeebe.enabled:true}")
@EnableConfigurationProperties(ZeebeProperties.class)
public class ZeebeClientConfiguration {

    private final ZeebeProperties zeebeProperties;

    public ZeebeClientConfiguration(ZeebeProperties zeebeProperties) {
        this.zeebeProperties = zeebeProperties;
    }

    @Bean(destroyMethod = "close")
    public ZeebeClient setup() {
        return ZeebeClient.newClientBuilder().gatewayAddress(zeebeProperties.broker().contactpoint()).usePlaintext()
                .defaultJobPollInterval(Duration.ofMillis(zeebeProperties.client().pollInterval())).defaultJobWorkerMaxJobsActive(2000)
                .numJobWorkerExecutionThreads(zeebeProperties.client().maxExecutionThreads()).build();
    }
}
