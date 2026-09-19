package org.mifos.connector.mockpaymentschema.zeebe;

import static org.mifos.connector.mockpaymentschema.zeebe.ZeebeVariables.PARTY_LOOKUP_FAILED;
import static org.mifos.connector.mockpaymentschema.zeebe.ZeebeVariables.TRANSACTION_FAILED;
import static org.mifos.connector.mockpaymentschema.zeebe.ZeebeVariables.TRANSACTION_ID;
import static org.mifos.connector.mockpaymentschema.zeebe.ZeebeVariables.TRANSFER_CODE;
import static org.mifos.connector.mockpaymentschema.zeebe.ZeebeVariables.TRANSFER_CREATE_FAILED;
import static org.mifos.connector.mockpaymentschema.zeebe.ZeebeVariables.TRANSFER_PREPARE_FAILED;
import static org.mifos.connector.mockpaymentschema.zeebe.ZeebeVariables.TRANSFER_SETTLEMENT_FAILED;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.json.JSONObject;
import org.mifos.connector.mockpaymentschema.config.AmsProperties;
import org.mifos.connector.mockpaymentschema.config.MockFailureProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * The nine workers the mock scheme registers on Zeebe.
 *
 * <p>
 * Conditional on the same {@code zeebe.enabled} expression as {@link ZeebeClientConfiguration}. It used to be an
 * unconditional component that took the client with {@code @Autowired(required = false)} and dereferenced it in
 * {@code @PostConstruct}, so setting {@code zeebe.enabled=false} - the switch this service documents through its own
 * default - made the application refuse to start with a NullPointerException instead of starting without Zeebe.
 * </p>
 */
@Component
@ConditionalOnExpression("${zeebe.enabled:true}")
public class ZeebeeWorkers {

    public static final String WORKER_PARTY_LOOKUP_LOCAL = "party-lookup-local-";
    public static final String WORKER_PAYEE_COMMIT_TRANSFER = "payee-commit-transfer-";
    public static final String WORKER_PAYEE_QUOTE = "payee-quote-";
    public static final String WORKER_PAYER_LOCAL_QUOTE = "payer-local-quote-";
    public static final String WORKER_INTEROP_PARTY_REGISTRATION = "interop-party-registration-";
    public static final String WORKER_PAYEE_DEPOSIT_TRANSFER = "payee-deposit-transfer-";

    private final Logger logger = LoggerFactory.getLogger(ZeebeeWorkers.class);

    private final ZeebeClient zeebeClient;

    private final ZeebeProperties zeebeProperties;

    private final AmsProperties amsProperties;

    private final MockFailureProperties mockFailureProperties;

    public ZeebeeWorkers(ZeebeClient zeebeClient, ZeebeProperties zeebeProperties, AmsProperties amsProperties,
            MockFailureProperties mockFailureProperties) {
        this.zeebeClient = zeebeClient;
        this.zeebeProperties = zeebeProperties;
        this.amsProperties = amsProperties;
        this.mockFailureProperties = mockFailureProperties;
    }

