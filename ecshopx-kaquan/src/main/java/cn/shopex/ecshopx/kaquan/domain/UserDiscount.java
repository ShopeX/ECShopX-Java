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

/** 用户领取的优惠券表 */
@Data
@MpTable(value = "kaquan_user_discount", comment = "用户领取的优惠券表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_code", columns = {"code"}), @MpIndex(name = "idx_source_type", columns = {"source_type"}), @MpIndex(name = "idx_userid_status_companyid", columns = {"user_id", "status", "company_id"}), @MpIndex(name = "idx_userid_enddate", columns = {"user_id", "end_date"}), @MpIndex(name = "idx_status_expiredtime", columns = {"status", "expired_time"}), @MpIndex(name = "idx_cardid_companyid_userid", columns = {"card_id", "company_id", "user_id"}), @MpIndex(name = "idx_dm_card_code", columns = {"dm_card_code"}), @MpIndex(name = "idx_salesperson_code", columns = {"salesperson_code"})})
public class UserDiscount {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", length = 64, comment = "自增id")
    private Long id;

    /** 用户的唯一标识 */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户的唯一标识")
    private Long userId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;

    /** 微信用户领取的卡券 id  */
    @MpField(value = "card_id", columnType = "bigint", length = 40, comment = "微信用户领取的卡券 id ")
    private Long cardId = 0L;

    /** 卡券 code 序列号 */
    @MpField(value = "code", columnType = "string", length = 30, comment = "卡券 code 序列号")
    private String code;

    /** 卡券来源类型，可选值有 local:本地卡券,wechat:微信卡券 */
    @MpField(value = "source_type", columnType = "string", length = 30, comment = "卡券来源类型，可选值有 local:本地卡券,wechat:微信卡券", defaultValue = "local")
    private String sourceType = "local";

    /** 用户领取的优惠券使用状态{1:未使用,2:已核销,3:已转赠,5:已过期,6:作废,10:已使用(兑换券)}; */
    @MpField(value = "status", columnType = "integer", nullable = true, comment = "用户领取的优惠券使用状态{1:未使用,2:已核销,3:已转赠,5:已过期,6:作废,10:已使用(兑换券)};", defaultValue = "1")
    private Integer status = 1;

    /** 优惠券类型,可选值有 discount:折扣券，cash:代金券，new_gift:兑换券 */
    @MpField(value = "card_type", columnType = "string", nullable = true, comment = "优惠券类型,可选值有 discount:折扣券，cash:代金券，new_gift:兑换券", defaultValue = "1")
    private String cardType;

    /** 优惠券适用平台（mall:线上商城专用, store:门店专用） */
    @MpField(value = "use_platform", columnType = "string", nullable = true, comment = "优惠券适用平台（mall:线上商城专用, store:门店专用）", defaultValue = "store")
    private String usePlatform = "store";

    /** 有效期开始时间 */
    @MpField(value = "begin_date", columnType = "integer", comment = "有效期开始时间")
    private Integer beginDate;

    /** 有效期结束时间 */
    @MpField(value = "end_date", columnType = "integer", comment = "有效期结束时间")
    private Integer endDate;

    /** 优惠券获取时间 */
    @MpField(value = "get_date", columnType = "integer", comment = "优惠券获取时间")
    private Integer getDate;

    /** 卡券名,最大9个汉字 */
    @MpField(value = "title", columnType = "string", length = 27, comment = "卡券名,最大9个汉字")
    private String title;

    /** 券颜色值 */
    @MpField(value = "color", columnType = "string", length = 16, comment = "券颜色值")
    private String color;

    /** 折扣券打折额度（百分比) */
    @MpField(value = "discount", columnType = "integer", nullable = true, comment = "折扣券打折额度（百分比)", defaultValue = "0")
    private Integer discount = 0;

    /** 代金券起用金额 */
    @MpField(value = "least_cost", columnType = "integer", nullable = true, comment = "代金券起用金额", defaultValue = "0")
    private Integer leastCost = 0;

    /** 代金券减免金额 or 兑换券起用金额 */
    @MpField(value = "reduce_cost", columnType = "integer", nullable = true, comment = "代金券减免金额 or 兑换券起用金额", defaultValue = "0")
    private Integer reduceCost = 0;

    /** 门店shop_id;线下门店时,必填; */
    @MpField(value = "rel_shops_ids", columnType = "text", nullable = true, comment = "门店shop_id;线下门店时,必填;")
    private String relShopsIds;

    /** 使用商品 */
    @MpField(value = "rel_item_ids", columnType = "text", nullable = true, comment = "使用商品")
    private String relItemIds;

    /** 使用商品 */
    @MpField(value = "rel_distributor_ids", columnType = "text", nullable = true, comment = "使用商品")
    private String relDistributorIds;

    /** 核销来源 */
    @MpField(value = "consume_source", columnType = "string", nullable = true, comment = "核销来源")
    private String consumeSource;

    /** 领取场景值 */
    @MpField(value = "get_outer_str", columnType = "string", nullable = true, comment = "领取场景值")
    private String getOuterStr;

    /** 核销卡券的门店名称 */
    @MpField(value = "location_name", columnType = "string", nullable = true, comment = "核销卡券的门店名称")
    private String locationName;

    /** 卡券核销员 */
    @MpField(value = "staff_open_id", columnType = "string", nullable = true, comment = "卡券核销员")
    private String staffOpenId;

