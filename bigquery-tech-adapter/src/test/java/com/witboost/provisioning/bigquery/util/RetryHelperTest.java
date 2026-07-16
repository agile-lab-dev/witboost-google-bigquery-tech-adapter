package com.witboost.provisioning.bigquery.util;

import static org.junit.jupiter.api.Assertions.*;

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.AbortedException;
import com.witboost.provisioning.bigquery.config.RetryConfig;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetryHelperTest {

    private AbortedException newAbortedException() {
        return new AbortedException(
                new StatusRuntimeException(Status.ABORTED), GrpcStatusCode.of(Status.Code.ABORTED), true);
    }

    @Test
    void returnsImmediatelyOnSuccess() throws Exception {
        var config = new RetryConfig(100, 200, 1000);
        var helper = new RetryHelper(config);

        String result = helper.retryOnAbortedException(() -> "ok");

        assertEquals("ok", result);
    }

    @Test
    void retriesOnAbortedExceptionThenSucceeds() throws Exception {
        var config = new RetryConfig(10, 50, 5000);
        var helper = new RetryHelper(config);
        var counter = new AtomicInteger(0);

        String result = helper.retryOnAbortedException(() -> {
            if (counter.incrementAndGet() < 3) {
                throw newAbortedException();
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(3, counter.get());
    }

    @Test
    void throwsAbortedExceptionWhenMaxTotalWaitExceeded() {
        // Very short maxTotalWaitMs so it gives up quickly
        var config = new RetryConfig(10, 20, 1);
        var helper = new RetryHelper(config);

        assertThrows(
                AbortedException.class,
                () -> helper.retryOnAbortedException(() -> {
                    throw newAbortedException();
                }));
    }

    @Test
    void propagatesNonAbortedExceptionImmediately() {
        var config = new RetryConfig(10, 50, 5000);
        var helper = new RetryHelper(config);
        var counter = new AtomicInteger(0);

        var thrown = assertThrows(
                IllegalStateException.class,
                () -> helper.retryOnAbortedException(() -> {
                    counter.incrementAndGet();
                    throw new IllegalStateException("not retryable");
                }));

        assertEquals("not retryable", thrown.getMessage());
        assertEquals(1, counter.get());
    }

    @Test
    void returnsNullWhenCallableReturnsNull() throws Exception {
        var config = new RetryConfig(10, 50, 5000);
        var helper = new RetryHelper(config);

        Void result = helper.retryOnAbortedException(() -> null);

        assertNull(result);
    }
}
