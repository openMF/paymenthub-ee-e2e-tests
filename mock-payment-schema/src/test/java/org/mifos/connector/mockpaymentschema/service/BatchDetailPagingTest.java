package org.mifos.connector.mockpaymentschema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.mockpaymentschema.api.BatchApi;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * GET /batches/{id}/detail documented pageNo with a default of 0 while the arithmetic counted pages from 1, so the
 * endpoint's own default was subList(-10, 0) and a 500. Pages now count from 1 everywhere, as connector-bulk sends
 * them.
 */
class BatchDetailPagingTest {

    private final BatchService service = new BatchService(null, null, null);

    private BatchApi api() {
        BatchApi api = new BatchApi();
        ReflectionTestUtils.setField(api, "batchService", service);
        return api;
    }

    @Test
    @DisplayName("page 1 is the first page, and the default")
    void firstPage() {
        assertThat(service.getBatchDetails("B1", 1, 10).getContent()).hasSize(10);
        assertThat(api().batchDetail("B1", 1, 10).getContent()).hasSize(10);
    }

    @Test
    @DisplayName("a partial last page and a page past the end are answered, not a 500")
    void pagesNearTheEnd() {
        assertThat(service.getBatchDetails("B1", 2, 7).getContent()).hasSize(3);
        assertThat(service.getBatchDetails("B1", 3, 7).getContent()).isEmpty();
        assertThat(service.getBatchDetails("B1", Integer.MAX_VALUE, Integer.MAX_VALUE).getContent()).isEmpty();
    }

    @Test
    @DisplayName("page 0, or a page size of 0, is a 400")
    void zeroIsABadRequest() {
        assertThatThrownBy(() -> api().batchDetail("B1", 0, 10)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> api().batchDetail("B1", 1, 0)).isInstanceOf(ResponseStatusException.class);
    }
}
