package org.windy.ftbquestssync.data;

import org.windy.ftbquestssync.util.RedisManager;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RedisPlayerDataService implements PlayerDataService {
    private final boolean debugMode;

    public RedisPlayerDataService(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public List<String> getCompletedQuestIds(UUID playerUuid, String playerName) {
        List<String> result = new ArrayList<>();
        String key = "player:" + playerUuid + ":data";
        try (Jedis jedis = RedisManager.getPool().getResource()) {
            List<String> ids = jedis.lrange(key, 0, -1);
            if (debugMode) {
                System.out.println("Redis拉取 " + playerName + " 完成任务: " + ids);
            }
            result.addAll(ids);
        }
        return result;
    }

    // 发布玩家完成任务到Redis并广播
    public void publishPlayerData(UUID playerUuid, List<String> questIds) {
        String key = "player:" + playerUuid + ":data";
        try (Jedis jedis = RedisManager.getPool().getResource()) {
            jedis.del(key);
            if (!questIds.isEmpty()) {
                jedis.rpush(key, questIds.toArray(new String[0]));
            }
            // 广播消息
            RedisManager.publish("ftbquests_sync", playerUuid.toString());
            if (debugMode) {
                System.out.println("Redis发布 " + playerUuid + " 完成任务同步，广播到频道");
            }
        }
    }
}