package org.mifos.connector.mockpaymentschema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mifos.connector.mockpaymentschema.config.ThresholdProperties;
import org.mifos.connector.mockpaymentschema.schema.AuthorizationRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    private static final String CALLBACK_URL = "http://localhost:9999/callback";

    @Mock
    private SendCallbackService sendCallbackService;

    /** The mapper Boot auto-configures, so the asserted bodies below are the ones that actually go on the wire. */
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    private BatchService serviceWithThreshold(String threshold) {
        return new BatchService(new ThresholdProperties(new BigDecimal(threshold)), sendCallbackService, objectMapper);
    }

    private AuthorizationRequest request(String amount) {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setAmount(new BigDecimal(amount));
        request.setCurrency("USD");
        return request;
    }

    private String callbackBody() {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sendCallbackService).sendCallback(body.capture(), eq(CALLBACK_URL));
        return body.getValue();
    }

    @Test
    @DisplayName("an amount below the threshold is authorized")
    void amountBelowThresholdIsAuthorized() {
        serviceWithThreshold("20000").getAuthorization("batch-1", "corr-1", request("100"), CALLBACK_URL);

        assertThat(callbackBody()).isEqualTo("{\"clientCorrelationId\":\"corr-1\",\"status\":\"Y\",\"reason\":null}");
    }

    @Test
    @DisplayName("an amount at the threshold is refused, with the reason the callers already parse")
    void amountAtThresholdIsRefused() {
        serviceWithThreshold("20000").getAuthorization("batch-1", "corr-1", request("20000"), CALLBACK_URL);

        assertThat(callbackBody()).isEqualTo(
                "{\"clientCorrelationId\":\"corr-1\",\"status\":\"N\",\"reason\":\"Error getting authorization for the request\"}");
    }

    @Test
    @DisplayName("a threshold with decimals is compared, not rejected")
    void decimalThresholdIsHonoured() {
        // this is the case that used to throw NumberFormatException on every request, after the caller had its 202
        serviceWithThreshold("20000.50").getAuthorization("batch-1", "corr-1", request("20000.40"), CALLBACK_URL);

        assertThat(callbackBody()).contains("\"status\":\"Y\"");
    }

    @Test
    @DisplayName("a request with no amount does not escape the async method, and sends no callback")
    void missingAmountIsLoggedAndNoCallbackIsSent() {
        BatchService service = serviceWithThreshold("20000");
        AuthorizationRequest withoutAmount = new AuthorizationRequest();

        assertThatCode(() -> service.getAuthorization("batch-1", "corr-1", withoutAmount, CALLBACK_URL)).doesNotThrowAnyException();

        verify(sendCallbackService, never()).sendCallback(anyString(), anyString());
    }
}
