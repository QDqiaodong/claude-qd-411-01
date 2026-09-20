package com.orchard.grove.mapper;

import com.orchard.grove.model.Plot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlotMapper {
    List<Plot> findAll();
    Plot findById(@Param("id") Long id);
    Plot findByCode(@Param("code") String code);
    /** 在产株数：已清树不计入，清树/撤回后该值立刻随之变化 */
    int countTrees(@Param("plotId") Long plotId);
    int insert(Plot p);
    int update(Plot p);
}
