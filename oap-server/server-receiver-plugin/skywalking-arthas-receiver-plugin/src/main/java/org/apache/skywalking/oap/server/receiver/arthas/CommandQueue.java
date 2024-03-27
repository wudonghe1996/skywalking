package org.apache.skywalking.oap.server.receiver.arthas;

import lombok.Builder;
import lombok.Data;
import org.apache.skywalking.apm.network.arthas.v3.Command;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CommandQueue {

    private static final Map<String, ArthasCommand> COMMANDS = new ConcurrentHashMap<>();

    private static final Map<String, String> FLAME_DIAGRAM_PATH = new ConcurrentHashMap<>();

    public static void produceCommand(String serviceName, String instanceName, Integer profileTaskId, Command command) {
        ArthasCommand.ArthasCommandBuilder builder = ArthasCommand.builder();
        builder.command(command).profileTaskId(profileTaskId);
        COMMANDS.put(serviceName + instanceName, builder.build());
    }

    public static Optional<ArthasCommand> consumeCommand(String serviceName, String instanceName) {
        return Optional.ofNullable(COMMANDS.remove(serviceName + instanceName));
    }

    public static void produceFlameDiagram(String serviceName, String instanceName, String filePath) {
        FLAME_DIAGRAM_PATH.put(serviceName + instanceName, filePath);
    }

    public static Optional<String> consumeFlameDiagram(String serviceName, String instanceName) {
        return Optional.ofNullable(FLAME_DIAGRAM_PATH.remove(serviceName + instanceName));
    }

    @Data
    @Builder
    public static class ArthasCommand {
        private Command command;
        private Integer profileTaskId;
    }

}
