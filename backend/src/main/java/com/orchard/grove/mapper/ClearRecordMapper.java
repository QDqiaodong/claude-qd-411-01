package com.orchard.grove.mapper;

import com.orchard.grove.model.ClearRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClearRecordMapper {
    List<ClearRecord> findAll();
    ClearRecord findById(@Param("id") Long id);
    /** 该树当前生效（未撤回）的清树单，同一棵树至多一张 */
    ClearRecord findActiveByTree(@Param("treeId") Long treeId);
    int insert(ClearRecord r);
    int withdraw(@Param("id") Long id, @Param("withdrawnAt") String withdrawnAt);
}
