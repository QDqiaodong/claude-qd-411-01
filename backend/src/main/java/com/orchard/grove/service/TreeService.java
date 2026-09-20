package com.orchard.grove.service;

import com.orchard.grove.dto.BizException;
import com.orchard.grove.mapper.PlotMapper;
import com.orchard.grove.mapper.TreeMapper;
import com.orchard.grove.model.Plot;
import com.orchard.grove.model.Tree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TreeService {
    @Autowired
    TreeMapper treeMapper;
    @Autowired
    PlotMapper plotMapper;

    public List<Tree> list() {
        return treeMapper.findAll();
    }

    public Tree create(Tree f) {
        if (f.code == null || f.code.isBlank()) throw new BizException("果树编号必填");
        if (treeMapper.findByCode(f.code) != null) throw new BizException("果树编号 " + f.code + " 已存在");
        if (f.plotId == null) throw new BizException("必须指定归属地块");
        Plot plot = plotMapper.findById(f.plotId);
        if (plot == null) throw new BizException("归属地块不存在");
        if (!"在用".equals(plot.status)) throw new BizException("只能归到在用地块");
        if (f.variety == null || f.variety.isBlank()) throw new BizException("品种必填");
        Tree t = new Tree();
        t.code = f.code;
        t.plotId = f.plotId;
        t.variety = f.variety;
        t.plantYear = f.plantYear;
        t.status = (f.status == null || f.status.isBlank()) ? "正常" : f.status;
        t.note = f.note;
        treeMapper.insert(t);
        return t;
    }

    public Tree update(Long id, Tree f) {
        Tree t = treeMapper.findById(id);
        if (t == null) throw new BizException("果树不存在");
        if (f.code != null && !f.code.isBlank()) t.code = f.code;
        if (f.plotId != null) {
            Plot plot = plotMapper.findById(f.plotId);
            if (plot == null) throw new BizException("归属地块不存在");
            if (!"在用".equals(plot.status)) throw new BizException("只能归到在用地块");
            t.plotId = f.plotId;
        }
        if (f.variety != null && !f.variety.isBlank()) t.variety = f.variety;
        if (f.plantYear != null) t.plantYear = f.plantYear;
        if (f.status != null && !f.status.isBlank()) t.status = f.status;
        if (f.note != null) t.note = f.note;
        treeMapper.update(t);
        return t;
    }
}
