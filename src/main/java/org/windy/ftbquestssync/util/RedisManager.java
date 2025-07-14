package org.windy.ftbquestssync.util;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class RedisManager {
    private static JedisPool pool;

    public static void init(String host, int port, String password) {
        pool = new JedisPool(new JedisPoolConfig(), host, port, 2000, password);
    }

    public static JedisPool getPool() {
        return pool;
    }

    // 发布消息到频道
    public static void publish(String channel, String message) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, message);
        }
    }

    // 订阅频道
    public static void subscribe(JedisPubSub subscriber, String channel) {
        new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(subscriber, channel);
            }
        }).start();
    }
}