package org.apache.skywalking.oap.server.core.query;

import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.core.storage.model.MachineCondition;
import org.apache.skywalking.oap.server.core.storage.model.MachineData;
import org.apache.skywalking.oap.server.core.storage.model.MachineDataLine;
import org.apache.skywalking.oap.server.core.storage.model.arthas.*;
import org.apache.skywalking.oap.server.core.storage.query.IDayuQueryDao;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.module.Service;

import java.util.List;

public class DayuQueryService implements Service {

    private final ModuleManager moduleManager;

    private IDayuQueryDao iDayuQueryDao;

    public DayuQueryService(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private IDayuQueryDao getDayuQueryDao() {
        if (iDayuQueryDao == null) {
            this.iDayuQueryDao = moduleManager.find(StorageModule.NAME).provider().getService(IDayuQueryDao.class);
        }
        return iDayuQueryDao;
    }

    public List<MachineData> getMachineMetrics(final MachineCondition machineCondition) {
        return getDayuQueryDao().getMachineMetrics(machineCondition);
    }

    public MachineDataLine getMachineMetricsLine(final MachineCondition machineCondition) {
        return getDayuQueryDao().getMachineMetricsLine(machineCondition);
    }

    public List<CpuCharts> getCpuCharts(final ArthasCondition arthasCondition) {
        return getDayuQueryDao().getCpuCharts(arthasCondition);
    }

    public List<CpuStack> getCpuStack(final ArthasCondition arthasCondition) {
        return getDayuQueryDao().getCpuStack(arthasCondition);
    }

    public List<MemCharts> getMemCharts(final ArthasCondition arthasCondition) {
        return getDayuQueryDao().getMemCharts(arthasCondition);
    }

    public ClassNameData getClassNameList(final ArthasCondition condition) {
        return getDayuQueryDao().getClassNameList(condition);
    }

    public SystemData getSystemData(final ArthasCondition arthasCondition) {
        return getDayuQueryDao().getSystemData(arthasCondition);
    }

    public List<FlameDiagramList> getFlameDiagramList(final Integer profileTaskId) {
        return getDayuQueryDao().getFlameDiagramList(profileTaskId);
    }

    public String getFlameDiagram(final Integer profileTaskId, final String id) {
        return getDayuQueryDao().getFlameDiagram(profileTaskId, id);
    }

    public Integer getFlameDiagramTotal(final Integer profileTaskId) {
        return getDayuQueryDao().getFlameDiagramTotal(profileTaskId);
    }

}
