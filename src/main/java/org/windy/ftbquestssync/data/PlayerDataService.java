package org.windy.ftbquestssync.data;

import java.util.List;
import java.util.UUID;

public interface PlayerDataService {
    List<String> getCompletedQuestIds(UUID playerUuid, String playerName);
}