    /** 自助核销的验证码 */
    @MpField(value = "verify_code", columnType = "string", nullable = true, comment = "自助核销的验证码")
    private String verifyCode;

    /** 自助核销时备注金额 */
    @MpField(value = "remark_amount", columnType = "string", nullable = true, comment = "自助核销时备注金额")
    private String remarkAmount;

    /** 核销渠道 */
    @MpField(value = "consume_outer_str", columnType = "string", nullable = true, comment = "核销渠道")
    private String consumeOuterStr;

    /** 微信支付交易订单号,买单核销专用 */
    @MpField(value = "trans_id", columnType = "string", nullable = true, comment = "微信支付交易订单号,买单核销专用")
    private String transId;

    /** 实付金额,买单核销专用 */
    @MpField(value = "fee", columnType = "string", nullable = true, comment = "实付金额,买单核销专用")
    private String fee;

    /** 应付金额,买单核销专用 */
    @MpField(value = "original_fee", columnType = "string", nullable = true, comment = "应付金额,买单核销专用")
    private String originalFee;

    /** 当前卡券核销的门店ID */
    @MpField(value = "location_id", columnType = "string", nullable = true, comment = "当前卡券核销的门店ID")
    private String locationId;

    /** 可被核销的方式，默认 QUICK */
    @MpField(value = "use_scenes", columnType = "string", nullable = true, comment = "可被核销的方式", defaultValue = "QUICK")
    private String useScenes = "QUICK";

    /** 代金券最高消费限额 */
    @MpField(value = "most_cost", columnType = "integer", nullable = true, comment = "代金券最高消费限额", defaultValue = "0")
    private Integer mostCost = 0;

    /**
     * 使用条件字段。可含 accept_category、reject_category、least_cost、object_use_for、can_use_with_other_discount 等（序列化/JSON）
     */
    @MpField(value = "use_condition", columnType = "array", nullable = true, comment = "使用条件字段")
    private String useCondition;

    /** 是否为转赠领取 */
    @MpField(value = "is_give_by_friend", columnType = "boolean", nullable = true, comment = "是否为转赠领取", defaultValue = "False")
    private Boolean isGiveByFriend = Boolean.FALSE;

    /** 转赠之后,旧的 code 序列号 */
    @MpField(value = "old_code", columnType = "string", nullable = true, comment = "转赠之后,旧的 code 序列号")
    private String oldCode;

    /** 转赠卡券时接收方 open_id */
    @MpField(value = "friend_open_id", columnType = "string", nullable = true, comment = "转赠卡券时接收方 open_id")
    private String friendOpenId;

    /** 转赠时是否退回 */
    @MpField(value = "is_return_back", columnType = "boolean", nullable = true, comment = "转赠时是否退回", defaultValue = "False")
    private Boolean isReturnBack = Boolean.FALSE;

    /** 是否群转赠 */
    @MpField(value = "is_chat_room", columnType = "boolean", nullable = true, comment = "是否群转赠", defaultValue = "False")
    private Boolean isChatRoom = Boolean.FALSE;

    /** 导购id */
    @MpField(value = "salesperson_id", columnType = "bigint", nullable = true, comment = "导购id")
    private Long salespersonId;

    /** 导购编号(employee_number/work_userid) */
    @MpField(value = "salesperson_code", columnType = "string", length = 100, nullable = true, comment = "导购编号(employee_number/work_userid)")
    private String salespersonCode;

    /** 是否可以多次使用,仅记录无业务 */
    @MpField(value = "use_limited", columnType = "integer", nullable = true, comment = "是否可以多次使用,仅记录无业务", defaultValue = "0")
    private Integer useLimited = 0;

    /** 剩余使用次数,仅记录无业务 */
    @MpField(value = "remain_times", columnType = "integer", nullable = true, comment = "剩余使用次数,仅记录无业务", defaultValue = "1")
    private Integer remainTimes = 1;

    /** 适用范围: 0:全场可用,1:指定商品可用,2:指定分类可用,3:指定商品标签可用,4:指定商品品牌可用 */
    @MpField(value = "use_bound", columnType = "integer", comment = "适用范围: 0:全场可用,1:指定商品可用,2:指定分类可用,3:指定商品标签可用,4:指定商品品牌可用", defaultValue = "0")
    private Integer useBound = 0;

    /** 可使用的类目 */
    @MpField(value = "rel_category_ids", columnType = "text", nullable = true, comment = "可使用的类目")
    private String relCategoryIds;

    /** 适用范围 */
    @MpField(value = "apply_scope", columnType = "text", nullable = true, comment = "适用范围")
    private String applyScope;

    /** 兑换券使用时间 */
    @MpField(value = "used_time", columnType = "integer", comment = "兑换券使用时间", defaultValue = "0")
    private Integer usedTime = 0;

    /** 兑换券过期时间 */
    @MpField(value = "expired_time", columnType = "integer", comment = "兑换券过期时间", defaultValue = "0")
    private Integer expiredTime = 0;

    /** 参与活动名称 */
    @MpField(value = "activity_name", columnType = "string", length = 100, nullable = true, comment = "参与活动名称")
    private String activityName;

    /** 达摩CRM会员卡券code */
    @MpField(value = "dm_card_code", columnType = "string", length = 100, nullable = true, comment = "达摩CRM会员卡券code")
    private String dmCardCode;
}
