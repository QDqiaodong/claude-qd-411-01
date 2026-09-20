package com.orchard.grove.model;

public class ClearRecord {
    public Long id;
    public Long treeId;
    public Long plotId;
    /** 清树前果树的健康状态（正常/病害/弱株），撤回时恢复用 */
    public String prevStatus;
    public String reason;
    /** 生效 / 已撤回 */
    public String status;
    public String createdAt;
    public String withdrawnAt;
    // 计算字段（不入库）：展示用
    public String treeCode;
    public String variety;
}
