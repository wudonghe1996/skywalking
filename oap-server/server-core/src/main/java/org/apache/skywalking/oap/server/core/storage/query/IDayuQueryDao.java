package org.apache.skywalking.oap.server.core.storage.query;

import org.apache.skywalking.oap.server.core.storage.DAO;
import org.apache.skywalking.oap.server.core.storage.model.MachineCondition;
import org.apache.skywalking.oap.server.core.storage.model.MachineData;
import org.apache.skywalking.oap.server.core.storage.model.MachineDataLine;
import org.apache.skywalking.oap.server.core.storage.model.arthas.*;

import java.util.List;

public interface IDayuQueryDao extends DAO {

    List<MachineData> getMachineMetrics(final MachineCondition machineCondition);

    MachineDataLine getMachineMetricsLine(final MachineCondition machineCondition);

    List<CpuCharts> getCpuCharts(final ArthasCondition arthasCondition);

    List<CpuStack> getCpuStack(final ArthasCondition arthasCondition);

    List<MemCharts> getMemCharts(final ArthasCondition arthasCondition);

    ClassNameData getClassNameList(final ArthasCondition arthasCondition);

    SystemData getSystemData(final ArthasCondition arthasCondition);

    List<FlameDiagramList> getFlameDiagramList(final Integer profileTaskId);

    String getFlameDiagram(final Integer profileTaskId, final String id);
}
