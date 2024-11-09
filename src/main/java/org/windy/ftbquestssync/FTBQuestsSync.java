package org.windy.ftbquestssync;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
public final class FTBQuestsSync extends JavaPlugin implements Listener {
    String value = this.getConfig().getString("Debug");
    boolean debugMode = Boolean.parseBoolean(value);


    @Override
    public void onEnable() {
        // Plugin startup logic
        this.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getServer().getConsoleSender().sendMessage(Texts.bless);
        this.getServer().getConsoleSender().sendMessage(Texts.logo);

        //日志屏蔽
        Logger logger = getServer().getLogger();

        // 设置日志过滤器
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFilter(new LogFilter());  // 为控制台日志添加过滤器
        logger.addHandler(consoleHandler);
    }

    @Override
    public void onDisable() {
        this.getServer().getConsoleSender().sendMessage(Texts.bless);
        this.getServer().getConsoleSender().sendMessage(Texts.logo);
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        String playerName = player.getName(); //获取玩家的名字
        boolean status = true;
        if(status){
            UUID uuid = player.getUniqueId();
            if (debugMode){
                this.getLogger().info("开始获取玩家" + playerName +"("+uuid+")的基本信息");
            }
            sync(String.valueOf(uuid),String.valueOf(playerName));
        }
    }
    public void sync(String uuid,String player) {
        long startTime = System.currentTimeMillis(); // 记录开始时间
        FileConfiguration config = getConfig();
        String path = config.getString("path");
        String fileName = uuid + ".snbt";
        String filePath = path + fileName;
        if (debugMode){
            this.getLogger().info("获取"+player+"FTB任务文件路径为" + filePath);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean inCompletedSection = false;
            boolean inChangeProgressSection = false;
            if (debugMode){
                this.getLogger().info("开始搜索"+player+"的FTB任务进度");
            }
            while ((line = reader.readLine()) != null) {
                if (line.contains("completed: {")) {
                    inCompletedSection = true;
                } else if (inCompletedSection && line.contains("}")) {
                    inCompletedSection = false;
                    break; // 结束读取
                }
                if (debugMode){
                    this.getLogger().info("开始对"+player+"任务进度分析...");
                }
                if (inCompletedSection) {
                    // 使用正则表达式提取key部分
                    Pattern pattern = Pattern.compile("([A-Fa-f0-9]+): ");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String result = "ftbquests change_progress "+player+" complete " + matcher.group(1);
                        if (debugMode){
                            this.getLogger().info("读取成功！\n"+player+"已完成的任务id为：\n"+result+"即将开始执行同步...");
                        }
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), result);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis(); // 记录结束时间
        long duration = endTime - startTime; // 计算耗时
        if (debugMode){
            this.getLogger().info("执行完毕！耗时："+ duration + " ms");
        }
    }
    class LogFilter implements Filter {
        @Override
        public boolean isLoggable(LogRecord record) {
            // 过滤掉包含 "Progress has been changed!" 或 "No quest object found" 的日志
            if (record.getMessage().contains("Progress has been changed!") ||
                    record.getMessage().contains("No quest object found")) {
                return false; // 不记录这些日志
            }
            return true; // 其他日志正常记录
        }
    }
}
