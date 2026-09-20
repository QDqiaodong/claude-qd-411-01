package com.orchard.grove.mapper;

import com.orchard.grove.model.Tree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TreeMapper {
    List<Tree> findAll();
    Tree findById(@Param("id") Long id);
    Tree findByCode(@Param("code") String code);
    int countByPlotAndStatus(@Param("plotId") Long plotId, @Param("status") String status);
    int insert(Tree t);
    int update(Tree t);
}
