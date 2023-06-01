package com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties.Policy.MatchType;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StringToMatchTypeConverterTest {

    private StringToMatchTypeConverter target;

    @BeforeEach
    public void setUp() {
        target = new StringToMatchTypeConverter();
    }

    @Test
    public void testConvertStringTypeOnly() {
        MatchType matchType = target.convert("url");
        assertThat(matchType).isNotNull();
        assertThat(matchType.getType()).isEqualByComparingTo(RateLimitType.URL);
        assertThat(matchType.getMatcher()).isNull();
    }

    @Test
    public void testConvertStringTypeUrlPatternWithMatcher() {
        MatchType matchType = target.convert("url_pattern=/api/*/specific");
        assertThat(matchType).isNotNull();
        assertThat(matchType.getType()).isEqualByComparingTo(RateLimitType.URL_PATTERN);
        assertThat(matchType.getMatcher()).isEqualTo("/api/*/specific");
    }

    @Test
    public void testConvertStringTypeWithMatcher() {
        MatchType matchType = target.convert("url=/api");
        assertThat(matchType).isNotNull();
        assertThat(matchType.getType()).isEqualByComparingTo(RateLimitType.URL);
        assertThat(matchType.getMatcher()).isEqualTo("/api");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testConvertStringTypeMethodOnly() {
        MatchType matchType = target.convert("httpmethod");
        assertThat(matchType).isNotNull();
        assertThat(matchType.getType()).isEqualByComparingTo(RateLimitType.HTTPMETHOD);
        assertThat(matchType.getMatcher()).isNull();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testConvertStringTypeMethodWithMatcher() {
        MatchType matchType = target.convert("httpmethod=get");
        assertThat(matchType).isNotNull();
        assertThat(matchType.getType()).isEqualByComparingTo(RateLimitType.HTTPMETHOD);
        assertThat(matchType.getMatcher()).isEqualTo("get");
    }

    @Test
    public void testConvertStringTypeHttpMethodOnly() {
        MatchType matchType = target.convert("http_method");
        assertThat(matchType).isNotNull();
        assertThat(matchType.getType()).isEqualByComparingTo(RateLimitType.HTTP_METHOD);
        assertThat(matchType.getMatcher()).isNull();
    }

    @Test
    public void testConvertStringTypeHttpMethodWithMatcher() {
        MatchType matchType = target.convert("http_method=get");
        assertThat(matchType).isNotNull();
        assertThat(matchType.getType()).isEqualByComparingTo(RateLimitType.HTTP_METHOD);
        assertThat(matchType.getMatcher()).isEqualTo("get");
    }

    @Test
    public void testConvertStringTypeHttpHeaderWithMatcher() {
        MatchType matchType = target.convert("http_header=customHeader");
        assertThat(matchType).isNotNull();
        assertThat(matchType.getType()).isEqualByComparingTo(RateLimitType.HTTP_HEADER);
        assertThat(matchType.getMatcher()).isEqualTo("customHeader");
    }
}