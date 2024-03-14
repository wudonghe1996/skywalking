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

package org.apache.skywalking.oap.server.storage.plugin.elasticsearch.query;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.library.elasticsearch.requests.search.*;
import org.apache.skywalking.library.elasticsearch.requests.search.aggregation.Aggregation;
import org.apache.skywalking.library.elasticsearch.response.search.SearchHit;
import org.apache.skywalking.library.elasticsearch.response.search.SearchResponse;
import org.apache.skywalking.oap.server.core.analysis.manual.machine.MachineConstant;
import org.apache.skywalking.oap.server.core.storage.model.MachineCondition;
import org.apache.skywalking.oap.server.core.storage.model.MachineData;
import org.apache.skywalking.oap.server.core.storage.model.MachineDataLine;
import org.apache.skywalking.oap.server.core.storage.query.IDayuQueryDao;
import org.apache.skywalking.oap.server.library.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.oap.server.library.util.StringUtil;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base.EsDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class DayuQueryEsDAO extends EsDAO implements IDayuQueryDao {
    public DayuQueryEsDAO(ElasticSearchClient client) {
        super(client);
    }

    @Override
    public List<MachineData> getMachineMetrics(final MachineCondition machineCondition) {
        String serviceName = machineCondition.getServiceName();
        String instanceName = machineCondition.getInstanceName();
        if (StringUtil.isEmpty(serviceName) || StringUtil.isEmpty(instanceName)) {
            log.error("query process data error, because serviceName or instanceName is null");
            return Lists.newArrayList();
        }

        SearchBuilder search = Search.builder();
        BoolQueryBuilder boolQueryBuilder = Query.bool();
        boolQueryBuilder.must(Query.term(MachineConstant.SERVICE_NAME_KEYWORD, serviceName));
        boolQueryBuilder.must(Query.term(MachineConstant.INSTANCE_NAME_KEYWORD, instanceName));
        Long startTime = machineCondition.getStartTime();
        Long endTime = machineCondition.getEndTime();
        if (Objects.nonNull(startTime) && Objects.nonNull(endTime)) {
            boolQueryBuilder.must(Query.range(MachineConstant.TIME).gte(startTime).lte(endTime));
        }

        search.query(boolQueryBuilder);
        search.size(10000);
        SearchResponse response;
        response = getClient().search(MachineConstant.INDEX_NAME, search.build());

        List<MachineData> result = Lists.newArrayList();
        for (SearchHit hit : response.getHits().getHits()) {
            Map<String, Object> sourceAsMap = hit.getSource();
            MachineData.MachineDataBuilder machine = MachineData.builder();
            machine.cpuCore((Integer) sourceAsMap.get(MachineConstant.CPU_CORE));
            machine.memoryTotal((Integer) sourceAsMap.get(MachineConstant.MEMORY_TOTAL));
            machine.processCount((Integer) sourceAsMap.get(MachineConstant.PROCESS_COUNT));
            machine.threadCount((Integer) sourceAsMap.get(MachineConstant.THREAD_COUNT));
            machine.systemCpuUsed((Double) sourceAsMap.get(MachineConstant.SYSTEM_CPU_USED));
            machine.processCpuUsed((Double) sourceAsMap.get(MachineConstant.PROCESS_CPU_USED));
            machine.machineMemoryUsed((Double) sourceAsMap.get(MachineConstant.MACHINE_MEMORY_USED));
            machine.netRecv(Long.parseLong(sourceAsMap.get(MachineConstant.NET_RECV).toString()));
            machine.netSent(Long.parseLong(sourceAsMap.get(MachineConstant.NET_RECV).toString()));
            machine.time((Long) sourceAsMap.get(MachineConstant.TIME));
            result.add(machine.build());
        }
        return result;
    }

    @Override
    public MachineDataLine getMachineMetricsLine(MachineCondition machineCondition) {
        String serviceName = machineCondition.getServiceName();
        String instanceName = machineCondition.getInstanceName();
        MachineDataLine.MachineDataLineBuilder builder = MachineDataLine.builder();
        if (StringUtil.isEmpty(serviceName) || StringUtil.isEmpty(instanceName)) {
            log.error("query process data error, because serviceName or instanceName is null");
            return builder.build();
        }

        SearchBuilder searchBuilder = Search.builder();
        BoolQueryBuilder boolQueryBuilder = Query.bool();
        boolQueryBuilder.must(Query.term(MachineConstant.SERVICE_NAME_KEYWORD, serviceName));
        boolQueryBuilder.must(Query.term(MachineConstant.INSTANCE_NAME_KEYWORD, instanceName));
        Long startTime = machineCondition.getStartTime();
        Long endTime = machineCondition.getEndTime();
        if (Objects.nonNull(startTime) && Objects.nonNull(endTime)) {
            boolQueryBuilder.must(Query.range(MachineConstant.TIME).gte(startTime).lte(endTime));
        }
        searchBuilder.aggregation(Aggregation.avg(MachineConstant.PROCESS_CPU_AVG).field(MachineConstant.PROCESS_CPU_USED));
        searchBuilder.aggregation(Aggregation.max(MachineConstant.PROCESS_CPU_MAX).field(MachineConstant.PROCESS_CPU_USED));
        searchBuilder.aggregation(Aggregation.avg(MachineConstant.SYSTEM_CPU_AVG).field(MachineConstant.SYSTEM_CPU_USED));
        searchBuilder.aggregation(Aggregation.max(MachineConstant.SYSTEM_CPU_MAX).field(MachineConstant.SYSTEM_CPU_USED));
        searchBuilder.aggregation(Aggregation.avg(MachineConstant.MACHINE_MEMORY_AVG).field(MachineConstant.MACHINE_MEMORY_USED));
        searchBuilder.aggregation(Aggregation.max(MachineConstant.MACHINE_MEMORY_MAX).field(MachineConstant.MACHINE_MEMORY_USED));

        searchBuilder.query(boolQueryBuilder);
        searchBuilder.size(1);
        searchBuilder.sort(MachineConstant.TIME, Sort.Order.DESC);
        SearchResponse response = getClient().search(MachineConstant.INDEX_NAME, searchBuilder.build());

        pushLineDataToBuilder(builder, response.getAggregations(), response.getHits().getHits());

        return builder.build();
    }

    private void pushLineDataToBuilder(MachineDataLine.MachineDataLineBuilder builder,
                                       Map<String, Object> aggregations,
                                       List<SearchHit> hits) {
        builder.processCpuAvg(((HashMap<String, Double>) aggregations.get(MachineConstant.PROCESS_CPU_AVG)).get("value"))
                .processCpuMax(((HashMap<String, Double>) aggregations.get(MachineConstant.PROCESS_CPU_MAX)).get("value"))
                .systemCpuAvg(((HashMap<String, Double>) aggregations.get(MachineConstant.SYSTEM_CPU_AVG)).get("value"))
                .systemCpuMax(((HashMap<String, Double>) aggregations.get(MachineConstant.SYSTEM_CPU_MAX)).get("value"))
                .machineMemoryAvg(((HashMap<String, Double>) aggregations.get(MachineConstant.MACHINE_MEMORY_AVG)).get("value"))
                .machineMemoryMax(((HashMap<String, Double>) aggregations.get(MachineConstant.MACHINE_MEMORY_MAX)).get("value"));

        for (SearchHit hit : hits) {
            builder.cpuCore((Integer) hit.getSource().get(MachineConstant.CPU_CORE));
            builder.memoryTotal((Integer) hit.getSource().get(MachineConstant.MEMORY_TOTAL));
            builder.processCount((Integer) hit.getSource().get(MachineConstant.PROCESS_COUNT));
            builder.threadCount((Integer) hit.getSource().get(MachineConstant.THREAD_COUNT));
        }
    }
}
