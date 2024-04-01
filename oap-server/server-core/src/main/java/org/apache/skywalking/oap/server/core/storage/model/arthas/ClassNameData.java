package org.apache.skywalking.oap.server.core.storage.model.arthas;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClassNameData {
    private List<String> classNameList;
}
