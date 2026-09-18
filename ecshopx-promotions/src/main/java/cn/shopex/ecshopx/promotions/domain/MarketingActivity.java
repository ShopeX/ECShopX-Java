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

/** 各种促销活动表 */
@Data
@MpTable(value = "promotions_marketing_activity", comment = "各种促销活动表", indexes = {@MpIndex(name = "ix_marketing_type", columns = {"marketing_type"}), @MpIndex(name = "ix_start_time", columns = {"start_time"}), @MpIndex(name = "ix_source_id", columns = {"source_id"}), @MpIndex(name = "idx_companyid_starttime_endtime", columns = {"company_id", "start_time", "end_time"})})
public class MarketingActivity {

    /** 营销id */
    @MpId(value = "marketing_id", type = IdType.AUTO, columnType = "bigint", comment = "营销id")
    private Long marketingId;

    /**
     * 营销类型: full_discount:满折,full_minus:满减,full_gift:满赠,self_select:任选优惠,plus_price_buy:加价购,member_preference:会员优先购
     */
    @MpField(value = "marketing_type", columnType = "string", comment = "营销类型: full_discount:满折,full_minus:满减,full_gift:满赠,self_select:任选优惠,plus_price_buy:加价购,member_preference:会员优先购")
    private String marketingType;

    /** 关联其他营销id */
    @MpField(value = "rel_marketing_id", columnType = "bigint", nullable = true, comment = "关联其他营销id", defaultValue = "0")
    private Long relMarketingId = 0L;

    /** 营销活动名称 */
    @MpField(value = "marketing_name", columnType = "string", comment = "营销活动名称")
    private String marketingName;

    /** 活动广告图 */
    @MpField(value = "ad_pic", columnType = "string", nullable = true, comment = "活动广告图")
    private String adPic;

    /** 营销活动描述 */
    @MpField(value = "marketing_desc", columnType = "string", comment = "营销活动描述")
    private String marketingDesc;

    /** 活动开始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "活动开始时间")
    private Integer startTime;

    /** 活动结束时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "活动结束时间")
    private Integer endTime;

    /** 活动发布时间 */
    @MpField(value = "release_time", columnType = "integer", nullable = true, comment = "活动发布时间")
    private Integer releaseTime;

    /**
     * 适用平台:  0:全场可用,1:只用于pc端,2:小程序端,3:h5端
     */
    @MpField(value = "used_platform", columnType = "integer", comment = "适用平台:  0:全场可用,1:只用于pc端,2:小程序端,3:h5端", defaultValue = "0")
    private Integer usedPlatform = 0;

    /**
     * 适用范围: 0:全场可用,1:指定商品可用,2:指定分类可用,3:指定商品标签可用,4:指定商品品牌可用
     */
    @MpField(value = "use_bound", columnType = "integer", comment = "适用范围: 0:全场可用,1:指定商品可用,2:指定分类可用,3:指定商品标签可用,4:指定商品品牌可用", defaultValue = "0")
    private Integer useBound = 0;

    /** 标签id集合 */
    @MpField(value = "tag_ids", columnType = "text", nullable = true, comment = "标签id集合")
    private String tagIds;

    /** 品牌id集合 */
    @MpField(value = "brand_ids", columnType = "text", nullable = true, comment = "品牌id集合")
    private String brandIds;

    /** 适用店铺: 0:全场可用,1:指定店铺可用 */
    @MpField(value = "use_shop", columnType = "integer", comment = "适用店铺: 0:全场可用,1:指定店铺可用", defaultValue = "0")
    private Integer useShop = 0;

    /** 店铺id集合 */
    @MpField(value = "shop_ids", columnType = "text", nullable = true, comment = "店铺id集合", defaultValue = "all")
    private String shopIds = "all";

    /** 会员级别集合 */
    @MpField(value = "valid_grade", columnType = "text", nullable = true, comment = "会员级别集合")
    private String validGrade;

    /** 营销条件标准  quantity:按总件数, totalfee:按总金额 */
    @MpField(value = "condition_type", columnType = "text", comment = "营销条件标准  quantity:按总件数, totalfee:按总金额", defaultValue = "totalfee")
    private String conditionType = "totalfee";

    /** 营销规则值 */
    @MpField(value = "condition_value", columnType = "text", comment = "营销规则值")
    private String conditionValue;

    /** 是否按比例多次赠送 */
    @MpField(value = "in_proportion", columnType = "boolean", nullable = true, comment = "是否按比例多次赠送", defaultValue = "False")
    private Boolean inProportion = false;

    /** 加价购活动页面背景 */
    @MpField(value = "activity_background", columnType = "string", nullable = true, comment = "加价购活动页面背景")
    private String activityBackground;

    /** 加价购活动页面导航栏颜色 */
    @MpField(value = "navbar_color", columnType = "string", nullable = true, comment = "加价购活动页面导航栏颜色")
    private String navbarColor;

    /** 加价购活动页面时间背景颜色 */
    @MpField(value = "timeBackgroundColor", columnType = "string", nullable = true, comment = "加价购活动页面时间背景颜色")
    private String timeBackgroundColor;

    /** 是否上不封顶 */
    @MpField(value = "canjoin_repeat", columnType = "boolean", nullable = true, comment = "是否上不封顶", defaultValue = "False")
    private Boolean canjoinRepeat = false;

    /** 可参与次数 */
    @MpField(value = "join_limit", columnType = "integer", comment = "可参与次数", defaultValue = "0")
    private Integer joinLimit = 0;

    /** 是否免邮 */
    @MpField(value = "free_postage", columnType = "boolean", comment = "是否免邮", defaultValue = "False")
    private Boolean freePostage = false;

    /** 促销标签 */
    @MpField(value = "promotion_tag", columnType = "string", length = 15, comment = "促销标签")
    private String promotionTag;

    /**
     * 促销状态:  non-reviewed:未审核,pending:待审核,agree:审核通过,refuse:已拒绝,cancel:已取消,overdue:已过期
     */
    @MpField(value = "check_status", columnType = "string", comment = "促销状态:  non-reviewed:未审核,pending:待审核,agree:审核通过,refuse:已拒绝,cancel:已取消,overdue:已过期", defaultValue = "agree")
    private String checkStatus = "agree";

    /** 审核不通过原因 */
    @MpField(value = "reason", columnType = "string", length = 500, nullable = true, comment = "审核不通过原因")
    private String reason;

    /** 活动商品类型: normal:实体类商品,service:服务类商品 */
    @MpField(value = "item_type", columnType = "string", comment = "活动商品类型: normal:实体类商品,service:服务类商品", defaultValue = "normal")
    private String itemType = "normal";

    /** 开启加价购，满赠时启用 */
    @MpField(value = "is_increase_purchase", columnType = "boolean", nullable = true, comment = "开启加价购，满赠时启用")
    private Boolean isIncreasePurchase;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

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
}