    @PostConstruct
    public void setupWorkers() {
        int workerMaxJobs = zeebeProperties.client().evenlyAllocatedMaxJobs();

        zeebeClient.newWorker().jobType("mockPayerBlockFunds").handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            logWorkerDetails(job);
            if (amsProperties.local().enabled()) {
                Map<String, Object> variables = job.getVariablesAsMap();
                variables = setSuccessOrFailure(MockScenario.BLOCK, variables);
                variables.put(TRANSFER_CREATE_FAILED, false);
                variables.put("payeeTenantId", job.getVariablesAsMap().get("payeeTenantId"));
                variables.put(TRANSFER_CODE, "000");
                zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
                logger.info("Zeebe variable {}", job.getVariablesAsMap());
            } else {
                Map<String, Object> variables = new HashMap<>();
                variables.put(TRANSFER_PREPARE_FAILED, false);
                zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
            }
        }).name("mockPayerBlockFunds").maxJobsActive(workerMaxJobs).open();

        zeebeClient.newWorker().jobType("mockBookFunds").handler((client, job) -> {
            logWorkerDetails(job);
            if (amsProperties.local().enabled()) {
                Map<String, Object> variables = new HashMap<>();
                variables = setSuccessOrFailure(MockScenario.BOOK, variables);
                variables.put(TRANSFER_CREATE_FAILED, false);
                variables.put("payeeTenantId", job.getVariablesAsMap().get("payeeTenantId"));
                variables.put(TRANSFER_CODE, "000");
                zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
                logger.info("Zeebe variable {}", job.getVariablesAsMap());
            } else {
                Map<String, Object> variables = new HashMap<>();
                variables.put("transferCreateFailed", false);
                zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
            }
        }).name("mockBookFunds").maxJobsActive(workerMaxJobs).open();

        zeebeClient.newWorker().jobType("mockReleaseBlock").handler((client, job) -> {
            logWorkerDetails(job);
            if (amsProperties.local().enabled()) {
                Map<String, Object> variables = job.getVariablesAsMap();
                variables = setSuccessOrFailure(MockScenario.RELEASE, variables);
                variables.put(TRANSFER_CREATE_FAILED, false);
                variables.put("transferReleaseFailed", false);
                variables.put("payeeTenantId", job.getVariablesAsMap().get("payeeTenantId"));
                variables.put(TRANSFER_CODE, "000");
                zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();

                logger.info("Zeebe variable {}", job.getVariablesAsMap());
            } else {
                Map<String, Object> variables = new HashMap<>();
                variables.put("transferReleaseFailed", false);
                zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
            }
        }).name("mockReleaseBlock").maxJobsActive(workerMaxJobs).open();

        zeebeClient.newWorker().jobType("mockPayeeLookup").handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();
            client.newCompleteCommand(job.getKey()).variables(variables).send().join();
        }).name("mockPayeeLookup").maxJobsActive(workerMaxJobs).open();

        zeebeClient.newWorker().jobType("mockPayeeAccountStatus").handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();
            variables = setSuccessOrFailure(MockScenario.PAYEE_LOOKUP, variables);
            client.newCompleteCommand(job.getKey()).variables(variables).send().join();
        }).name("mockPayeeAccountStatus").maxJobsActive(workerMaxJobs).open();

        zeebeClient.newWorker().jobType("mockInitiateTransfer").handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();
            variables = setSuccessOrFailure(MockScenario.TRANSACTION, variables);
            variables.put("externalId", UUID.randomUUID());
            // added to pass the transaction request api not found error
            logger.debug("{} {}", variables.get(TRANSACTION_FAILED), variables.get(TRANSACTION_ID));

            zeebeClient.newPublishMessageCommand().messageName("mockTransferResponse")
                    .correlationKey((String) variables.get(TRANSACTION_ID)).timeToLive(Duration.ofMillis(30000)).send().join();
            client.newCompleteCommand(job.getKey()).variables(variables).send().join();
        }).name("mockInitiateTransfer").maxJobsActive(workerMaxJobs).open();

        zeebeClient.newWorker().jobType("getMockStatus").handler((client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();
            variables = setSuccessOrFailure(MockScenario.TRANSACTION, variables);
            // added to pass the transaction request api not found error
            zeebeClient.newPublishMessageCommand().messageName("mockTransferResponse")
                    .correlationKey(variables.get(TRANSACTION_ID).toString()).timeToLive(Duration.ofMillis(30000)).variables(variables)
                    .send().join();
            client.newCompleteCommand(job.getKey()).variables(variables).send().join();
        }).name("getMockStatus").maxJobsActive(workerMaxJobs).open();

        zeebeClient.newWorker().jobType("transfer-validation-ams").handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();
            variables.put(PARTY_LOOKUP_FAILED, false);
            zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
        }).name("transfer-validation-ams").maxJobsActive(workerMaxJobs).open();

        zeebeClient.newWorker().jobType("transfer-clearing-ams").handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();
            variables.put(TRANSFER_SETTLEMENT_FAILED, false);
            zeebeClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
        }).name("transfer-clearing-ams").maxJobsActive(workerMaxJobs).open();

    }

    /**
     * Rolls the dice and marks the scenario failed or not.
     *
     * <p>
     * The five branches of the old switch all did the same thing to a different variable, so the scenario now carries
     * its own variable name and the branching is gone. The odds are unchanged: a draw in 1..percentage fails, 0 and
     * anything above the percentage succeeds.
     * </p>
     */
    Map<String, Object> setSuccessOrFailure(MockScenario scenario, Map<String, Object> variables) {
        int successProbability = ThreadLocalRandom.current().nextInt(100);
        logger.info("Success probability {}", successProbability);
        boolean failed = successProbability > 0 && successProbability <= mockFailureProperties.percentage();
        variables.put(scenario.failureVariable(), failed);
        return variables;
    }

    private void logWorkerDetails(ActivatedJob job) {
        JSONObject jsonJob = new JSONObject();
        jsonJob.put("bpmnProcessId", job.getBpmnProcessId());
        jsonJob.put("elementInstanceKey", job.getElementInstanceKey());
        jsonJob.put("jobKey", job.getKey());
        jsonJob.put("jobType", job.getType());
        jsonJob.put("workflowElementId", job.getElementId());
        jsonJob.put("workflowDefinitionVersion", job.getProcessDefinitionVersion());
        jsonJob.put("workflowKey", job.getProcessDefinitionKey());
        jsonJob.put("workflowInstanceKey", job.getProcessInstanceKey());
        logger.info("Job started: {}", jsonJob.toString(4));
    }

}
