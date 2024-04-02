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
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.arthas.v3.SamplingEnum;
import org.apache.skywalking.library.elasticsearch.requests.search.*;
import org.apache.skywalking.library.elasticsearch.requests.search.aggregation.Aggregation;
import org.apache.skywalking.library.elasticsearch.response.search.SearchHit;
import org.apache.skywalking.library.elasticsearch.response.search.SearchResponse;
import org.apache.skywalking.oap.server.core.analysis.manual.arthas.ArthasConstant;
import org.apache.skywalking.oap.server.core.analysis.manual.arthas.FlameDiagramSamplingStatus;
import org.apache.skywalking.oap.server.core.analysis.manual.machine.MachineConstant;
import org.apache.skywalking.oap.server.core.storage.model.MachineCondition;
import org.apache.skywalking.oap.server.core.storage.model.MachineData;
import org.apache.skywalking.oap.server.core.storage.model.MachineDataLine;
import org.apache.skywalking.oap.server.core.storage.model.arthas.*;
import org.apache.skywalking.oap.server.core.storage.query.IDayuQueryDao;
import org.apache.skywalking.oap.server.library.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.oap.server.library.util.StringUtil;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base.EsDAO;

import java.util.*;

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

    @Override
    public List<CpuCharts> getCpuCharts(ArthasCondition arthasCondition) {
        Integer profileTaskId = arthasCondition.getProfileTaskId();
        if (Objects.isNull(profileTaskId)) {
            log.error("query cpu charts data error, because profileTaskId is null");
            return Lists.newArrayList();
        }

        String indexName = ArthasConstant.CPU_INDEX_NAME + profileTaskId;
        SearchBuilder builder = Search.builder();
        builder.source(ArthasConstant.CPU_DATA);
        builder.source(ArthasConstant.DATA_SAMPLING_TIME);

        int total = 5000;
        List<CpuCharts> result = Lists.newArrayList();
        int length = arthasCondition.getDataTotal() / total;
        for (int i = 0; i <= length; i++) {
            builder.from(i);
            builder.size(i == length ? arthasCondition.getDataTotal() % total : total);
            SearchResponse response = getClient().search(indexName, builder.build());
            for (SearchHit hit : response.getHits().getHits()) {
                CpuCharts.CpuChartsBuilder cpuCharts = CpuCharts.builder();
                cpuCharts.cpuData((Double) hit.getSource().get(ArthasConstant.CPU_DATA));
                cpuCharts.dataSamplingTime((String) hit.getSource().get(ArthasConstant.DATA_SAMPLING_TIME));
                result.add(cpuCharts.build());
            }
        }
        return result;
    }

    @Override
    public List<CpuStack> getCpuStack(ArthasCondition arthasCondition) {
        Integer profileTaskId = arthasCondition.getProfileTaskId();
        if (Objects.isNull(profileTaskId)) {
            log.error("query cpu charts data error, because profileTaskId is null");
            return Lists.newArrayList();
        }
        String indexName = ArthasConstant.CPU_INDEX_NAME + profileTaskId;
        SearchBuilder builder = Search.builder();
        BoolQueryBuilder boolQueryBuilder = Query.bool();
        boolQueryBuilder.must(Query.term(ArthasConstant.SAMPLING_ENUM_KEYWORD, SamplingEnum.CPU));
        boolQueryBuilder.must(Query.term(ArthasConstant.DATA_SAMPLING_TIME_KEYWORD, arthasCondition.getDataSamplingTime()));

        builder.query(boolQueryBuilder);
        builder.source(ArthasConstant.STACK_LIST);
        builder.size(1);

        List<CpuStack> result = Lists.newArrayList();
        SearchResponse response = getClient().search(indexName, builder.build());
        for (SearchHit hit : response.getHits().getHits()) {
            List<Map<String, Object>> stackListData = (List<Map<String, Object>>) hit.getSource().get(ArthasConstant.STACK_LIST);
            stackListData.forEach(x -> {
                CpuStack.CpuStackBuilder cpuStackBuilder = CpuStack.builder();
                cpuStackBuilder.id((Integer) x.get("id")).cpu((Double) x.get("cpu"))
                        .state((String) x.get("state")).name((String) x.get("name"))
                        .priority((Integer) x.get("priority")).group((String) x.get("group"));
                result.add(cpuStackBuilder.build());
            });
        }
        result.sort(Comparator.comparing(CpuStack::getCpu).reversed());
        return result;
    }

    @Override
    public List<MemCharts> getMemCharts(ArthasCondition arthasCondition) {
        Integer profileTaskId = arthasCondition.getProfileTaskId();
        if (Objects.isNull(profileTaskId)) {
            log.error("query mem charts data error, because profileTaskId is null");
            return Lists.newArrayList();
        }
        String indexName = ArthasConstant.MEM_INDEX_NAME + profileTaskId;
        SearchBuilder builder = Search.builder();
        BoolQueryBuilder boolQueryBuilder = Query.bool();
        boolQueryBuilder.must(Query.term(ArthasConstant.SAMPLING_ENUM_KEYWORD, SamplingEnum.MEM));
        builder.query(boolQueryBuilder);

        int total = 5000;
        List<MemCharts> result = Lists.newArrayList();
        int length = arthasCondition.getDataTotal() / total;
        Gson gson = new Gson();
        for (int i = 0; i <= length; i++) {
            builder.from(i);
            builder.size(i == length ? arthasCondition.getDataTotal() % total : total);
            SearchResponse response = getClient().search(indexName, builder.build());
            for (SearchHit hit : response.getHits().getHits()) {
                MemCharts.MemChartsBuilder memChartsBuilder = MemCharts.builder();
                String memData = (String) hit.getSource().get(ArthasConstant.MEM_DATA);
                memData = memData.replace("_", "");
                MemCharts.MemChartsData memoryData = gson.fromJson(memData, MemCharts.MemChartsData.class);
                memChartsBuilder.dataSamplingTime((String) hit.getSource().get(ArthasConstant.DATA_SAMPLING_TIME))
                        .memData(memoryData);
                result.add(memChartsBuilder.build());
            }
        }
        return result;
    }

    @Override
    public ClassNameData getClassNameList(ArthasCondition arthasCondition) {
        Integer profileTaskId = arthasCondition.getProfileTaskId();
        ClassNameData.ClassNameDataBuilder builder = ClassNameData.builder();
        if (Objects.isNull(profileTaskId)) {
            log.error("query class name data error, because profileTaskId is null");
            return builder.build();
        }
        String indexName = ArthasConstant.CLASS_INDEX_NAME + profileTaskId;
        SearchResponse response = getClient().search(indexName, Search.builder().build());
        for (SearchHit hit : response.getHits().getHits()) {
            builder.classNameList((List<String>) hit.getSource().get(ArthasConstant.CLASS_NAME_LIST));
        }
        return builder.build();
    }

    @Override
    public SystemData getSystemData(ArthasCondition arthasCondition) {
        Integer profileTaskId = arthasCondition.getProfileTaskId();
        if (Objects.isNull(profileTaskId)) {
            log.error("query system data error, because profileTaskId is null");
            return new SystemData();
        }
        String indexName = ArthasConstant.SYSTEM_INDEX_NAME + profileTaskId;
        SearchResponse response = getClient().search(indexName, Search.builder().build());
        SystemData result = new SystemData();
        for (SearchHit hit : response.getHits().getHits()) {
            result.setJvmInfo((String) hit.getSource().get(ArthasConstant.JVM_INFO));
            result.setSysEnv((String) hit.getSource().get(ArthasConstant.SYS_ENV));
            result.setSysProp((String) hit.getSource().get(ArthasConstant.SYS_PROP));
            result.setVmOption((String) hit.getSource().get(ArthasConstant.VM_OPTION));
        }
        return result;
    }

    @Override
    public List<FlameDiagramList> getFlameDiagramList(Integer profileTaskId) {
        String indexName = ArthasConstant.FLAME_DIAGRAM_INDEX_NAME + profileTaskId;
        boolean exists = getClient().isExistsIndex(indexName);
        if (!exists) {
            getClient().createIndex(indexName);
        }
        SearchBuilder builder = Search.builder();
        builder.source(ArthasConstant.CREATE_TIME);
        builder.source(ArthasConstant.FLAME_DIAGRAM_SAMPLING_STATUS);
        SearchResponse response = getClient().search(indexName, builder.build());
        List<FlameDiagramList> result = Lists.newArrayList();
        for (SearchHit hit : response.getHits().getHits()) {

            result.add(new FlameDiagramList().setId(hit.getId())
                    .setStatus(FlameDiagramSamplingStatus.valueOf(String.valueOf(hit.getSource().get(ArthasConstant.FLAME_DIAGRAM_SAMPLING_STATUS))))
                    .setCreateTime(new Date((Long) hit.getSource().get(ArthasConstant.CREATE_TIME))));
        }
        return result;
    }

    @Override
    public String getFlameDiagram(Integer profileTaskId, String id) {
        String indexName = ArthasConstant.FLAME_DIAGRAM_INDEX_NAME + profileTaskId;
        boolean exists = getClient().isExistsIndex(indexName);
        if (!exists) {
            getClient().createIndex(indexName);
        }

        SearchBuilder builder = Search.builder();
        builder.query(Query.ids(id));
        SearchResponse response = getClient().search(indexName, builder.build());
        String result = "";
        for (SearchHit hit : response.getHits().getHits()) {
            result = String.valueOf(hit.getSource().get(ArthasConstant.FLAME_DIAGRAM_DATA));
        }
        return result;
    }

}
