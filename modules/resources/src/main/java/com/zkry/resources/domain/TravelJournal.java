package com.zkry.resources.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zkry.common.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tm_travel_journal")
public class TravelJournal extends BaseEntity {

    private Long userId;

    private Long tripPlanId;

    private String title;

    private String coverImage;

    private String destinationCity;

    private Integer travelDays;

    private String summary;

    private String status;

    private String visibility;

    private Integer viewCount;

    private Integer likeCount;
}
