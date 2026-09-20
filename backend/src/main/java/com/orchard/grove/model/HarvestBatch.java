package com.orchard.grove.model;

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
}
