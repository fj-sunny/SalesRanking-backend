package com.jing.salesrankingbackend;

import com.jing.salesrankingbackend.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@IntegrationTest
class RedisConnectivityTest {

    private static final String TEST_KEY = "connectivity:test";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldConnectToRedis() {
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        assertNotNull(connectionFactory, "RedisConnectionFactory 未注入");

        try (RedisConnection connection = connectionFactory.getConnection()) {
            assertEquals("PONG", connection.ping(), "Redis PING 未返回 PONG");
        }

        redisTemplate.opsForValue().set(TEST_KEY, "ok");
        assertEquals("ok", redisTemplate.opsForValue().get(TEST_KEY), "Redis 读写校验失败");
        redisTemplate.delete(TEST_KEY);
    }
}
