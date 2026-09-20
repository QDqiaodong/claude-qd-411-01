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
    // 计算字段（不入库）：被间隔未满的药单卡住时，前端据此处显示原因并禁止推进
    public Boolean blocked;
    public String blockReason;
    // 批次挂的果树（存 batch_tree 表）：开单选树、未入仓可摘掉、已入仓冻结
    public List<Long> treeIds;
}
