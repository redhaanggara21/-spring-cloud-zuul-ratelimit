package com.marcosbarbero.tests.it;

import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.RateLimiter;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.repository.springdata.JpaRateLimiter;
import com.marcosbarbero.tests.SpringDataApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.support.RateLimitConstants.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

/**
 * @author Marcos Barbero
 * @since 2017-06-27
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:/override-deny-request.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class SpringDataApplicationTestIT {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private RateLimiter rateLimiter;
    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Test
    public void testSpringDataRateLimiter() {
        assertTrue(rateLimiter instanceof JpaRateLimiter, "JpaRateLimiter");
    }

    @Test
    public void testKeyPrefixDefaultValue() {
        assertEquals("rate-limit-application", rateLimitProperties.getKeyPrefix());
    }

    @Test
    public void testNotExceedingCapacityRequest() {
        ResponseEntity<String> response = this.restTemplate.getForEntity("/serviceA", String.class);
        HttpHeaders headers = response.getHeaders();
        assertHeaders(headers, "rate-limit-application_serviceA_127.0.0.1", false, false);
        assertEquals(OK, response.getStatusCode());
    }

    @Test
    public void testExceedingCapacity() {
        ResponseEntity<String> response = this.restTemplate.getForEntity("/serviceB", String.class);
        HttpHeaders headers = response.getHeaders();
        String key = "rate-limit-application_serviceB_127.0.0.1";
        assertHeaders(headers, key, false, false);
        assertEquals(OK, response.getStatusCode());

        for (int i = 0; i < 2; i++) {
            response = this.restTemplate.getForEntity("/serviceB", String.class);
        }

        assertEquals(TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotEquals(SpringDataApplication.ServiceController.RESPONSE_BODY, response.getBody());

        await().pollDelay(2, TimeUnit.SECONDS).untilAsserted(() -> {
            final ResponseEntity<String> responseAfterReset = this.restTemplate
                    .getForEntity("/serviceB", String.class);
            final HttpHeaders headersAfterReset = responseAfterReset.getHeaders();
            assertHeaders(headersAfterReset, key, false, false);
            assertEquals(OK, responseAfterReset.getStatusCode());
        });
    }

    @Test
    public void testNoRateLimit() {
        ResponseEntity<String> response = this.restTemplate.getForEntity("/serviceC", String.class);
        HttpHeaders headers = response.getHeaders();
        assertHeaders(headers, "rate-limit-application_serviceC", true, false);
        assertEquals(OK, response.getStatusCode());
    }

    @Test
    public void testMultipleUrls() {
        String randomPath = UUID.randomUUID().toString();

        for (int i = 0; i < 12; i++) {

            if (i % 2 == 0) {
                randomPath = UUID.randomUUID().toString();
            }

            ResponseEntity<String> response = this.restTemplate.getForEntity("/serviceD/" + randomPath, String.class);
            HttpHeaders headers = response.getHeaders();
            assertHeaders(headers, "rate-limit-application_serviceD_serviceD_" + randomPath, false, false);
            assertEquals(OK, response.getStatusCode());
        }
    }

    @Test
    public void testExceedingQuotaCapacityRequest() {
        ResponseEntity<String> response = this.restTemplate.getForEntity("/serviceE", String.class);
        HttpHeaders headers = response.getHeaders();
        String key = "rate-limit-application_serviceE_127.0.0.1";
        assertHeaders(headers, key, false, true);
        assertEquals(OK, response.getStatusCode());

        response = this.restTemplate.getForEntity("/serviceE", String.class);
        headers = response.getHeaders();
        assertHeaders(headers, key, false, true);
        assertEquals(TOO_MANY_REQUESTS, response.getStatusCode());
    }

    @Test
    public void testUsingBreakOnMatchSpecificCase() {
        ResponseEntity<String> response = this.restTemplate.getForEntity("/serviceF", String.class);
        HttpHeaders headers = response.getHeaders();
        String key = "rate-limit-application_serviceF_127.0.0.1_127.0.0.1";
        assertHeaders(headers, key, false, false);
        assertEquals(OK, response.getStatusCode());

        response = this.restTemplate.getForEntity("/serviceF", String.class);
        headers = response.getHeaders();
        assertHeaders(headers, key, false, false);
        assertEquals(OK, response.getStatusCode());
    }

    @Test
    public void testUsingBreakOnMatchGeneralCase() {
        ResponseEntity<String> response = this.restTemplate.getForEntity("/serviceG", String.class);
        HttpHeaders headers = response.getHeaders();
        String key = "rate-limit-application_serviceG_127.0.0.1";
        assertHeaders(headers, key, false, false);
        assertEquals(OK, response.getStatusCode());

        response = this.restTemplate.getForEntity("/serviceG", String.class);
        headers = response.getHeaders();
        assertHeaders(headers, key, false, false);
        assertEquals(TOO_MANY_REQUESTS, response.getStatusCode());
    }

    @Test
    public void testUsingBreakOnMatchGeneralCaseWithCidr() {
        ResponseEntity<String> response = this.restTemplate.getForEntity("/serviceI", String.class);
        HttpHeaders headers = response.getHeaders();
        String key = "rate-limit-application_serviceI_127.0.0.1";
        assertHeaders(headers, key, false, false);
        assertEquals(OK, response.getStatusCode());

        response = this.restTemplate.getForEntity("/serviceI", String.class);
        headers = response.getHeaders();
        assertHeaders(headers, key, false, false);
        assertEquals(TOO_MANY_REQUESTS, response.getStatusCode());
    }

    private void assertHeaders(HttpHeaders headers, String key, boolean nullable, boolean quotaHeaders) {
        if (key != null && !key.startsWith("-")) {
            key = "-" + key;
        }
        String quota = headers.getFirst(HEADER_QUOTA + key);
        String remainingQuota = headers.getFirst(HEADER_REMAINING_QUOTA + key);
        String limit = headers.getFirst(HEADER_LIMIT + key);
        String remaining = headers.getFirst(HEADER_REMAINING + key);
        String reset = headers.getFirst(HEADER_RESET + key);

        if (nullable) {
            if (quotaHeaders) {
                assertNull(quota);
                assertNull(remainingQuota);
            } else {
                assertNull(limit);
                assertNull(remaining);
            }
            assertNull(reset);
        } else {
            if (quotaHeaders) {
                assertNotNull(quota);
                assertNotNull(remainingQuota);
            } else {
                assertNotNull(limit);
                assertNotNull(remaining);
            }
            assertNotNull(reset);
        }
    }
}
