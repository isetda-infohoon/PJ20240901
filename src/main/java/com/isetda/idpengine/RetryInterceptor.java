package com.isetda.idpengine;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class RetryInterceptor implements Interceptor {
    private final int maxRetries;
    private final long delayMillis;

    public RetryInterceptor(int maxRetries, long delayMillis) {
        this.maxRetries = maxRetries;
        this.delayMillis = delayMillis;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        IOException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return chain.proceed(request);
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(delayMillis * attempt); // 점진적 지연
                    } catch (InterruptedException ignored) {}
                }
            }
        }
        throw lastException;
    }
}
