package org.mifos.connector.mockpaymentschema.service;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Posts the batch authorization result back to the URL the caller gave in the X-CallbackURL header. */
@Service
public class SendCallbackService {

    private final Logger logger = LoggerFactory.getLogger(SendCallbackService.class);

    private final RestClient restClient;

    public SendCallbackService(RestClient callbackRestClient) {
        this.restClient = callbackRestClient;
    }

    /**
     * Sends the callback and says what came back.
     *
     * <p>
     * The result used to be thrown away, so a callback that 404'd looked exactly like one that worked. It is inspected
     * here and logged; this method still does not throw, because the caller has already answered the client and there
     * is nothing left to fail.
     * </p>
     *
     * @return true when the callback was accepted, false when it was not sent or was refused
     */
    public boolean sendCallback(String body, String callbackUrl) {
        try {
            ResponseEntity<Void> response = restClient.post().uri(URI.create(callbackUrl)).contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().onStatus(status -> true, (request, clientResponse) -> {
                        // handled below by reading the status, instead of throwing
                    }).toBodilessEntity();
            if (response.getStatusCode().isError()) {
                logger.error("Callback to {} was refused with {}", callbackUrl, response.getStatusCode());
                return false;
            }
            logger.info("Callback to {} accepted with {}", callbackUrl, response.getStatusCode());
            return true;
        } catch (RestClientException | IllegalArgumentException e) {
            logger.error("Callback to {} could not be sent", callbackUrl, e);
            return false;
        }
    }
}
