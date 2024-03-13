package org.apache.skywalking.oap.server.receiver.arthas;

import org.apache.skywalking.apm.network.arthas.v3.Command;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CommandQueue {

    private static final Map<String, Command> COMMANDS = new ConcurrentHashMap<>();

    private static final Map<String, String> FLAME_DIAGRAM_PATH = new ConcurrentHashMap<>();

    public static void produceCommand(String serviceName, String instanceName, Command command) {
        COMMANDS.put(serviceName + instanceName, command);
    }

    public static Optional<Command> consumeCommand(String serviceName, String instanceName) {
        return Optional.ofNullable(COMMANDS.remove(serviceName + instanceName));
    }

    public static void produceFlameDiagram(String serviceName, String instanceName, String filePath) {
        FLAME_DIAGRAM_PATH.put(serviceName + instanceName, filePath);
    }

    public static Optional<String> consumeFlameDiagram(String serviceName, String instanceName) {
        return Optional.ofNullable(FLAME_DIAGRAM_PATH.remove(serviceName + instanceName));
    }

}
