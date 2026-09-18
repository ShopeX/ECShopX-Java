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

package cn.shopex.ecshopx.kaquan.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 优惠券信息表 */
@Data
@MpTable(value = "kaquan_discount_cards", comment = "优惠券信息表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_source_id", columns = {"source_id"})})
public class DiscountCards {

    /** 卡券id */
    @MpId(value = "card_id", type = IdType.AUTO, columnType = "bigint", length = 64, comment = "卡券id")
    private Long cardId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 卡券类型，可选值有，discount:折扣券;cash:代金券;gift:兑换券;new_gift:兑换券(新) */
    @MpField(value = "card_type", columnType = "string", length = 16, comment = "卡券类型，可选值有，discount:折扣券;cash:代金券;gift:兑换券;new_gift:兑换券(新)")
    private String cardType;

    /** 商户名称 */
    @MpField(value = "brand_name", columnType = "string", length = 36, nullable = true, comment = "商户名称")
    private String brandName;

    /** 卡券商户 logo */
    @MpField(value = "logo_url", columnType = "string", nullable = true, comment = "卡券商户 logo")
    private String logoUrl;

    /** 卡券名,最大9个汉字 */
    @MpField(value = "title", columnType = "string", length = 27, comment = "卡券名,最大9个汉字")
    private String title;

    /** 券颜色值 */
    @MpField(value = "color", columnType = "string", length = 16, comment = "券颜色值")
    private String color;

    /** 卡券使用提醒,最大16汉字 */
    @MpField(value = "notice", columnType = "string", length = 48, nullable = true, comment = "卡券使用提醒,最大16汉字")
    private String notice;

    /** 卡券使用说明 */
    @MpField(value = "description", columnType = "text", comment = "卡券使用说明")
    private String description;

    /** 有效期的类型 */
    @MpField(value = "date_type", columnType = "string", comment = "有效期的类型")
    private String dateType;

    /** 有效期开始时间 */
    @MpField(value = "begin_date", columnType = "integer", nullable = true, comment = "有效期开始时间")
    private Integer beginDate;

    /** 有效期结束时间 */
    @MpField(value = "end_date", columnType = "integer", nullable = true, comment = "有效期结束时间")
    private Integer endDate;

    /** 有效期的有效天数 */
    @MpField(value = "fixed_term", columnType = "integer", nullable = true, comment = "有效期的有效天数")
    private Integer fixedTerm;

    /** 指定会员id */
    @MpField(value = "grade_ids", columnType = "string", comment = "指定会员id")
    private String gradeIds = "";

    /** 指定付费会员id */
    @MpField(value = "vip_grade_ids", columnType = "string", comment = "指定付费会员id")
    private String vipGradeIds = "";

    /** 卡券状态 0:正常 1:暂停 2:关闭 */
    @MpField(value = "kq_status", columnType = "integer", comment = "卡券状态 0:正常 1:暂停 2:关闭", defaultValue = "0")
    private Integer kqStatus = 0;

    /** 兑换商品后的锁定时间 */
    @MpField(value = "lock_time", columnType = "integer", comment = "兑换商品后的锁定时间", defaultValue = "0")
    private Integer lockTime = 0;

    /** 发放开始时间 */
    @MpField(value = "send_begin_time", columnType = "integer", nullable = true, comment = "发放开始时间")
    private Integer sendBeginTime;

    /** 发放结束时间 */
    @MpField(value = "send_end_time", columnType = "integer", nullable = true, comment = "发放结束时间")
    private Integer sendEndTime;

    /** 客服电话 */
    @MpField(value = "service_phone", columnType = "string", length = 15, nullable = true, comment = "客服电话")
    private String servicePhone;

    /** 卡券顶部居中的按钮，仅在卡券状态正常(可以核销)时显示 */
    @MpField(value = "center_title", columnType = "string", nullable = true, comment = "卡券顶部居中的按钮，仅在卡券状态正常(可以核销)时显示")
    private String centerTitle;

    /** 显示在入口下方的提示语 */
    @MpField(value = "center_sub_title", columnType = "string", nullable = true, comment = "显示在入口下方的提示语")
    private String centerSubTitle;

    /** 顶部居中的url */
    @MpField(value = "center_url", columnType = "string", nullable = true, comment = "顶部居中的url")
    private String centerUrl;

    /** 自定义跳转外链的入口名字 */
    @MpField(value = "custom_url_name", columnType = "string", length = 15, nullable = true, comment = "自定义跳转外链的入口名字")
    private String customUrlName;

