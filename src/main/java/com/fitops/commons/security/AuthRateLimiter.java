package com.fitops.commons.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {
  private final Cache<String, Bucket> buckets;
  private final Bandwidth limit;

  public AuthRateLimiter(RateLimitProperties props) {
    this.buckets =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();
    this.limit =
        Bandwidth.builder()
            .capacity(props.capacity())
            .refillGreedy(props.capacity(), props.refillPeriod())
            .build();
  }

  public ConsumptionProbe tryConsume(String key) {
    var bucket = buckets.get(key, k -> Bucket.builder().addLimit(limit).build());
    return bucket.tryConsumeAndReturnRemaining(1);
  }
}
