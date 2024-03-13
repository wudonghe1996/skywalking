package org.apache.skywalking.oap.server.core.storage.query;

import org.apache.skywalking.oap.server.core.storage.DAO;
import org.apache.skywalking.oap.server.core.storage.model.MachineCondition;
import org.apache.skywalking.oap.server.core.storage.model.MachineData;
import org.apache.skywalking.oap.server.core.storage.model.MachineDataLine;
import java.util.List;

public interface IDayuQueryDao extends DAO {

    List<MachineData> getMachineMetrics(final MachineCondition machineCondition);

    MachineDataLine getMachineMetricsLine(final MachineCondition machineCondition);
}
