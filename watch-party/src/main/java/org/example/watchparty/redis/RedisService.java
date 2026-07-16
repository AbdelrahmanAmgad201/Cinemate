package org.example.watchparty.redis;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Atomically checks membership, adds to the set, and increments the participant
    // count in one round-trip (REL-NEW-01) — doing these as separate calls lets two
    // concurrent joins both pass the "not already a member" check and both increment,
    // overcounting a party that actually has one more member than it reports.
    private static final RedisScript<Long> JOIN_SET_AND_INCREMENT_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then\n" +
            "  return 0\n" +
            "else\n" +
            "  redis.call('SADD', KEYS[1], ARGV[1])\n" +
            "  redis.call('HINCRBY', KEYS[2], ARGV[2], 1)\n" +
            "  return 1\n" +
            "end",
            Long.class
    );

    /**
     * Adds {@code member} to the set at {@code setKey} and increments {@code hashField}
     * in the hash at {@code hashKey} atomically, but only if {@code member} wasn't
     * already present. Returns {@code true} if the member was newly added, {@code false}
     * if it was already a member (no-op).
     */
    public boolean addToSetAndIncrementHash(String setKey, String hashKey, String hashField, Object member) {
        Long result = redisTemplate.execute(JOIN_SET_AND_INCREMENT_SCRIPT,
                List.of(setKey, hashKey), member, hashField);
        return result != null && result == 1L;
    }

    // The mirror of the join script for the leave path (REL-NEW-03): atomically checks
    // membership, removes from the set, and decrements the participant count in one
    // round-trip. Doing these as separate calls (SISMEMBER → SREM → HINCRBY) lets a
    // check-then-act race double-decrement the counter or drift it out of step with the
    // member set — and since a party self-destructs when the count reaches 0, a miscount
    // can prematurely tear down a live party or strand an empty one. Returns -1 (not a
    // valid count: the set/count are kept in lockstep, so a present member always implies
    // count ≥ 1) when the user wasn't a member, so the caller can distinguish a no-op.
    private static final RedisScript<Long> LEAVE_SET_AND_DECREMENT_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 0 then\n" +
            "  return -1\n" +
            "end\n" +
            "redis.call('SREM', KEYS[1], ARGV[1])\n" +
            "return redis.call('HINCRBY', KEYS[2], ARGV[2], -1)",
            Long.class
    );

    /**
     * Removes {@code member} from the set at {@code setKey} and decrements {@code hashField}
     * in the hash at {@code hashKey} atomically, but only if {@code member} was present.
     * Returns the new participant count (≥ 0), or {@code -1} if the user was not a member
     * (no-op).
     */
    public long removeFromSetAndDecrementHash(String setKey, String hashKey, String hashField, Object member) {
        Long result = redisTemplate.execute(LEAVE_SET_AND_DECREMENT_SCRIPT,
                List.of(setKey, hashKey), member, hashField);
        return result != null ? result : -1;
    }

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

    // Fetches multiple keys in a single round-trip instead of one GET per key (PERF-01) —
    // used by getPartyMembers(), which previously issued one Redis call per party member.
    public List<Object> multiGet(Collection<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
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

    // ========== Hash Operations ==========

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

    public Long incrementHashValue(String key, String hashKey) {
        return redisTemplate.opsForHash().increment(key, hashKey, 1);
    }

    public Long decrementHashValue(String key, String hashKey) {
        return redisTemplate.opsForHash().increment(key, hashKey, -1);
    }

    // ========== List Operations ==========

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

    // ========== Set Operations ==========

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

    // ========== Sorted Set Operations ==========

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

    // ========== Increment/Decrement Operations ==========

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
        return scanKeys(pattern);
    }

    public Long deleteKeysByPattern(String pattern) {
        Set<String> keys = scanKeys(pattern);
        if (!keys.isEmpty()) {
            return redisTemplate.delete(keys);
        }
        return 0L;
    }

    // SCAN instead of KEYS (PERF-05) — KEYS blocks the single-threaded Redis server for
    // the full O(N) keyspace scan; SCAN walks it incrementally in small batches instead.
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                connection -> connection.scan(options))) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        }
        return keys;
    }

    // ========== Pub/Sub Support ==========

    public void publish(String channel, Object message) {
        redisTemplate.convertAndSend(channel, message);
        log.info("Published message to channel: {}", channel);
    }
}