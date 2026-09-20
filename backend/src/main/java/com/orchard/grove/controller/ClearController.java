package com.orchard.grove.controller;

import com.orchard.grove.model.ClearRecord;
import com.orchard.grove.service.ClearService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clear-records")
public class ClearController {
    @Autowired
    ClearService service;

    @GetMapping
    public List<ClearRecord> list() {
        return service.list();
    }

    /** 开清树单：{ treeId, reason } */
    @PostMapping
    public ClearRecord clear(@RequestBody Map<String, Object> body) {
        Object id = body.get("treeId");
        if (id == null) throw new IllegalArgumentException("treeId 必填");
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        return service.clearTree(Long.valueOf(String.valueOf(id)), reason);
    }

    /** 撤回清树单 */
    @PostMapping("/{id}/withdraw")
    public ClearRecord withdraw(@PathVariable Long id) {
        return service.withdraw(id);
    }
}
