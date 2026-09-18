/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.promotions.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 开团表 */
@Data
@MpTable(value = "promotion_groups_team", comment = "开团表")
public class PromotionGroupsTeam {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 团id号 */
    @MpField(value = "team_id", columnType = "string", length = 100, comment = "团id号")
    private String teamId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 活动ID号 */
    @MpField(value = "act_id", columnType = "bigint", comment = "活动ID号")
    private Long actId;

    /** 团长会员ID */
    @MpField(value = "head_mid", columnType = "bigint", comment = "团长会员ID")
    private Long headMid;

    /** 开团时间 */
    @MpField(value = "begin_time", columnType = "bigint", comment = "开团时间")
    private Long beginTime;

    /** 结束时间(根据成团时效和活动结束时间算出来的) */
    @MpField(value = "end_time", columnType = "bigint", comment = "结束时间(根据成团时效和活动结束时间算出来的)")
    private Long endTime;

    /** 参与人数 */
    @MpField(value = "join_person_num", columnType = "bigint", comment = "参与人数", defaultValue = "0")
    private Long joinPersonNum = 0L;

    /** 团购活动商品类型 */
    @MpField(value = "group_goods_type", columnType = "string", length = 255, comment = "团购活动商品类型", defaultValue = "services")
    private String groupGoodsType = "services";

    /** 状态:1.进行中2.成功3.失败 */
    @MpField(value = "team_status", columnType = "bigint", comment = "状态:1.进行中2.成功3.失败", defaultValue = "1")
    private Long teamStatus = 1L;

    /** 是否禁用 true=禁用,false=启用 */
    @MpField(value = "disabled", columnType = "boolean", nullable = true, defaultValue = "False")
    private Boolean disabled;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
