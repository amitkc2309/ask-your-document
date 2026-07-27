package com.amd.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class KeycloakPolicyCacheService {

    private final StringRedisTemplate redisTemplate;
    private static final String POLICY_VERSION_KEY = "policy_version";

    private static final Duration TTL = Duration.ofMinutes(5);

    public Boolean get(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    public void put(String key, boolean allowed) {
        redisTemplate.opsForValue()
                .set(key, String.valueOf(allowed), TTL);
    }

    public void evict(String key) {
        redisTemplate.delete(key);
    }

    public String getPolicyVersion() {
        String version = redisTemplate.opsForValue().get(POLICY_VERSION_KEY);
        return version != null ? version : "1";
    }

    public void incrementPolicyVersion() {
        redisTemplate.opsForValue().increment(POLICY_VERSION_KEY);
    }

}
