package com.orchard.grove.mapper;

import com.orchard.grove.model.HarvestBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HarvestBatchMapper {
    List<HarvestBatch> findAll();
    HarvestBatch findById(@Param("id") Long id);
    HarvestBatch findByPlotAndDate(@Param("plotId") Long plotId, @Param("batchDate") String batchDate);
    int insert(HarvestBatch b);
    int update(HarvestBatch b);
    /** 该树挂着的未入仓批次（待采/采集中），清树前据此拦截 */
    List<HarvestBatch> findUnstoredByTree(@Param("treeId") Long treeId);
    List<Long> findTreeIds(@Param("batchId") Long batchId);
    int insertBatchTree(@Param("batchId") Long batchId, @Param("treeId") Long treeId);
    int deleteBatchTrees(@Param("batchId") Long batchId);
}
