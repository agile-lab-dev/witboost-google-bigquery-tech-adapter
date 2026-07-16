package com.witboost.provisioning.bigquery.util;

import com.google.api.gax.rpc.AbortedException;
import com.witboost.provisioning.bigquery.config.RetryConfig;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RetryHelper {
    private final RetryConfig config;

    public RetryHelper(RetryConfig config) {
        this.config = config;
    }

    public <T> T retryOnAbortedException(Callable<T> fn) throws Exception {
        int attempt = 0;
        long totalWaited = 0L;
        while (true) {
            try {
                return fn.call();
            } catch (AbortedException e) {
                attempt++;
                log.info("Retrying to call function with attempt {}", attempt);
                long exp = Math.min(config.initialBackoffMs() * (1L << (attempt - 1)), config.maxBackoffMs());
                long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, exp / 4));
                long sleepMs = Math.min(exp + jitter, config.maxBackoffMs());
                if (totalWaited + sleepMs > config.maxTotalWaitMs()) {
                    throw e;
                }
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
                totalWaited += sleepMs;
            }
        }
    }
}
