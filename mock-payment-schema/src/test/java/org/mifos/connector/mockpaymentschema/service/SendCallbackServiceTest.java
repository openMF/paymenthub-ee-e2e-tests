package org.mifos.connector.mockpaymentschema.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * The callback result used to be thrown away, so a callback that was refused looked exactly like one that worked. These
 * tests drive a real HTTP server so the return value means something.
 */
class SendCallbackServiceTest {

    private HttpServer server;

    private final List<String> received = new CopyOnWriteArrayList<>();

    private int status = 200;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (InputStream body = exchange.getRequestBody()) {
            received.add(new String(body.readAllBytes(), StandardCharsets.UTF_8));
        }
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/callback";
    }

    private SendCallbackService service() {
        return new SendCallbackService(RestClient.create());
    }

    @Test
    @DisplayName("a callback that is accepted is reported as sent, with the body intact")
    void acceptedCallbackIsReportedAsSent() {
        status = 202;

        assertThat(service().sendCallback("{\"status\":\"Y\"}", url())).isTrue();
        assertThat(received).containsExactly("{\"status\":\"Y\"}");
    }

    @Test
    @DisplayName("a callback the target refuses with 500 is reported as not sent")
    void refusedCallbackIsReportedAsNotSent() {
        status = 500;

        assertThat(service().sendCallback("{\"status\":\"Y\"}", url())).isFalse();
        // the request did reach the target - it is the answer that says no
        assertThat(received).hasSize(1);
    }

    @Test
    @DisplayName("a callback to a URL nothing is listening on is reported as not sent, and does not throw")
    void unreachableCallbackIsReportedAsNotSent() {
        int deadPort = server.getAddress().getPort();
        server.stop(0);

        assertThat(service().sendCallback("{\"status\":\"Y\"}", "http://127.0.0.1:" + deadPort + "/callback")).isFalse();
    }

    @Test
    @DisplayName("a callback URL that is not a URL at all is reported as not sent, and does not throw")
    void malformedCallbackUrlIsReportedAsNotSent() {
        assertThat(service().sendCallback("{\"status\":\"Y\"}", "not a url")).isFalse();
    }
}
