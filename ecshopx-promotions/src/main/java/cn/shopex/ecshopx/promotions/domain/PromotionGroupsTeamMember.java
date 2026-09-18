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

/** 参团表 */
@Data
@MpTable(value = "promotion_groups_team_member", comment = "参团表")
public class PromotionGroupsTeamMember {

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

    /** 会员ID, 0为拼团机器人 */
    @MpField(value = "member_id", columnType = "bigint", nullable = true, comment = "会员ID, 0为拼团机器人")
    private Long memberId;

    /** 参团时间 */
    @MpField(value = "join_time", columnType = "bigint", comment = "参团时间")
    private Long joinTime;

    /** 订单编号 */
    @MpField(value = "order_id", columnType = "string", length = 100, nullable = true, comment = "订单编号")
    private String orderId;

    /** 团购活动商品类型 */
    @MpField(value = "group_goods_type", columnType = "string", length = 255, comment = "团购活动商品类型", defaultValue = "services")
    private String groupGoodsType = "services";

    /** 拼团机器人 */
    @MpField(value = "member_info", columnType = "string", length = 255, nullable = true, comment = "拼团机器人")
    private String memberInfo;

    /** 是否禁用 true=禁用,false=启用 */
    @MpField(value = "disabled", columnType = "boolean", nullable = true, defaultValue = "False")
    private Boolean disabled;
}
