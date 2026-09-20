package com.orchard.grove.service;

import com.orchard.grove.dto.BizException;
import com.orchard.grove.mapper.HarvestBatchMapper;
import com.orchard.grove.mapper.HarvestBatchTreeMapper;
import com.orchard.grove.mapper.PlotMapper;
import com.orchard.grove.mapper.TreeMapper;
import com.orchard.grove.model.HarvestBatch;
import com.orchard.grove.model.Plot;
import com.orchard.grove.model.SprayRecord;
import com.orchard.grove.model.Tree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HarvestBatchService {
    @Autowired
    HarvestBatchMapper batchMapper;
    @Autowired
    HarvestBatchTreeMapper batchTreeMapper;
    @Autowired
    TreeMapper treeMapper;
    @Autowired
    PlotMapper plotMapper;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    SprayService sprayService;

    public List<HarvestBatch> list() {
        List<HarvestBatch> all = batchMapper.findAll();
        for (HarvestBatch b : all) {
            fillTrees(b);
            b.blocked = false;
            if (!"已入仓".equals(b.status)) {
                SprayRecord s = sprayService.blockingSpray(b.plotId, b.batchDate);
                if (s != null) {
                    b.blocked = true;
                    b.blockReason = sprayService.blockReason(s) + "，本批次在解禁前不能推进";
                }
            }
        }
        return all;
    }

    public HarvestBatch getById(Long id) {
        HarvestBatch b = batchMapper.findById(id);
        if (b == null) throw new BizException("采摘批次不存在");
        fillTrees(b);
        return b;
    }

    @Transactional
    public HarvestBatch create(HarvestBatch f) {
        if (f.plotId == null) throw new BizException("必须指定地块");
        Plot plot = plotMapper.findById(f.plotId);
        if (plot == null) throw new BizException("地块不存在");
        if (!"在用".equals(plot.status)) throw new BizException("停用地块不能开采摘批次");
        SprayRecord block = sprayService.plotIntervalBlock(f.plotId);
        if (block != null)
            throw new BizException(sprayService.blockReason(block) + "，间隔未满前不能新开采摘批次");
        if (f.batchDate == null || f.batchDate.isBlank()) throw new BizException("采摘日期必填");
        if (batchMapper.findByPlotAndDate(f.plotId, f.batchDate) != null)
            throw new BizException("该地块 " + f.batchDate + " 已有一批采摘");
        if (f.variety == null || f.variety.isBlank()) throw new BizException("品种必填");
        if (f.estimateKg == null || f.estimateKg <= 0) throw new BizException("预估产量必须大于 0");

        List<Tree> picked = validateTrees(f.plotId, f.treeIds);

        HarvestBatch b = new HarvestBatch();
        b.plotId = f.plotId;
        b.batchDate = f.batchDate;
        b.variety = f.variety;
        b.estimateKg = f.estimateKg;
        b.status = "待采";
        b.actualKg = null;
        batchMapper.insert(b);
        for (Tree t : picked) batchTreeMapper.insertBatchTree(b.id, t.id);
        return getById(b.id);
    }

    @Transactional
    public HarvestBatch update(Long id, HarvestBatch f) {
        HarvestBatch b = batchMapper.findById(id);
        if (b == null) throw new BizException("采摘批次不存在");

        // 已入仓批次关联冻结：清树/撤回都不允许动历史批次，库存也只认入仓这一刻
        if (f.treeIds != null && !"已入仓".equals(b.status)) {
            List<Tree> picked = validateTrees(b.plotId, f.treeIds);
            batchTreeMapper.deleteByBatch(b.id);
            for (Tree t : picked) batchTreeMapper.insertBatchTree(b.id, t.id);
        }

        if (f.status != null && !f.status.isBlank() && !f.status.equals(b.status)) {
            String next = f.status;
            boolean ok = ("待采".equals(b.status) && "采集中".equals(next))
                    || ("采集中".equals(b.status) && "已入仓".equals(next))
                    || ("待采".equals(b.status) && "已入仓".equals(next));
            if (!ok) throw new BizException("采摘状态只能 待采→采集中→已入仓");
            SprayRecord blockSpray = sprayService.blockingSpray(b.plotId, b.batchDate);
            if (blockSpray != null)
                throw new BizException(sprayService.blockReason(blockSpray)
                        + "，且施药日不晚于本批次采摘日 " + b.batchDate + "，本批次暂不能推进到「" + next + "」");
            if ("已入仓".equals(next)) {
                Double actual = f.actualKg != null ? f.actualKg : b.actualKg;
                if (actual == null) throw new BizException("入仓必须登记实际产量");
                if (actual > b.estimateKg) throw new BizException("实际产量不得超过预估 " + b.estimateKg);
                inventoryService.onStored(b.variety, actual);
                b.actualKg = actual;
            }
            b.status = next;
        }
        if (f.estimateKg != null) b.estimateKg = f.estimateKg;
        if (f.variety != null && !f.variety.isBlank()) b.variety = f.variety;
        if (f.actualKg != null && !"已入仓".equals(b.status)) b.actualKg = f.actualKg;
        batchMapper.update(b);
        return getById(b.id);
    }

    /** 批次选树：必须非空、必须是该地块的在产树；已清树不能出现在新开/修改的批次里。 */
    private List<Tree> validateTrees(Long plotId, List<Long> treeIds) {
        if (treeIds == null || treeIds.isEmpty()) throw new BizException("采摘批次至少要选一棵在产果树");
        List<Long> distinct = new ArrayList<>(new HashSet<>(treeIds));
        List<Tree> trees = treeMapper.findByIds(distinct);
        Map<Long, Tree> map = trees.stream().collect(Collectors.toMap(x -> x.id, Function.identity()));
        List<String> notFound = new ArrayList<>();
        List<String> otherPlot = new ArrayList<>();
        List<String> cleared = new ArrayList<>();
        for (Long tid : distinct) {
            Tree t = map.get(tid);
            if (t == null) notFound.add("#" + tid);
            else if (!t.plotId.equals(plotId)) otherPlot.add(t.code);
            else if ("已清".equals(t.status)) cleared.add(t.code);
        }
        if (!notFound.isEmpty()) throw new BizException("以下果树不存在：" + String.join("、", notFound));
        if (!otherPlot.isEmpty()) throw new BizException("以下果树不属于本地块，不能挂到本批次：" + String.join("、", otherPlot));
        if (!cleared.isEmpty()) throw new BizException("以下果树已清，不能再出现在采摘批次里：" + String.join("、", cleared));
        return trees;
    }

    private void fillTrees(HarvestBatch b) {
        List<Tree> trees = batchTreeMapper.findTreesByBatch(b.id);
        b.treeIds = trees.stream().map(t -> t.id).collect(Collectors.toList());
        b.treeCodes = trees.stream().map(t -> {
            String suffix = "已清".equals(t.status) ? "（已清）" : "";
            return t.code + suffix;
        }).collect(Collectors.toList());
    }
}
