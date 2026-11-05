package com.ai.exam_spring.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * 基于 StringRedisTemplate + Jackson 封装，支持存入任意 Java 对象
 */
@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // ============================= Common (通用操作) =============================

    /**
     * 指定缓存失效时间
     * @param key 键
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean expire(String key, long time) {
        return stringRedisTemplate.expire(key, time, TimeUnit.SECONDS);
    }

    /**
     * 判断 key 是否存在
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 删除缓存
     * @param key 可以传一个值 或多个
     */
    public void delete(String... key) {
        if (key != null && key.length > 0) {
            stringRedisTemplate.delete((Collection<String>) java.util.Arrays.asList(key));
        }
    }

    /** 按前缀批量删除缓存 */
    public void deleteByPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return;
        }
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    // ============================= String (字符串操作) =============================

    /**
     * 普通缓存获取 (返回字符串)
     * @param key 键
     * @return 值
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存获取 (将 JSON 反序列化为对象)
     * @param key 键
     * @param clazz 目标对象的 Class 类型
     * @return 反序列化后的对象
     */
    public <T> T get(String key, Class<T> clazz) {
        String jsonValue = stringRedisTemplate.opsForValue().get(key);
        if (jsonValue == null) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonValue, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("从Redis读取对象失败: " + e.getMessage(), e);
        }
    }

    /**
     * 普通缓存放入 (存入字符串)
     * @param key 键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
        return true;
    }

    /**
     * 普通缓存放入 (将对象序列化为 JSON 存入)
     * @param key 键
     * @param value 任意 Java 对象
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, jsonValue);
            return true;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象转JSON失败: " + e.getMessage(), e);
        }
    }

    /**
     * 普通缓存放入并设置时间 (存入字符串)
     * @param key 键
     * @param value 值
     * @param time 时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, String value, long time) {
        if (time > 0) {
            stringRedisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
        } else {
            set(key, value);
        }
        return true;
    }

    /**
     * 普通缓存放入并设置时间 (将对象序列化为 JSON 存入)
     * @param key 键
     * @param value 任意 Java 对象
     * @param time 时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            if (time > 0) {
                stringRedisTemplate.opsForValue().set(key, jsonValue, time, TimeUnit.SECONDS);
            } else {
                stringRedisTemplate.opsForValue().set(key, jsonValue);
            }
            return true;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象转JSON失败: " + e.getMessage(), e);
        }
    }

    /**
     * 仅当 key 不存在时写入对象并设置过期时间
     * @param key 键
     * @param value 任意 Java 对象
     * @param time 时间(秒)
     * @return true 写入成功 false key 已存在
     */
    public boolean setIfAbsent(String key, Object value, long time) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            if (time > 0) {
                return Boolean.TRUE.equals(
                        stringRedisTemplate.opsForValue().setIfAbsent(key, jsonValue, time, TimeUnit.SECONDS));
            }
            return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, jsonValue));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象转JSON失败: " + e.getMessage(), e);
        }
    }

    // ============================= Hash (哈希操作) =============================

    /**
     * HashGet 获取hash表中指定字段的值
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return 值 (字符串形式)
     */
    public String hGet(String key, String item) {
        Object value = stringRedisTemplate.opsForHash().get(key, item);
        return value == null ? null : value.toString();
    }

    /**
     * HashGet 获取hash表中指定字段的值 (反序列化为对象)
     * @param key 键
     * @param item 项
     * @param clazz 目标对象的 Class 类型
     * @return 反序列化后的对象
     */
    public <T> T hGet(String key, String item, Class<T> clazz) {
        Object value = stringRedisTemplate.opsForHash().get(key, item);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value.toString(), clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("从Redis Hash读取对象失败: " + e.getMessage(), e);
        }
    }


    /**
     * HashSet 向一张hash表中放入数据 (自动将对象值序列化为JSON)
     * @param key 键
     * @param map 对应多个键值 (值可以是任意对象)
     * @return true 成功 false 失败
     */
    public boolean hSetAll(String key, Map<String, Object> map) {
        try {
            Map<String, String> jsonMap = new java.util.HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                jsonMap.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
            }
            stringRedisTemplate.opsForHash().putAll(key, jsonMap);
            return true;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Hash对象转JSON失败: " + e.getMessage(), e);
        }
    }

    /**
     * HashSet 向一张hash表中放入数据并设置过期时间 (自动将对象值序列化为JSON)
     * @param key 键
     * @param map 对应多个键值 (值可以是任意对象)
     * @param expireSeconds 过期时间(秒) 小于等于0表示不设置过期时间
     * @return true 成功 false 失败
     */
    public boolean hSetAll(String key, Map<String, Object> map, long expireSeconds) {
        hSetAll(key, map);
        if (expireSeconds > 0) {
            expire(key, expireSeconds);
        }
        return true;
    }

    /**
     * HashGetAll 获取hash表中所有字段和值 (反序列化为对象)
     * @param key 键
     * @param clazz 目标对象的 Class 类型
     * @return 反序列化后的对象Map (field -> object)
     */
    public <T> Map<String, T> hGetAll(String key, Class<T> clazz) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        Map<String, T> result = new java.util.HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                T value = objectMapper.readValue(entry.getValue().toString(), clazz);
                result.put(entry.getKey().toString(), value);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("从Redis Hash读取对象失败: " + e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * HashGetAll 轻量版 - 只返回原始 JSON 字符串，不做反序列化
     * 适用于只需要提取部分字段的场景，避免 N 次 JSON 解析开销
     * @param key 键
     * @return 原始 JSON 字符串 Map (field -> json string)
     */
    public Map<String, String> hGetAllRaw(String key) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        Map<String, String> result = new java.util.HashMap<>(entries.size());
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return result;
    }

    // ============================= List (列表操作) =============================

    /**
     * ListGet 获取list缓存的内容
     * @param key 键
     * @param start 开始
     * @param end 结束 0 到 -1代表所有值
     * @return 缓存内容 (字符串列表)
     */
    public List<String> lRange(String key, long start, long end) {
        return stringRedisTemplate.opsForList().range(key, start, end);
    }

    /**
     * ListPush 将list放入缓存(右侧追加)
     * @param key 键
     * @param value 值 (字符串)
     * @return 成功条数
     */
    public long lRightPush(String key, String value) {
        return stringRedisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * ListPush 将对象序列化为JSON后放入list缓存(右侧追加)
     * @param key 键
     * @param value 任意 Java 对象
     * @return 成功条数
     */
    public long lRightPush(String key, Object value) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            return stringRedisTemplate.opsForList().rightPush(key, jsonValue);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("List对象转JSON失败: " + e.getMessage(), e);
        }
    }

    // ============================= Set (集合操作) =============================

    /**
     * SetAdd 将数据放入set缓存
     * @param key 键
     * @param values 值 可以是多个 (字符串)
     * @return 成功个数
     */
    public long sAdd(String key, String... values) {
        return stringRedisTemplate.opsForSet().add(key, values);
    }

    /**
     * SetAdd 将对象序列化为JSON后放入set缓存
     * @param key 键
     * @param values 任意 Java 对象
     * @return 成功个数
     */
    public long sAdd(String key, Object... values) {
        try {
            String[] jsonValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                jsonValues[i] = objectMapper.writeValueAsString(values[i]);
            }
            return stringRedisTemplate.opsForSet().add(key, jsonValues);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Set对象转JSON失败: " + e.getMessage(), e);
        }
    }

    /**
     * SetMembers 获取set缓存所有的成员
     * @param key 键
     * @return 成员集合 (字符串集合)
     */
    public Set<String> sMembers(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }

    // ============================= Lock (简易分布式锁) =============================

    /**
     * 尝试获取分布式锁 (利用 setIfAbsent 原子操作)
     * @param key 锁的键
     * @param value 锁的值 (建议传 UUID，防止误删别人的锁)
     * @param timeout 锁的过期时间(秒)，防止死锁
     * @return true 获取成功 false 获取失败
     */
    public boolean tryLock(String key, String value, long timeout) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS)
        );
    }

    /**
     * 释放分布式锁 (释放前必须校验value，防止误删)
     * @param key 锁的键
     * @param value 锁的值
     */
    public void unlock(String key, String value) {
        String currentValue = stringRedisTemplate.opsForValue().get(key);
        if (value.equals(currentValue)) {
            stringRedisTemplate.delete(key);
        }
    }
}
