package org.windy.ftbquestssync.log;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class LogFilter implements Filter {
    @Override
    public boolean isLoggable(LogRecord record) {
        if (record.getMessage().contains("Progress has been changed!") ||
                record.getMessage().contains("No quest object found") ||
                record.getMessage().contains("complete ed<--[HERE]")) {
            return false;
        }
        return true;
    }
}