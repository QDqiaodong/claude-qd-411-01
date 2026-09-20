package com.orchard.grove.model;

import java.util.List;

public class HarvestBatch {
    public Long id;
    public Long plotId;
    public String batchDate;
    public String status;
    public Double estimateKg;
    public Double actualKg;
    public String variety;
    /** 本批次挂着的果树（入参为 id 列表；查询时回填树编号） */
    public List<Long> treeIds;
    public List<String> treeCodes;
    // 计算字段（不入库）：被间隔未满的药单卡住时，前端据此处显示原因并禁止推进
    public Boolean blocked;
    public String blockReason;
}
