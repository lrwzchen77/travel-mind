package com.zkry.resources.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zkry.resources.domain.TravelJournal;
import org.apache.ibatis.annotations.Mapper;

/**
 * 游记主表 Mapper 接口。
 */
@Mapper
public interface TravelJournalMapper extends BaseMapper<TravelJournal> {
}
