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
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.dayu.v3.Machine;
import org.apache.skywalking.apm.network.dayu.v3.MachineMetric;
import org.apache.skywalking.oap.server.core.analysis.manual.machine.MachineConstant;
import org.apache.skywalking.oap.server.core.storage.IDayuDAO;
import org.apache.skywalking.oap.server.library.client.elasticsearch.ElasticSearchClient;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

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

}
