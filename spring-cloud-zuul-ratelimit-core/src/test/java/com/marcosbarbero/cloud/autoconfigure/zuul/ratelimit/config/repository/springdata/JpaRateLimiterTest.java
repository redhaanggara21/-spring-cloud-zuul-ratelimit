package com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.repository.springdata;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.Rate;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.repository.BaseRateLimiterTest;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.repository.RateLimiterErrorHandler;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class JpaRateLimiterTest extends BaseRateLimiterTest {

    @Mock
    private RateLimiterErrorHandler rateLimiterErrorHandler;
    @Mock
    private RateLimiterRepository rateLimiterRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Map<String, Rate> repository = Maps.newHashMap();
        when(rateLimiterRepository.save(any(Rate.class))).thenAnswer(invocationOnMock -> {
            Rate rate = invocationOnMock.getArgument(0);
            repository.put(rate.getKey(), rate);
            return rate;
        });
        when(rateLimiterRepository.findById(any())).thenAnswer(invocationOnMock -> {
            String key = invocationOnMock.getArgument(0);
            return Optional.of(repository.get(key));
        });

        target = new JpaRateLimiter(rateLimiterErrorHandler, rateLimiterRepository);
    }
}