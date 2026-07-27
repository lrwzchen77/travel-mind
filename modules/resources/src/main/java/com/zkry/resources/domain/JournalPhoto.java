package com.zkry.resources.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zkry.common.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tm_journal_photo")
public class JournalPhoto extends BaseEntity {

    private Long journalId;

    private String photoUrl;

    private String caption;

    private String location;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Integer dayIndex;

    private Integer sortOrder;
}
