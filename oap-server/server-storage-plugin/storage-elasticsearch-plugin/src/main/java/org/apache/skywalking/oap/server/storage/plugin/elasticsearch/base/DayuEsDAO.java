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

package org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.arthas.v3.ArthasSamplingData;
import org.apache.skywalking.apm.network.arthas.v3.SystemData;
import org.apache.skywalking.apm.network.dayu.v3.Machine;
import org.apache.skywalking.apm.network.dayu.v3.MachineMetric;
import org.apache.skywalking.oap.server.core.storage.model.arthas.CpuStack;
import org.apache.skywalking.oap.server.core.analysis.manual.arthas.ArthasConstant;
import org.apache.skywalking.oap.server.core.analysis.manual.machine.MachineConstant;
import org.apache.skywalking.oap.server.core.storage.IDayuDAO;
import org.apache.skywalking.oap.server.library.client.elasticsearch.ElasticSearchClient;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class DayuEsDAO extends EsDAO implements IDayuDAO {

    public DayuEsDAO(ElasticSearchClient client) {
        super(client);
    }

    @Override
    public void saveMachineMetrics(Machine machine) {
        try {
            boolean exists = getClient().isExistsIndex(MachineConstant.INDEX_NAME);
            if (!exists) {
                getClient().createIndex(MachineConstant.INDEX_NAME);
            }
            String serviceName = machine.getServiceName();
            String instanceName = machine.getInstanceName();
            for (MachineMetric machineMetric : machine.getMetricsList()) {
                Map<String, Object> map = Maps.newHashMap();
                map.put("serviceName", serviceName);
                map.put("instanceName", instanceName);
                map.put("cpuCore", machineMetric.getCpuCore());
                map.put("memoryTotal", machineMetric.getMemoryTotal());
                map.put("processCount", machineMetric.getProcessCount());
                map.put("threadCount", machineMetric.getThreadCount());
                map.put("systemCpuUsed", machineMetric.getSystemCpuUsed());
                map.put("processCpuUsed", machineMetric.getProcessCpuUsed());
                map.put("machineMemoryUsed", machineMetric.getMachineMemoryUsed());
                map.put("netRecv", machineMetric.getNetRecv());
                map.put("netSent", machineMetric.getNetSent());
                map.put("time", machineMetric.getTime());
                map.put("date", new Date(machineMetric.getTime()));
                getClient().forceInsert(MachineConstant.INDEX_NAME, UUID.randomUUID().toString(), map);
            }
        } catch (Exception e) {
            log.error("save process machine data fail, {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void saveArthasData(ArthasSamplingData arthasSamplingData) {
        switch (arthasSamplingData.getSamplingEnum()){
            case CPU:
                saveCpuData(arthasSamplingData);
                break;
            case MEM:
                saveMemData(arthasSamplingData);
                break;
            case SYSTEM:
                saveSystemData(arthasSamplingData);
        }
    }

    private void saveCpuData(ArthasSamplingData arthasSamplingData){
        try {
            int profileTaskId = arthasSamplingData.getProfileTaskId();
            String indexName = ArthasConstant.CPU_INDEX_NAME + profileTaskId;
            boolean exists = getClient().isExistsIndex(indexName);
            if (!exists) {
                getClient().createIndex(indexName);
            }

            Map<String, Object> map = Maps.newHashMap();
            map.put("dataSamplingTime", arthasSamplingData.getDataSamplingTime());
            map.put("samplingEnum", arthasSamplingData.getSamplingEnum());
            map.put("cpuData", arthasSamplingData.getCpuData());
            List<CpuStack> cpuStackList = arthasSamplingData.getStackListList().stream()
                    .map(x -> CpuStack.builder().cpu(x.getCpu()).id(x.getId())
                            .state(x.getState()).name(x.getName())
                            .priority(x.getPriority()).group(x.getGroup()).build())
                    .collect(Collectors.toList());
            map.put("stackList", cpuStackList);
            getClient().forceInsert(indexName, UUID.randomUUID().toString(), map);
        } catch (Exception e) {
            log.error("save arthas profile task cpu data fail, {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveMemData(ArthasSamplingData arthasSamplingData){
        try {
            int profileTaskId = arthasSamplingData.getProfileTaskId();
            String indexName = ArthasConstant.MEM_INDEX_NAME + profileTaskId;
            boolean exists = getClient().isExistsIndex(indexName);
            if (!exists) {
                getClient().createIndex(indexName);
            }

            Map<String, Object> map = Maps.newHashMap();
            map.put("dataSamplingTime", arthasSamplingData.getDataSamplingTime());
            map.put("samplingEnum", arthasSamplingData.getSamplingEnum());
            Gson gson = new Gson();
            String memoryData = gson.toJson(arthasSamplingData.getMemoryData());
            map.put("memData", memoryData);
            getClient().forceInsert(indexName, UUID.randomUUID().toString(), map);
        } catch (Exception e) {
            log.error("save arthas profile task mem data fail, {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveSystemData(ArthasSamplingData arthasSamplingData){
        try {
            int profileTaskId = arthasSamplingData.getProfileTaskId();
            String indexName = ArthasConstant.SYSTEM_INDEX_NAME + profileTaskId;
            boolean exists = getClient().isExistsIndex(indexName);
            if (!exists) {
                getClient().createIndex(indexName);
            }

            Map<String, Object> map = Maps.newHashMap();
            SystemData systemData = arthasSamplingData.getSystemData();
            map.put("jvmInfo", systemData.getJvmInfo());
            map.put("sysEnv", systemData.getSysEnv());
            map.put("sysProp", systemData.getSysProp());
            map.put("vmOption", systemData.getVmOption());
            getClient().forceInsert(indexName, UUID.randomUUID().toString(), map);
        } catch (Exception e) {
            log.error("save arthas profile task system data fail, {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
