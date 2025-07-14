package org.windy.ftbquestssync;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.windy.ftbquestssync.data.*;
import org.windy.ftbquestssync.log.LogFilter;
import org.windy.ftbquestssync.util.RedisManager;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public final class FTBQuestsSync extends JavaPlugin implements Listener {
    private PlayerDataService playerDataService;
    public boolean debugMode;
    public String mode;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        FileConfiguration config = getConfig();
        debugMode = config.getBoolean("Debug");

        mode = config.getString("mode", "file");
        if (mode.equalsIgnoreCase("redis")) {
            String host = config.getString("redis.host", "localhost");
            int port = config.getInt("redis.port", 6379);
            String password = config.getString("redis.password", ""); // 为空也没关系
            RedisManager.init(host, port, password); // 关键1：先初始化
            playerDataService = new RedisPlayerDataService(debugMode); // 关键2：初始化服务
            RedisManager.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    try {
                        UUID uuid = UUID.fromString(message);
                        String playerName = null;
                        // 尝试获取玩家名
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            playerName = player.getName();
                        } else {
                            // 离线玩家，也可以尝试这样获得名字
                            playerName = Bukkit.getOfflinePlayer(uuid).getName();
                        }
                        if (playerName != null && !playerName.isEmpty()) {
                            // 一定要在主线程执行sync
                            String finalPlayerName = playerName;
                            Bukkit.getScheduler().runTask(FTBQuestsSync.this, () -> {
                                sync(uuid, finalPlayerName);
                            });
                            if (debugMode) {
                                getLogger().info("收到Redis广播，自动同步玩家 " + playerName + "（" + uuid + "） 的进度到本服");
                            }
                        } else {
                            if (debugMode) {
                                getLogger().warning("收到Redis消息但未找到玩家名: " + uuid);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, "ftbquests_sync");
        } else {
            playerDataService = new FilePlayerDataService(this, debugMode);
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        this.getServer().getConsoleSender().sendMessage(Texts.bless);
        this.getServer().getConsoleSender().sendMessage(Texts.logo);

        // 日志过滤
        Logger logger = getServer().getLogger();
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFilter(new LogFilter());
        logger.addHandler(consoleHandler);
    }

    @Override
    public void onDisable() {
        this.getServer().getConsoleSender().sendMessage(Texts.bless);
        this.getServer().getConsoleSender().sendMessage(Texts.logo);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        if(mode.equalsIgnoreCase("redis")){
            return;
        }
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (debugMode){
            this.getLogger().info("开始获取玩家" + player.getName() +"("+uuid+")的基本信息");
        }
        sync(uuid, player.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!mode.equalsIgnoreCase("redis")) {
            return;
        }
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        if (debugMode){
            this.getLogger().info("开始同步玩家" + playerName + "（" + uuid + "）的进度到Redis");
        }

        // 提取完成任务id（用文件版实现，确保最新）
        FilePlayerDataService fileDataService = new FilePlayerDataService(this, debugMode);
        List<String> questIds = fileDataService.getCompletedQuestIds(uuid, playerName);

        // 推送到Redis
        RedisPlayerDataService redisDataService = new RedisPlayerDataService(debugMode);
        redisDataService.publishPlayerData(uuid, questIds);

        if (debugMode){
            this.getLogger().info("玩家" + playerName + "（" + uuid + "）的进度已广播到Redis");
        }
    }

    public void sync(UUID uuid, String playerName) {
        long startTime = System.currentTimeMillis();
        if (debugMode){
            this.getLogger().info("开始同步玩家" + playerName +"("+uuid+") 的任务进度");
        }
        List<String> questIds = playerDataService.getCompletedQuestIds(uuid, playerName);
        for (String questId : questIds) {
            String cmd = "ftbquests change_progress " + playerName + " complete " + questId;
            if (debugMode){
                this.getLogger().info("下发指令: "+cmd);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        long duration = System.currentTimeMillis() - startTime;
        if (debugMode){
            this.getLogger().info("执行完毕！耗时："+ duration + " ms");
        }
    }


}