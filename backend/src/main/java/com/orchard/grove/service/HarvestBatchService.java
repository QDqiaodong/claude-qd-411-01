package com.orchard.grove.service;

import com.orchard.grove.dto.BizException;
import com.orchard.grove.mapper.HarvestBatchMapper;
import com.orchard.grove.mapper.PlotMapper;
import com.orchard.grove.model.HarvestBatch;
import com.orchard.grove.model.Plot;
import com.orchard.grove.model.SprayRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HarvestBatchService {
    @Autowired
    HarvestBatchMapper batchMapper;
    @Autowired
    PlotMapper plotMapper;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    SprayService sprayService;

    public List<HarvestBatch> list() {
        List<HarvestBatch> all = batchMapper.findAll();
        for (HarvestBatch b : all) {
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
        HarvestBatch b = new HarvestBatch();
        b.plotId = f.plotId;
        b.batchDate = f.batchDate;
        b.variety = f.variety;
        b.estimateKg = f.estimateKg;
        b.status = "待采";
        b.actualKg = null;
        batchMapper.insert(b);
        return b;
    }

    public HarvestBatch update(Long id, HarvestBatch f) {
        HarvestBatch b = batchMapper.findById(id);
        if (b == null) throw new BizException("采摘批次不存在");
        if (f.status != null && !f.status.isBlank() && !f.status.equals(b.status)) {
            String next = f.status;
            boolean ok = ("待采".equals(b.status) && "采集中".equals(next))
                    || ("采集中".equals(b.status) && "已入仓".equals(next))
                    || ("待采".equals(b.status) && "已入仓".equals(next));
            if (!ok) throw new BizException("采摘状态只能 待采→采集中→已入仓");
            SprayRecord block = sprayService.blockingSpray(b.plotId, b.batchDate);
            if (block != null)
                throw new BizException(sprayService.blockReason(block)
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
        return b;
    }
}
