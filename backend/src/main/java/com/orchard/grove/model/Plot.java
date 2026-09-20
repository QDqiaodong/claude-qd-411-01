package com.orchard.grove.model;

public class Plot {
    public Long id;
    public String code;
    public String name;
    public Double area;
    public String status;
    public String note;
    // 计算字段（不入库）：在产株数实时按 tree 表 status<>'已清' 统计，与清树动作天然对账
    public Integer activeTreeCount;
}
