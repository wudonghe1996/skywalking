/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.receiver.dayu.provider.grpc;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.dayu.v3.ArthasIpMessage;
import org.apache.skywalking.apm.network.dayu.v3.DayuMessage;
import org.apache.skywalking.apm.network.dayu.v3.DayuServiceGrpc;
import org.apache.skywalking.apm.network.dayu.v3.Machine;
import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.apache.skywalking.oap.server.core.storage.IDayuDAO;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCHandler;
import org.apache.skywalking.oap.server.receiver.dayu.constant.DayuConstant;
import org.apache.skywalking.oap.server.receiver.dayu.provider.DayuModuleProvider;
import org.apache.skywalking.oap.server.receiver.dayu.utils.HttpUtils;

import java.io.IOException;
import java.util.HashMap;

@Slf4j
public class DayuServiceHandler extends DayuServiceGrpc.DayuServiceImplBase implements GRPCHandler {

    public static final String DAYU_HOST = "http://" + DayuModuleProvider.HOST;

    public DayuServiceHandler(ModuleManager moduleManager) { }

    @Override
    public void online(final DayuMessage request,
                       final StreamObserver<Commands> responseObserver) {
        String url = DAYU_HOST + DayuConstant.ONLINE_URL;
        HashMap<String, Object> map = buildParams(request);
        map.put("serviceStartTime", request.getServiceStartTime());
        HttpUtils.doPostRequest(url, map);

        responseObserver.onNext(Commands.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void offline(final DayuMessage request, final StreamObserver<Commands> responseObserver) {
        HttpUtils.doPostRequest(DAYU_HOST + DayuConstant.OFFLINE_URL, buildParams(request));

        responseObserver.onNext(Commands.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendArthasIp(final ArthasIpMessage request, final StreamObserver<Commands> responseObserver) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("serviceName", request.getServiceName());
        params.put("instanceName", request.getInstanceName());
        params.put("ip", request.getIp());

        HttpUtils.doPostRequest(DAYU_HOST + DayuConstant.SEND_ARTHAS_IP_URL, params);

        responseObserver.onNext(Commands.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendMachineMetrics(final Machine machine, final StreamObserver<Commands> responseObserver) {
        IDayuDAO dayuDao = DayuModuleProvider.getDayuDao();
        try {
            dayuDao.saveMachineMetrics(machine);
        } catch (IOException e) {
            log.error("save process machine metrics data error");
            e.printStackTrace();
        }
        responseObserver.onNext(Commands.newBuilder().build());
        responseObserver.onCompleted();
    }

    private HashMap<String, Object> buildParams(DayuMessage request) {
        HashMap<String, Object> params = new HashMap<>();
        String serviceName = request.getServiceName();
        params.put("serviceId", IDManager.ServiceID.buildId(serviceName, true));
        params.put("serviceName", serviceName);
        params.put("instanceName", request.getInstanceName());
        params.put("timeStamp", System.currentTimeMillis());
        return params;
    }
}