    /** 自定义跳转的URL */
    @MpField(value = "custom_url", columnType = "string", nullable = true, comment = "自定义跳转的URL")
    private String customUrl;

    /** 显示在入口右侧的提示语 */
    @MpField(value = "custom_url_sub_title", columnType = "string", length = 18, nullable = true, comment = "显示在入口右侧的提示语")
    private String customUrlSubTitle;

    /** 营销场景的自定义入口名称 */
    @MpField(value = "promotion_url_name", columnType = "string", length = 15, nullable = true, comment = "营销场景的自定义入口名称")
    private String promotionUrlName;

    /** 营销场景的自定义入口url */
    @MpField(value = "promotion_url", columnType = "string", nullable = true, comment = "营销场景的自定义入口url")
    private String promotionUrl;

    /** 营销入口右侧的提示语 */
    @MpField(value = "promotion_url_sub_title", columnType = "string", length = 18, nullable = true, comment = "营销入口右侧的提示语")
    private String promotionUrlSubTitle;

    /** 每人可领券的数量限制 */
    @MpField(value = "get_limit", columnType = "integer", nullable = true, comment = "每人可领券的数量限制")
    private Integer getLimit;

    /** 每人可核销的数量限制 */
    @MpField(value = "use_limit", columnType = "integer", nullable = true, comment = "每人可核销的数量限制")
    private Integer useLimit;

    /** 卡券领取页面是否可分享 */
    @MpField(value = "can_share", columnType = "string", nullable = true, comment = "卡券领取页面是否可分享", defaultValue = "false")
    private String canShare = "false";

    /** 卡券是否可转赠 */
    @MpField(value = "can_give_friend", columnType = "string", nullable = true, comment = "卡券是否可转赠", defaultValue = "false")
    private String canGiveFriend = "false";

    /** 封面摘要 */
    @MpField(value = "abstract", columnType = "string", nullable = true, comment = "封面摘要")
    private String coverAbstract;

    /** 封面图片 */
    @MpField(value = "icon_url_list", columnType = "string", nullable = true, comment = "封面图片")
    private String iconUrlList;

    /** 图文列表（序列化/JSON） */
    @MpField(value = "text_image_list", columnType = "array", nullable = true, comment = "图文列表")
    private String textImageList;

    /** 使用时段限制（序列化/JSON） */
    @MpField(value = "time_limit", columnType = "array", nullable = true, comment = "使用时段限制")
    private String timeLimit;

    /** 兑换券兑换内容名称 */
    @MpField(value = "gift", columnType = "string", nullable = true, comment = "兑换券兑换内容名称")
    private String gift;

    /** 优惠券优惠详情 */
    @MpField(value = "default_detail", columnType = "string", nullable = true, comment = "优惠券优惠详情")
    private String defaultDetail;

    /** 折扣券打折额度（百分比) */
    @MpField(value = "discount", columnType = "integer", nullable = true, comment = "折扣券打折额度（百分比)", defaultValue = "0")
    private Integer discount = 0;

    /** 代金券起用金额 */
    @MpField(value = "least_cost", columnType = "integer", nullable = true, comment = "代金券起用金额", defaultValue = "0")
    private Integer leastCost = 0;

    /** 代金券减免金额 or 兑换券起用金额 */
    @MpField(value = "reduce_cost", columnType = "integer", nullable = true, comment = "代金券减免金额 or 兑换券起用金额", defaultValue = "0")
    private Integer reduceCost = 0;

    /** 团购券详情 */
    @MpField(value = "deal_detail", columnType = "string", nullable = true, comment = "团购券详情")
    private String dealDetail;

    /** 指定可用的商品类目,代金券专用 */
    @MpField(value = "accept_category", columnType = "string", nullable = true, comment = "指定可用的商品类目,代金券专用")
    private String acceptCategory;

    /** 指定不可用的商品类目,代金券专用 */
    @MpField(value = "reject_category", columnType = "string", nullable = true, comment = "指定不可用的商品类目,代金券专用")
    private String rejectCategory;

    /** 购买xx可用类型门槛，仅用于兑换 */
    @MpField(value = "object_use_for", columnType = "string", nullable = true, comment = "购买xx可用类型门槛，仅用于兑换")
    private String objectUseFor;

    /** 是否可与其他优惠共享 */
    @MpField(value = "can_use_with_other_discount", columnType = "string", nullable = true, comment = "是否可与其他优惠共享", defaultValue = "false")
    private String canUseWithOtherDiscount = "false";

