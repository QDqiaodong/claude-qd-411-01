package com.orchard.grove.mapper;

import com.orchard.grove.model.Tree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HarvestBatchTreeMapper {
    /** 批次挂着的全部果树（含已清树：历史批次要留痕） */
    List<Tree> findTreesByBatch(@Param("batchId") Long batchId);
    /** 该树挂着的未入仓（待采/采集中）批次——清树时据此拦截 */
    List<Long> findOpenBatchIdsByTree(@Param("treeId") Long treeId);
    void deleteByBatch(@Param("batchId") Long batchId);
    void insertBatchTree(@Param("batchId") Long batchId, @Param("treeId") Long treeId);
}
