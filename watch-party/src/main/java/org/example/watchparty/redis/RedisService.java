package org.example.watchparty.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ========== Basic Key-Value Operations ==========

    public void setValue(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        log.debug("Set key: {} with value: {}", key, value);
    }

    public void setValue(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
        log.debug("Set key: {} with value: {} and timeout: {} {}", key, value, timeout, unit);
    }

    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean deleteKey(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    // ========== Hash Operations (Perfect for Shared Objects) ==========

    public void setHashValue(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public Object getHashValue(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    public Map<Object, Object> getAllHashValues(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public Boolean deleteHashKey(String key, String hashKey) {
        return redisTemplate.opsForHash().delete(key, hashKey) > 0;
    }

    public Boolean hasHashKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    public Set<Object> getHashKeys(String key) {
        return redisTemplate.opsForHash().keys(key);
    }

    public Long getHashSize(String key) {
        return redisTemplate.opsForHash().size(key);
    }

    // ========== List Operations (Queues, Stacks) ==========

    public Long addToList(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    public Long addToListLeft(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    public Object getFromList(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    public List<Object> getListRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    public Object popFromList(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    public Object popFromListLeft(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    public Long getListSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // ========== Set Operations (Unique Collections) ==========

    public Long addToSet(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    public Boolean isMemberOfSet(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    public Set<Object> getSetMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public Boolean removeFromSet(String key, Object value) {
        return redisTemplate.opsForSet().remove(key, value) > 0;
    }

    public Long getSetSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    public Object popFromSet(String key) {
        return redisTemplate.opsForSet().pop(key);
    }

    // ========== Sorted Set Operations (Leaderboards, Rankings) ==========

    public Boolean addToSortedSet(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    public Set<Object> getSortedSetRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    public Set<Object> getSortedSetRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    public Long getSortedSetRank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(key, value);
    }

    public Double getSortedSetScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    public Boolean removeFromSortedSet(String key, Object value) {
        return redisTemplate.opsForZSet().remove(key, value) > 0;
    }

    public Long getSortedSetSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    // ========== Increment/Decrement Operations (Counters) ==========

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    public Double incrementFloat(String key, double delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    // ========== Pattern Matching & Bulk Operations ==========

    public Set<String> getKeysByPattern(String pattern) {
        return redisTemplate.keys(pattern);
    }

    public Long deleteKeysByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            return redisTemplate.delete(keys);
        }
        return 0L;
    }


    // ========== Pub/Sub Support ==========

    public void publish(String channel, Object message) {
        redisTemplate.convertAndSend(channel, message);
        log.info("Published message to channel: {}", channel);
    }
}