package com.fitops.commons.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class CacheConfigTest {
  // A minimal context holding ONLY CacheConfig + a test bean. No DB, no web, no full app.
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(CacheConfiguration.class, CachingTestConfig.class)
          .withPropertyValues("fitops.cache.default-ttl=10m", "fitops.cache.default-max-size=500");

  @Test
  void caches_secondCall_withSameKey() {
    contextRunner.run(
        context -> {
          CountingFoodService service = context.getBean(CountingFoodService.class);
          String first = service.load("apple");
          String second = service.load("apple");
          assertThat(first).isEqualTo(second);
          assertThat(service.callCount()).isEqualTo(1);
        });
  }

  @Test
  void differentKeys_executeSeparately() {
    contextRunner.run(
        context -> {
          CountingFoodService service = context.getBean(CountingFoodService.class);
          service.load("apple");
          service.load("banana");
          assertThat(service.callCount()).isEqualTo(2);
        });
  }

  @Test
  void cacheManager_isCaffeine() {
    contextRunner.run(
        context ->
            assertThat(context.getBean(CacheManager.class))
                .isInstanceOf(CaffeineCacheManager.class));
  }

  @Test
  void resolves_unknownCacheName_Dynamically() {
    contextRunner.run(
        context -> {
          CacheManager cacheManager = context.getBean(CacheManager.class);
          assertThat(cacheManager.getCache("foods")).isNotNull();
          assertThat(cacheManager.getCache("never-declared-anywhere")).isNotNull();
        });
  }

  @Test
  void reject_nonPositive_maxSize() {
    new ApplicationContextRunner()
        .withUserConfiguration(CacheConfiguration.class)
        .withPropertyValues("fitops.cache.default-ttl=10m", "fitops.cache.default-max-size=-1")
        .run((AssertableApplicationContext context) -> assertThat(context).hasFailed());
  }

  @Test
  void recordsStats_whenEnabled() {
    contextRunner.run(
        context -> {
          CountingFoodService service = context.getBean(CountingFoodService.class);
          service.load("apple"); // miss → method runs, value cached
          service.load("apple"); // hit  → served from cache

          // The "foods" cache is created lazily on first @Cacheable call above.
          CaffeineCache foodsCache =
              (CaffeineCache) context.getBean(CacheManager.class).getCache("foods");
          assertThat(foodsCache).isNotNull();

          // stats() only counts if recordStats() was configured on the spec.
          CacheStats stats = foodsCache.getNativeCache().stats();
          assertThat(stats.requestCount()).isEqualTo(2);
          assertThat(stats.hitCount()).isEqualTo(1);
          assertThat(stats.missCount()).isEqualTo(1);
        });
  }

  // test fixture
  @Configuration
  static class CachingTestConfig {
    @Bean
    CountingFoodService countingFoodService() {
      return new CountingFoodService();
    }
  }

  // Stands in for a Phase-2 food service. Counts real method-body executions
  // so we can prove the cache short-circuits the 2nd call.
  static class CountingFoodService {
    private final AtomicInteger callCount = new AtomicInteger();

    @Cacheable("foods")
    public String load(String key) {
      callCount.incrementAndGet();
      return "nutrition:" + key;
    }

    int callCount() {
      return callCount.get();
    }
  }
}
