package com.orchard.grove.service;

import com.orchard.grove.dto.BizException;
import com.orchard.grove.mapper.ClearRecordMapper;
import com.orchard.grove.mapper.HarvestBatchTreeMapper;
import com.orchard.grove.mapper.TreeMapper;
import com.orchard.grove.model.ClearRecord;
import com.orchard.grove.model.HarvestBatch;
import com.orchard.grove.model.Tree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 清树台账。四条铁律：
 * 1. 清树成功的同一事务里把树置「已清」，地块在产株数实时统计，立刻少一棵；
 * 2. 树上还挂着待采/采集中批次时清树必须挡住，并报出具体批次；
 * 3. 清树、撤回都不动已入仓库存（本服务不接触 inventory）；
 * 4. 撤回只恢复株数与状态，不自动回挂撤单前从批次摘掉的树。
 */
@Service
public class ClearService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    ClearRecordMapper clearMapper;
    @Autowired
    TreeMapper treeMapper;
    @Autowired
    HarvestBatchTreeMapper batchTreeMapper;
    @Autowired
    HarvestBatchService batchService;

    public List<ClearRecord> list() {
        return clearMapper.findAll();
    }

    /**
     * 开清树单。行锁 + 生效单唯一索引双重防并发：两个人同时点同一棵树，
     * 后到的一笔会被挡下并说明原因。
     */
    @Transactional
    public ClearRecord clearTree(Long treeId, String reason) {
        Tree t = treeMapper.findByIdForUpdate(treeId);
        if (t == null) throw new BizException("果树不存在");
        ClearRecord exist = clearMapper.findActiveByTree(treeId);
        if (exist != null || "已清".equals(t.status))
            throw new BizException("果树 " + t.code + " 已有生效中的清树单（单号 "
                    + (exist != null ? exist.id : "—") + "），同一棵树只许清一次，请刷新后查看");

        // 树上还挂着未入仓批次 → 挡住，必须把是哪一条批次说清楚
        List<Long> openBatchIds = batchTreeMapper.findOpenBatchIdsByTree(treeId);
        if (!openBatchIds.isEmpty()) {
            StringBuilder sb = new StringBuilder("果树 ").append(t.code)
                    .append(" 还挂着未入仓批次，清树已被挡住，请先把它从批次中摘掉或等批次入仓：");
            for (Long bid : openBatchIds) {
                HarvestBatch b = batchService.getById(bid);
                sb.append("批次#").append(bid).append("（").append(b.batchDate)
                        .append("，状态：").append(b.status).append("）、");
            }
            sb.setLength(sb.length() - 1);
            throw new BizException(sb.toString());
        }

        ClearRecord r = new ClearRecord();
        r.treeId = t.id;
        r.plotId = t.plotId;
        r.prevStatus = t.status;
        r.reason = reason;
        r.status = "生效";
        r.createdAt = LocalDateTime.now().format(TS);
        r.withdrawnAt = null;
        try {
            clearMapper.insert(r);
        } catch (DuplicateKeyException e) {
            // 唯一索引兜底：并发下另一笔已抢先生效
            throw new BizException("果树 " + t.code + " 刚被另一个操作清掉，只保留一笔清树单，本笔未生效");
        }
        if (treeMapper.updateStatus(t.id, "已清") != 1)
            throw new BizException("清树失败，请刷新后重试");
        return clearMapper.findById(r.id);
    }

    /**
     * 撤回清树单：株数加回、健康状态恢复。刻意不碰任何批次关联——
     * 撤回前已从某条未入仓批次摘掉的树，不自动回挂、那条批次不会因此变回可推；
     * 已入仓库存同样不动。
     */
    @Transactional
    public ClearRecord withdraw(Long id) {
        ClearRecord r = clearMapper.findById(id);
        if (r == null) throw new BizException("清树单不存在");
        if (r.withdrawnAt != null) throw new BizException("清树单 " + id + " 已撤回，不能重复撤回");

        Tree t = treeMapper.findByIdForUpdate(r.treeId);
        if (t == null) throw new BizException("果树档案已不存在，无法撤回");
        if (!"已清".equals(t.status))
            throw new BizException("果树当前不是「已清」状态，清树单状态与档案对不上，拒绝撤回");

        String now = LocalDateTime.now().format(TS);
        if (clearMapper.withdraw(id, now) != 1)
            throw new BizException("清树单 " + id + " 已被另一个操作撤回，本笔未生效");
        if (treeMapper.updateStatus(t.id, r.prevStatus) != 1)
            throw new BizException("撤回失败，请刷新后重试");
        return clearMapper.findById(id);
    }
}
