package com.zkry.resources.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zkry.common.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tm_journal_location")
public class JournalLocation extends BaseEntity {

    private Long journalId;

    private String placeName;

    private String placeType;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Integer dayIndex;

    private String timeOfDay;

    private String description;

    private Integer sortOrder;
}
