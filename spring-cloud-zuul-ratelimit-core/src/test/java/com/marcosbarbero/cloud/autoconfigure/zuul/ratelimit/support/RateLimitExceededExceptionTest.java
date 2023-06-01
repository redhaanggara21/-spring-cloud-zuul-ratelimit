package com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.CounterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

public class RateLimitExceededExceptionTest {

    private RateLimitExceededException target;

    @BeforeEach
    public void setUp() {
        CounterFactory counterFactory = mock(CounterFactory.class);
        MockitoAnnotations.initMocks(this);
        CounterFactory.initialize(counterFactory);
        target = new RateLimitExceededException();
    }

    @Test
    public void testExceptionInfo() {
        Throwable cause = target.getCause();
        assertThat(cause).isInstanceOf(ZuulException.class);

        ZuulException zuulException = (ZuulException) cause;
        assertThat(zuulException.getMessage()).contains("429");
    }
}