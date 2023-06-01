/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.repository;

import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.Rate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Marcos Barbero
 * @author Liel Chayoun
 */
public class RedisRateLimiter extends AbstractNonBlockCacheRateLimiter {

    private final RateLimiterErrorHandler rateLimiterErrorHandler;
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> redisScript;

    public RedisRateLimiter(final RateLimiterErrorHandler rateLimiterErrorHandler,
                            final StringRedisTemplate redisTemplate) {
        this.rateLimiterErrorHandler = rateLimiterErrorHandler;
        this.redisTemplate = redisTemplate;
        this.redisScript = getScript();
    }

    @Override
    protected void calcRemainingLimit(final Long limit, final Duration refreshInterval,
                                      final Long requestTime, final String key, final Rate rate) {
        if (Objects.nonNull(limit)) {
            long usage = requestTime == null ? 1L : 0L;
            Long remaining = calcRemaining(limit, refreshInterval, usage, key, rate);
            rate.setRemaining(remaining);
        }
    }

    @Override
    protected void calcRemainingQuota(final Long quota, final Duration refreshInterval,
                                      final Long requestTime, final String key, final Rate rate) {
        if (Objects.nonNull(quota)) {
            String quotaKey = key + QUOTA_SUFFIX;
            long usage = requestTime != null ? requestTime : 0L;
            Long remaining = calcRemaining(quota, refreshInterval, usage, quotaKey, rate);
            rate.setRemainingQuota(remaining);
        }
    }

    private Long calcRemaining(Long limit, Duration refreshInterval, long usage, String key, Rate rate) {
        rate.setReset(refreshInterval.toMillis());
        Long current = 0L;
        try {
            current = redisTemplate.execute(redisScript, Collections.singletonList(key), Long.toString(usage),
                    Long.toString(refreshInterval.getSeconds()));
        } catch (RuntimeException e) {
            String msg = "Failed retrieving rate for " + key + ", will return the current value";
            rateLimiterErrorHandler.handleError(msg, e);
        }
        return Math.max(-1, limit - (current != null ? current.intValue() : 0));
    }

    private RedisScript<Long> getScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("/scripts/ratelimit.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}
