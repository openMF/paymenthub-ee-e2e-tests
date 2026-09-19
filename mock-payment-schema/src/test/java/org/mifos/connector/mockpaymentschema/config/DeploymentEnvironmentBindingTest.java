package org.mifos.connector.mockpaymentschema.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.mockpaymentschema.zeebe.ZeebeProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Binds every properties record from the exact spellings the gazelle deployment uses.
 *
 * <p>
 * These names are awkward on purpose and the awkwardness is the point. The operator sets
 * {@code ZEEBE_CLIENT_MAX-EXECUTION-THREADS} with hyphens inside the environment variable name, and
 * {@code mockFailure_percentage} in mixed case with an underscore. Moving a property from {@code @Value} to
 * {@code @ConfigurationProperties} changes how those names resolve, so each one is pinned here: if a rename or a prefix
 * change stops a value binding, this test fails instead of the cluster.
 * </p>
 *
 * <p>
 * {@code mockFailure_percentage} matters most. The deployment sets it to 0 so the demo flows are deterministic; the
 * file default is 50. If it silently stopped binding, half the demo payments would start failing at random.
 * </p>
 */
class DeploymentEnvironmentBindingTest {

    /** Exactly what {@code kubectl get pod ... -o json} shows on the running mock-payment-schema pod. */
    private static final Map<String, Object> DEPLOYMENT_ENVIRONMENT = Map.of("SPRING_PROFILES_ACTIVE", "bb", "DFSPIDS",
            "greenbank,redbank,bluebank", "LOGGING_LEVEL_ROOT", "INFO", "ZEEBE_BROKER_CONTACTPOINT", "paymenthub-infra-zeebe-gateway:26500",
            "ZEEBE_CLIENT_MAX-EXECUTION-THREADS", "50", "ZEEBE_CLIENT_POLL-INTERVAL", "10", "mockFailure_percentage", "0");

    /** The application.yml values that the deployment does not override. */
    private static Map<String, Object> fileDefaults() {
        return Map.of("zeebe.client.evenly-allocated-max-jobs", 1000, "ams.local.enabled", true, "ams.local.server-cert-check", false,
                "mockFailure.percentage", 50, "threshold.amount", 20000, "async.core_pool_size", 10, "async.max_pool_size", 10,
                "async.queue_capacity", 100);
    }

    /** Binds the way the application does: environment first, file defaults behind it, JSR-303 validation on. */
    private static <T> T bind(Map<String, Object> environmentVariables, String prefix, Class<T> type) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environmentVariables));
        environment.getPropertySources().addLast(new MapPropertySource("file-defaults", fileDefaults()));

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return new Binder(ConfigurationPropertySources.get(environment))
                .bind(prefix, Bindable.of(type), new ValidationBindHandler(validator)).get();
    }

    @Test
    @DisplayName("ZEEBE_BROKER_CONTACTPOINT and the two hyphenated ZEEBE_CLIENT_* variables reach ZeebeProperties")
    void zeebePropertiesBindFromTheDeploymentEnvironment() {
        ZeebeProperties properties = bind(DEPLOYMENT_ENVIRONMENT, "zeebe", ZeebeProperties.class);

        assertThat(properties.broker().contactpoint()).isEqualTo("paymenthub-infra-zeebe-gateway:26500");
        assertThat(properties.client().maxExecutionThreads()).isEqualTo(50);
        assertThat(properties.client().pollInterval()).isEqualTo(10);
        // not set by the deployment, so it has to come from application.yml
        assertThat(properties.client().evenlyAllocatedMaxJobs()).isEqualTo(1000);
    }

    @Test
    @DisplayName("mockFailure_percentage from the deployment wins over the 50 in application.yml")
    void mockFailurePercentageBindsFromTheDeploymentEnvironment() {
        assertThat(bind(DEPLOYMENT_ENVIRONMENT, "mockfailure", MockFailureProperties.class).percentage()).isZero();
    }

    @Test
    @DisplayName("without the environment variable the file default of 50 is what binds")
    void mockFailurePercentageFallsBackToTheFileDefault() {
        assertThat(bind(Map.of(), "mockfailure", MockFailureProperties.class).percentage()).isEqualTo(50);
    }

    @Test
    @DisplayName("threshold.amount binds as a BigDecimal, decimals included")
    void thresholdBindsAsBigDecimal() {
        assertThat(bind(Map.of(), "threshold", ThresholdProperties.class).amount()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(bind(Map.of("THRESHOLD_AMOUNT", "20000.50"), "threshold", ThresholdProperties.class).amount())
                .isEqualByComparingTo(new BigDecimal("20000.50"));
    }

    @Test
    @DisplayName("the async pool sizes still bind although the keys use underscores")
    void asyncPropertiesBind() {
        AsyncProperties properties = bind(Map.of(), "async", AsyncProperties.class);

        assertThat(properties.corePoolSize()).isEqualTo(10);
        assertThat(properties.maxPoolSize()).isEqualTo(10);
        assertThat(properties.queueCapacity()).isEqualTo(100);
    }

    @Test
    @DisplayName("ams.local keeps its file defaults and both flags are readable")
    void amsPropertiesBind() {
        AmsProperties properties = bind(Map.of(), "ams", AmsProperties.class);

        assertThat(properties.local().enabled()).isTrue();
        assertThat(properties.local().serverCertCheck()).isFalse();
    }

    @Test
    @DisplayName("a threshold that is not a number is refused at binding time, naming the property")
    void malformedThresholdIsRefused() {
        assertThatThrownBy(() -> bind(Map.of("THRESHOLD_AMOUNT", "twenty thousand"), "threshold", ThresholdProperties.class))
                .hasStackTraceContaining("threshold.amount");
    }

}
