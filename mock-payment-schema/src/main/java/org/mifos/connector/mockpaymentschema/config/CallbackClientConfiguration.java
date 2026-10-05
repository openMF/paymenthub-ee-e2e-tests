package org.mifos.connector.mockpaymentschema.config;

import java.security.GeneralSecurityException;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * One HTTP client for the outgoing batch authorization callbacks.
 *
 * <p>
 * The callback used to be sent through REST Assured - a test library - and a whole new client was built for every call,
 * with certificate and hostname checking switched off in the builder chain where nobody could see it. This builds the
 * client once, and the trust-all is now the {@code ams.local.server-cert-check} property, which was already in
 * application.yml doing nothing. Its default is still false, so nothing changes unless a deployment asks for it.
 * </p>
 */
@Configuration
public class CallbackClientConfiguration {

    private final Logger logger = LoggerFactory.getLogger(CallbackClientConfiguration.class);

    @Bean
    public RestClient callbackRestClient(AmsProperties amsProperties) throws GeneralSecurityException {
        if (amsProperties.local().serverCertCheck()) {
            logger.info("Callback client will verify server certificates (ams.local.server-cert-check=true)");
            return RestClient.create();
        }
        logger.warn("Callback client does NOT verify server certificates (ams.local.server-cert-check=false)");
        return RestClient.builder().requestFactory(new HttpComponentsClientHttpRequestFactory(trustAllHttpClient())).build();
    }

    @SuppressWarnings("deprecation")
    private HttpClient trustAllHttpClient() throws GeneralSecurityException {
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
        // setSSLSocketFactory is deprecated in favour of setTlsSocketStrategy, but on the httpclient5 the BOM pins the
        // socket factory does not implement the new interface, so this is still the way to do it.
        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create().setSslContext(sslContext)
                        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE).build())
                .build();
        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }
}
