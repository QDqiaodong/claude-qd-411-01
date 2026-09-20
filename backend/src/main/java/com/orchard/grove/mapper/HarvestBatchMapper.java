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
}