    /** 优惠券适用平台（线上商城专用 or 门店专用）。mall 商城专用；store 门店专用 */
    @MpField(value = "use_platform", columnType = "string", nullable = true, comment = "优惠券适用平台（线上商城专用 or 门店专用）", defaultValue = "store")
    private String usePlatform = "store";

    /** 卡券数量 */
    @MpField(value = "quantity", columnType = "integer", comment = "卡券数量")
    private Integer quantity;

    /** 是否适用所有门店 */
    @MpField(value = "use_all_shops", columnType = "string", nullable = true, comment = "是否适用所有门店", defaultValue = "true")
    private String useAllShops = "true";

    /** 适用的门店 */
    @MpField(value = "rel_shops_ids", columnType = "text", nullable = true, comment = "适用的门店")
    private String relShopsIds;

    /** 核销场景。可选值有，ONLINE:线上商城(兑换券不可使用);QUICK:快捷买单(兑换券不可使用);SWEEP:门店支付(扫码核销);SELF:到店支付(自助核销) */
    @MpField(value = "use_scenes", columnType = "string", nullable = true, comment = "核销场景。可选值有，ONLINE:线上商城(兑换券不可使用);QUICK:快捷买单(兑换券不可使用);SWEEP:门店支付(扫码核销);SELF:到店支付(自助核销)", defaultValue = "QUICK")
    private String useScenes = "QUICK";

    /** 自助核销验证码 */
    @MpField(value = "self_consume_code", columnType = "integer", nullable = true, comment = "自助核销验证码", defaultValue = "0")
    private Integer selfConsumeCode = 0;

    /** 是否前台直接领取 */
    @MpField(value = "receive", columnType = "string", nullable = true, comment = "是否前台直接领取", defaultValue = "true")
    private String receive = "true";

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "text", nullable = true, comment = "店铺id", defaultValue = ",")
    private String distributorId = ",";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;

    /** 代金券最高消费限额 */
    @MpField(value = "most_cost", columnType = "integer", nullable = true, comment = "代金券最高消费限额", defaultValue = "0")
    private Integer mostCost = 99999900;

    /** 适用范围: 0:全场可用,1:指定商品可用,2:指定分类可用,3:指定商品标签可用,4:指定商品品牌可用 */
    @MpField(value = "use_bound", columnType = "integer", comment = "适用范围: 0:全场可用,1:指定商品可用,2:指定分类可用,3:指定商品标签可用,4:指定商品品牌可用", defaultValue = "0")
    private Integer useBound = 0;

    /** 标签id集合 */
    @MpField(value = "tag_ids", columnType = "text", nullable = true, comment = "标签id集合")
    private String tagIds;

    /** 品牌id集合 */
    @MpField(value = "brand_ids", columnType = "text", nullable = true, comment = "品牌id集合")
    private String brandIds;

    /** 适用范围 */
    @MpField(value = "apply_scope", columnType = "text", nullable = true, comment = "适用范围")
    private String applyScope;

    /** 优惠券模板ID-第三方使用 */
    @MpField(value = "card_code", columnType = "string", nullable = true, comment = "优惠券模板ID-第三方使用")
    private String cardCode;

    /** 优惠券规则ID-第三方使用 */
    @MpField(value = "card_rule_code", columnType = "string", nullable = true, comment = "优惠券规则ID-第三方使用")
    private String cardRuleCode;

    /** 添加者类型：distributor */
    @MpField(value = "source_type", columnType = "string", length = 20, nullable = true, comment = "添加者类型：distributor")
    private String sourceType;

    /** 添加者ID: 如店铺ID */
    @MpField(value = "source_id", columnType = "bigint", nullable = true, comment = "添加者ID: 如店铺ID", defaultValue = "0")
    private Long sourceId = 0L;

    /** 达摩CRM卡券ID */
    @MpField(value = "dm_card_id", columnType = "string", nullable = true, comment = "达摩CRM卡券ID")
    private String dmCardId;

    /** 达摩CRM适用渠道。多选：offlineStore 线下门店；ThirdmicroMall 第三方商城 */
    @MpField(value = "dm_use_channel", columnType = "string", nullable = true, comment = "达摩CRM适用渠道")
    private String dmUseChannel;

    /** 券类型。mall:商城券;guide:导购专属券 */
    @MpField(value = "coupon_type", columnType = "string", length = 10, comment = "券类型。mall:商城券;guide:导购专属券", defaultValue = "mall")
    private String couponType = "mall";

    /** 导购发放数量 */
    @MpField(value = "guide_issue_quantity", columnType = "integer", comment = "导购发放数量", defaultValue = "0")
    private Integer guideIssueQuantity = 0;
}
