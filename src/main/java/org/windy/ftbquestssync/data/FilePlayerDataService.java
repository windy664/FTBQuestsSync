package org.windy.ftbquestssync.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class FilePlayerDataService implements PlayerDataService {
    private final Plugin plugin;
    private final boolean debugMode;

    public FilePlayerDataService(Plugin plugin, boolean debugMode) {
        this.plugin = plugin;
        this.debugMode = debugMode;
    }

    @Override
    public List<String> getCompletedQuestIds(UUID playerUuid, String playerName) {
        List<String> result = new ArrayList<>();
        FileConfiguration config = plugin.getConfig();
        String path = config.getString("path");
        String fileName = playerUuid + ".snbt";
        String filePath = path + fileName;
        if (debugMode){
            plugin.getLogger().info("获取"+playerName+"FTB任务文件路径为" + filePath);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean inCompletedSection = false;
            if (debugMode){
                plugin.getLogger().info("开始搜索"+playerName+"的FTB任务进度");
            }
            while ((line = reader.readLine()) != null) {
                if (line.contains("completed: {")) {
                    inCompletedSection = true;
                } else if (inCompletedSection && line.contains("}")) {
                    break; // 结束读取
                }
                if (debugMode){
                    plugin.getLogger().info("开始对"+playerName+"任务进度分析...");
                }
                if (inCompletedSection) {
                    Pattern pattern = Pattern.compile("([A-Fa-f0-9]+): ");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        result.add(matcher.group(1));
                        if (debugMode){
                            plugin.getLogger().info("读取成功！\n"+playerName+"已完成的任务id为："+matcher.group(1));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}