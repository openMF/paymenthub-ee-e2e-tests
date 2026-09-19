package org.mifos.connector.mockpaymentschema.zeebe;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * The application has to start with Zeebe switched off.
 *
 * <p>
 * It did not. {@code ZeebeeWorkers} was an unconditional component that dereferenced the client bean in
 * {@code @PostConstruct}, so {@code zeebe.enabled=false} produced
 * {@code BeanCreationException ... NullPointerException: ... "this.zeebeClient" is null} and the service refused to
 * start. That also made it impossible to bring the service up for a test without a broker - which is why this is the
 * first test the module has ever had.
 * </p>
 */
@SpringBootTest(properties = { "zeebe.enabled=false", "camel.springboot.main-run-controller=false" })
class ZeebeDisabledContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("the context starts with zeebe.enabled=false, and neither the client nor the workers are created")
    void contextStartsWithoutZeebe() {
        assertThat(context.getBeanNamesForType(ZeebeClient.class)).isEmpty();
        assertThat(context.getBeanNamesForType(ZeebeeWorkers.class)).isEmpty();
    }

    @Test
    @DisplayName("the REST layer and the callback client are still there")
    void theRestOfTheApplicationIsStillWired() {
        assertThat(context.containsBean("batchApi")).isTrue();
        assertThat(context.containsBean("callbackRestClient")).isTrue();
    }
}
