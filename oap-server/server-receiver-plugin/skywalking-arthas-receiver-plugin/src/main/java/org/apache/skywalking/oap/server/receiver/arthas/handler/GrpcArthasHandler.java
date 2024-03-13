package org.apache.skywalking.oap.server.receiver.arthas.handler;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.arthas.v3.ArthasCommandServiceGrpc;
import org.apache.skywalking.apm.network.arthas.v3.Command;
import org.apache.skywalking.apm.network.arthas.v3.CommandRequest;
import org.apache.skywalking.apm.network.arthas.v3.CommandResponse;
import org.apache.skywalking.oap.server.receiver.arthas.CommandQueue;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class GrpcArthasHandler extends ArthasCommandServiceGrpc.ArthasCommandServiceImplBase implements GRPCHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcArthasHandler.class);

    @Override
    public void get(final CommandRequest request, final StreamObserver<CommandResponse> responseObserver) {
        CommandResponse.Builder builder = CommandResponse.newBuilder().setCommand(Command.NONE);
        CommandQueue.consumeCommand(request.getServiceName(), request.getInstanceName())
                .ifPresent(command -> {
                    LOGGER.info(
                            "consume {} command for service {}, instance {}", command, request.getServiceName(),
                            request.getInstanceName()
                    );
                    builder.setCommand(command);
                });
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}