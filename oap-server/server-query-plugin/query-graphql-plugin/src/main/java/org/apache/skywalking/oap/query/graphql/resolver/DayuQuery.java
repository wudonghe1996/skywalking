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

package org.apache.skywalking.oap.query.graphql.resolver;

import graphql.kickstart.tools.GraphQLQueryResolver;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.query.DayuQueryService;
import org.apache.skywalking.oap.server.core.storage.model.MachineCondition;
import org.apache.skywalking.oap.server.core.storage.model.MachineData;
import org.apache.skywalking.oap.server.core.storage.model.MachineDataLine;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import java.util.List;

public class DayuQuery implements GraphQLQueryResolver {

    private final ModuleManager moduleManager;
    private DayuQueryService queryService;

    public DayuQuery(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private DayuQueryService getQueryService() {
        if (queryService == null) {
            this.queryService = moduleManager.find(CoreModule.NAME).provider().getService(DayuQueryService.class);
        }
        return queryService;
    }

    public List<MachineData> getMachineMetrics(final MachineCondition condition) {
        return getQueryService().getMachineMetrics(condition);
    }

    public MachineDataLine getMachineMetricsLine(final MachineCondition condition) {
        return getQueryService().getMachineMetricsLine(condition);
    }
}
