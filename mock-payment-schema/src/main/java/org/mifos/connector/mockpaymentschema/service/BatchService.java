package org.mifos.connector.mockpaymentschema.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.mifos.connector.mockpaymentschema.config.ThresholdProperties;
import org.mifos.connector.mockpaymentschema.schema.AuthorizationRequest;
import org.mifos.connector.mockpaymentschema.schema.AuthorizationResponse;
import org.mifos.connector.mockpaymentschema.schema.BatchDTO;
import org.mifos.connector.mockpaymentschema.schema.BatchDetailResponse;
import org.mifos.connector.mockpaymentschema.schema.Transfer;
import org.mifos.connector.mockpaymentschema.schema.TransferStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class BatchService {

    private final Logger logger = LoggerFactory.getLogger(BatchService.class);

    private final ThresholdProperties thresholdProperties;

    private final SendCallbackService sendCallbackService;

    private final ObjectMapper objectMapper;

    private final String payerPartyId = "835322416";

    private final String payeePartyId = "27713803912";

    private final ConcurrentHashMap<String, BatchDTO> batchSummaryStore = new ConcurrentHashMap<>();

    public BatchService(ThresholdProperties thresholdProperties, SendCallbackService sendCallbackService, ObjectMapper objectMapper) {
        this.thresholdProperties = thresholdProperties;
        this.sendCallbackService = sendCallbackService;
        this.objectMapper = objectMapper;
    }

    public void storeBatchSummary(String batchId, BatchDTO batchDTO) {
        batchSummaryStore.put(batchId, batchDTO);
        logger.info("Stored batch summary for batchId: {}, total: {}, successful: {}, failed: {}", batchId, batchDTO.getTotal(),
                batchDTO.getSuccessful(), batchDTO.getFailed());
    }

    /**
     * Decides whether the batch is authorized and posts the answer to the callback URL.
     *
     * <p>
     * This runs on the async executor, so the caller already has its 202 by the time anything here happens. Whatever
     * goes wrong must therefore be logged with enough context to tie it back to a batch - before, an exception here
     * left no trace the caller could act on and no callback was ever sent.
     * </p>
     */
    @Async("asyncExecutor")
    public void getAuthorization(String batchId, String clientCorrelationId, AuthorizationRequest authRequest, String callbackUrl) {
        try {
            AuthorizationResponse response = new AuthorizationResponse();
            response.setClientCorrelationId(clientCorrelationId);
            if (authRequest.getAmount().compareTo(thresholdProperties.amount()) >= 0) {
                response.setStatus("N");
                response.setReason("Error getting authorization for the request");
            } else {
                response.setStatus("Y");
            }
            logger.info("Sending callback for batch {} to {}", batchId, callbackUrl);
            sendCallbackService.sendCallback(objectMapper.writeValueAsString(response), callbackUrl);
        } catch (Exception e) {
            logger.error("Authorization failed for batch {} with correlation id {}; no callback was sent to {}", batchId,
                    clientCorrelationId, callbackUrl, e);
        }
    }

    public BatchDTO getBatchSummary(String batchId) {
        BatchDTO stored = batchSummaryStore.get(batchId);
        if (stored != null) {
            logger.info("Returning stored batch summary for batchId: {}", batchId);
            return stored;
        }
        return successfulBatchSummaryResponse(batchId);
    }

    public BatchDetailResponse getBatchDetails(String batchId, int pageNo, int pageSize) {
        List<Transfer> transactions = getTransactions(batchId);
        int toIndex = pageNo * pageSize;
        int fromIndex = toIndex - pageSize;
        toIndex = Math.min(toIndex, transactions.size());
        BatchDetailResponse batchDetailResponse = new BatchDetailResponse();
        batchDetailResponse.setContent(transactions.subList(fromIndex, toIndex));
        return batchDetailResponse;
    }

    private BatchDTO successfulBatchSummaryResponse(String batchId) {
        Long total = 10L;
        Long ongoing = 1L;
        Long failed = 1L;
        Long successful = 8L;
        BigDecimal totalAmount = BigDecimal.valueOf(100);
        BigDecimal ongoingAmount = BigDecimal.valueOf(0);
        BigDecimal failedAmount = BigDecimal.valueOf(10);
        BigDecimal successfulAmount = BigDecimal.valueOf(90);
        String status = "Pending";
        String successPercentage = "90";
        String failedPercentage = "10";

        return new BatchDTO(batchId, null, total, ongoing, failed, successful, totalAmount, successfulAmount, ongoingAmount, failedAmount,
                null, null, null, status, null, null, failedPercentage, successPercentage);
    }

    private List<Transfer> getTransactions(String batchId) {
        List<Transfer> transactionList = new ArrayList<>();

        for (int index = 0; index < 10; index++) {
            transactionList.add(getSingleTransaction(index, ThreadLocalRandom.current().nextLong(), UUID.randomUUID().toString(),
                    TransferStatus.COMPLETED, batchId));
        }
        return transactionList;
    }

    private Transfer getSingleTransaction(int index, Long workflowInstanceKey, String requestId, TransferStatus status, String batchId) {
        String id = String.valueOf(index);
        Date startedAt = new Date(1685536200000L);
        Date completedAt = new Date(1685536268000L);
        String payerPartyIdType = "MSISDN";
        String payeePartyIdType = "MSISDN";
        BigDecimal amount = BigDecimal.valueOf(10);
        String currency = "USD";
        String direction = "OUTGOING";

        return new Transfer(id, workflowInstanceKey, requestId, startedAt, completedAt, status, null, null, payeePartyId, payeePartyIdType,
                null, null, null, null, payerPartyId, payerPartyIdType, null, null, null, amount, currency, direction, null, batchId, null);
    }

}
