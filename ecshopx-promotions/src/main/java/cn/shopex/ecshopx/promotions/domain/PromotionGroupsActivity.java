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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 拼团活动表 */
@Data
@MpTable(value = "promotion_groups_activity", comment = "拼团活动表", indexes = {@MpIndex(name = "idx_goodsid_begintime_endtime", columns = {"goods_id", "begin_time", "end_time"})})
public class PromotionGroupsActivity {

    /** 活动ID */
    @MpId(value = "groups_activity_id", type = IdType.AUTO, columnType = "bigint", comment = "活动ID")
    private Long groupsActivityId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 活动名称 */
    @MpField(value = "act_name", columnType = "string", length = 50, comment = "活动名称")
    private String actName;

    /** 商品ID */
    @MpField(value = "goods_id", columnType = "bigint", comment = "商品ID")
    private Long goodsId;

    /** 团购活动商品类型 */
    @MpField(value = "group_goods_type", columnType = "string", length = 255, comment = "团购活动商品类型", defaultValue = "services")
    private String groupGoodsType = "services";

    /** 活动封面 */
    @MpField(value = "pics", columnType = "string", length = 255, comment = "活动封面")
    private String pics;

    /** 活动价格 */
    @MpField(value = "act_price", columnType = "bigint", comment = "活动价格")
    private Long actPrice;

    /** 拼团人数 */
    @MpField(value = "person_num", columnType = "bigint", comment = "拼团人数")
    private Long personNum;

    /** 开始时间 */
    @MpField(value = "begin_time", columnType = "bigint", comment = "开始时间")
    private Long beginTime;

    /** 结束时间 */
    @MpField(value = "end_time", columnType = "bigint", comment = "结束时间")
    private Long endTime;

    /** 限买数量 */
    @MpField(value = "limit_buy_num", columnType = "bigint", comment = "限买数量")
    private Long limitBuyNum;

    /** 成团时效(单位时) */
    @MpField(value = "limit_time", columnType = "integer", comment = "成团时效(单位时)")
    private Integer limitTime;

    /** 拼团库存 */
    @MpField(value = "store", columnType = "bigint", comment = "拼团库存")
    private Long store;

    /** 是否包邮 */
    @MpField(value = "free_post", columnType = "boolean", nullable = true, comment = "是否包邮", defaultValue = "True")
    private Boolean freePost = true;

    /** 是否展示开团列表 */
    @MpField(value = "rig_up", columnType = "boolean", nullable = true, comment = "是否展示开团列表", defaultValue = "True")
    private Boolean rigUp = true;

    /** 成团机器人 */
    @MpField(value = "robot", columnType = "boolean", nullable = true, comment = "成团机器人", defaultValue = "True")
    private Boolean robot = true;

    /** 分享描述 */
    @MpField(value = "share_desc", columnType = "string", length = 100, nullable = true, comment = "分享描述")
    private String shareDesc = "";

    /** 是否禁用 true=禁用,false=启用 */
    @MpField(value = "disabled", columnType = "boolean", nullable = true, defaultValue = "False")
    private Boolean disabled = false;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
