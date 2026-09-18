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

/** 秒杀活动表 */
@Data
@MpTable(value = "promotions_seckill_activity", comment = "秒杀活动表", indexes = {@MpIndex(name = "ix_item_type", columns = {"item_type"}), @MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class SeckillActivity {

    /** 秒杀活动id */
    @MpId(value = "seckill_id", type = IdType.AUTO, columnType = "bigint", comment = "秒杀活动id")
    private Long seckillId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 店铺ID, 多个逗号隔开 */
    @MpField(value = "distributor_id", columnType = "string", nullable = true, comment = "店铺ID, 多个逗号隔开")
    private String distributorId;

    /** 秒杀活动名称 */
    @MpField(value = "activity_name", columnType = "string", nullable = true, comment = "秒杀活动名称")
    private String activityName;

    /** 秒杀活动广告图 */
    @MpField(value = "ad_pic", columnType = "string", nullable = true, comment = "秒杀活动广告图")
    private String adPic;

    /** 秒杀开始时间 */
    @MpField(value = "activity_start_time", columnType = "integer", comment = "秒杀开始时间")
    private Integer activityStartTime;

    /** 秒杀结束时间 */
    @MpField(value = "activity_end_time", columnType = "integer", comment = "秒杀结束时间")
    private Integer activityEndTime;

    /** 秒杀活动发布时间 */
    @MpField(value = "activity_release_time", columnType = "integer", comment = "秒杀活动发布时间")
    private Integer activityReleaseTime;

    /** 秒杀活动是否返佣 */
    @MpField(value = "is_activity_rebate", columnType = "boolean", comment = "秒杀活动是否返佣", defaultValue = "False")
    private Boolean isActivityRebate = false;

    /** 秒杀活动是否包邮 */
    @MpField(value = "is_free_shipping", columnType = "boolean", comment = "秒杀活动是否包邮", defaultValue = "False")
    private Boolean isFreeShipping = false;

    /** 每人累计限额 */
    @MpField(value = "limit_total_money", columnType = "bigint", nullable = true, comment = "每人累计限额")
    private Long limitTotalMoney;

    /** 每人单笔限额 */
    @MpField(value = "limit_money", columnType = "bigint", nullable = true, comment = "每人单笔限额")
    private Long limitMoney;

    /** 未付款订单保留时长（分钟） */
    @MpField(value = "validity_period", columnType = "integer", nullable = true, comment = "未付款订单保留时长（分钟）")
    private Integer validityPeriod;

    /** 其他扩展字段 */
    @MpField(value = "otherext", columnType = "text", nullable = true, comment = "其他扩展字段")
    private String otherext;

    /** 秒杀活动描述 */
    @MpField(value = "description", columnType = "string", nullable = true, comment = "秒杀活动描述")
    private String description;

    /** 秒杀类型 normal正常的秒杀活动， limited_time_sale限时特惠 */
    @MpField(value = "seckill_type", columnType = "string", comment = "秒杀类型 normal正常的秒杀活动， limited_time_sale限时特惠", defaultValue = "normal")
    private String seckillType = "normal";

    /**
     * 秒杀活动商品类型。可选：normal 实体类；services 服务类
     */
    @MpField(value = "item_type", columnType = "string", comment = "秒杀活动商品类型", defaultValue = "normal")
    private String itemType = "normal";

    /** 适用范围: 1:指定商品可用 */
    @MpField(value = "use_bound", columnType = "integer", comment = "适用范围: 1:指定商品可用", defaultValue = "1")
    private Integer useBound = 1;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 添加者类型：distributor */
    @MpField(value = "source_type", columnType = "string", length = 20, nullable = true, comment = "添加者类型：distributor")
    private String sourceType;

    /** 添加者ID: 如店铺ID */
    @MpField(value = "source_id", columnType = "bigint", nullable = true, comment = "添加者ID: 如店铺ID", defaultValue = "0")
    private Long sourceId = 0L;

    /** 是否失效 */
    @MpField(value = "disabled", columnType = "boolean", comment = "是否失效", defaultValue = "0")
    private Boolean disabled = false;
}
