package org.apache.skywalking.oap.server.receiver.arthas;

import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Data;
import org.apache.skywalking.apm.network.arthas.v3.Command;
import org.apache.skywalking.apm.network.arthas.v3.RealTimeCommand;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CommandQueue {

    private static final Map<String, ArthasCommand> COMMANDS = new ConcurrentHashMap<>();
    private static final Map<String, List<RealTimeQueueData>> REAL_TIME_COMMANDS = new ConcurrentHashMap<>();

    public static void produceCommand(String serviceName, String instanceName, Integer profileTaskId, Command command) {
        ArthasCommand.ArthasCommandBuilder builder = ArthasCommand.builder();
        builder.command(command).profileTaskId(profileTaskId);
        COMMANDS.put(getKey(serviceName, instanceName), builder.build());
    }

    public static Optional<ArthasCommand> consumeCommand(String serviceName, String instanceName) {
        return Optional.ofNullable(COMMANDS.remove(getKey(serviceName, instanceName)));
    }

    public static void produceRealTimeCommand(String serviceName, String instanceName, RealTimeCommand realTimeCommand, String command) {
        String key = getKey(serviceName, instanceName);
        RealTimeQueueData.RealTimeQueueDataBuilder builder = RealTimeQueueData.builder();
        builder.realTimeCommand(realTimeCommand).command(command);
        if (!REAL_TIME_COMMANDS.containsKey(key)) {
            REAL_TIME_COMMANDS.put(key, Lists.newArrayList(builder.build()));
        } else {
            REAL_TIME_COMMANDS.get(key).add(builder.build());
        }
    }

    public static Optional<List<RealTimeQueueData>> consumeRealTimeCommand(String serviceName, String instanceName) {
        return Optional.ofNullable(REAL_TIME_COMMANDS.remove(getKey(serviceName, instanceName)));
    }

    public static String getKey(String serviceName, String instanceName) {
        return serviceName + instanceName;
    }

    @Data
    @Builder
    public static class ArthasCommand {
        private Command command;
        private Integer profileTaskId;
    }

    @Data
    @Builder
    public static class RealTimeQueueData {
        private String command;
        private RealTimeCommand realTimeCommand;
    }
}
