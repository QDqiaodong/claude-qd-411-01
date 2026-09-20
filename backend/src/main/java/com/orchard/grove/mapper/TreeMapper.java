package com.orchard.grove.mapper;

import com.orchard.grove.model.Tree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TreeMapper {
    List<Tree> findAll();
    Tree findById(@Param("id") Long id);
    /** 清树/撤回时加行锁，把两个人同时点清树串行化 */
    Tree findByIdForUpdate(@Param("id") Long id);
    /** 按 id 集合取树，校验批次选树时一次取回 */
    List<Tree> findByIds(@Param("ids") List<Long> ids);
    Tree findByCode(@Param("code") String code);
    int countByPlotAndStatus(@Param("plotId") Long plotId, @Param("status") String status);
    int insert(Tree t);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    int update(Tree t);
}
