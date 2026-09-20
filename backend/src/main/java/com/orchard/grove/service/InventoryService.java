package com.orchard.grove.service;

import com.orchard.grove.dto.BizException;
import com.orchard.grove.mapper.InventoryMapper;
import com.orchard.grove.model.Inventory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryService {
    @Autowired
    InventoryMapper inventoryMapper;

    public List<Inventory> list() {
        return inventoryMapper.findAll();
    }

    public void onStored(String variety, Double kg) {
        if (variety == null || variety.isBlank()) throw new BizException("品种必填");
        inventoryMapper.upsert(variety, 0.0);
        inventoryMapper.addStock(variety, kg);
    }

    public Inventory outbound(String variety, Double kg) {
        Inventory inv = inventoryMapper.findByVariety(variety);
        if (inv == null) throw new BizException("该品种无库存记录");
        if (inv.stockKg == null || inv.stockKg < kg)
            throw new BizException("库存不足，无法出库（现有 " + (inv.stockKg == null ? 0 : inv.stockKg) + "kg）");
        inventoryMapper.subtractStock(variety, kg);
        return inventoryMapper.findByVariety(variety);
    }

    public Inventory setWarnLine(String variety, Double warnLine) {
        Inventory inv = inventoryMapper.findByVariety(variety);
        if (inv == null) throw new BizException("该品种无库存记录");
        inv.warnLine = warnLine;
        inventoryMapper.update(inv);
        return inv;
    }
}
