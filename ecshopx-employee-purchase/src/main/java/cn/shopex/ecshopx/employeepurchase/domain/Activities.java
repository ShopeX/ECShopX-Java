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

package cn.shopex.ecshopx.employeepurchase.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 员工内购活动表 */
@Data
@MpTable(value = "employee_purchase_activities", comment = "员工内购活动表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"})})
public class Activities {

    /** 活动id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "活动id")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 店铺id,为0时表示商城的企业 */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺id,为0时表示商城的企业", defaultValue = "0")
    private Integer distributorId = 0;

    /** 操作id */
    @MpField(value = "operator_id", columnType = "integer", comment = "操作id", defaultValue = "0")
    private Integer operatorId = 0;

    /** 活动名称 */
    @MpField(value = "name", columnType = "string", length = 50, comment = "活动名称")
    private String name;

    /** 活动标题 */
    @MpField(value = "title", columnType = "string", length = 50, comment = "活动标题")
    private String title;

    /** 活动首页模版id */
    @MpField(value = "pages_template_id", columnType = "bigint", comment = "活动首页模版id")
    private Long pagesTemplateId;

    /** 活动图片 */
    @MpField(value = "pic", columnType = "string", comment = "活动图片")
    private String pic;

    /** 活动分享图片 */
    @MpField(value = "share_pic", columnType = "string", comment = "活动分享图片")
    private String sharePic;

    /** 活动列表海报 */
    @MpField(value = "list_pic", columnType = "string", length = 255, comment = "活动列表海报", defaultValue = "")
    private String listPic = "";

    /** 参与企业ID（多值存储） */
    @MpField(value = "enterprise_id", columnType = "simple_array", comment = "参与企业ID")
    private String enterpriseId;

    /** 活动展示(预热)时间 */
    @MpField(value = "display_time", columnType = "integer", comment = "活动展示(预热)时间")
    private Integer displayTime;

    /** 员工开始购买时间 */
    @MpField(value = "employee_begin_time", columnType = "integer", comment = "员工开始购买时间")
    private Integer employeeBeginTime;

    /** 员工结束购买时间 */
    @MpField(value = "employee_end_time", columnType = "integer", comment = "员工结束购买时间")
    private Integer employeeEndTime;

    /** 员工额度，以分为单位 */
    @MpField(value = "employee_limitfee", columnType = "integer", comment = "员工额度，以分为单位")
    private Integer employeeLimitfee;

    /** 亲友是否参与 */
    @MpField(value = "if_relative_join", columnType = "boolean", comment = "亲友是否参与", defaultValue = "False")
    private Boolean ifRelativeJoin = false;

    /** 员工邀请亲友上限 */
    @MpField(value = "invite_limit", columnType = "integer", nullable = true, comment = "员工邀请亲友上限", defaultValue = "0")
    private Integer inviteLimit = 0;

    /** 亲友开始购买时间 */
    @MpField(value = "relative_begin_time", columnType = "integer", nullable = true, comment = "亲友开始购买时间")
    private Integer relativeBeginTime;

    /** 亲友结束购买时间 */
    @MpField(value = "relative_end_time", columnType = "integer", nullable = true, comment = "亲友结束购买时间")
    private Integer relativeEndTime;

    /** 亲友是否共享员工额度 */
    @MpField(value = "if_share_limitfee", columnType = "boolean", nullable = true, comment = "亲友是否共享员工额度", defaultValue = "False")
    private Boolean ifShareLimitfee = false;

    /** 家属额度，以分为单位 */
    @MpField(value = "relative_limitfee", columnType = "integer", nullable = true, comment = "家属额度，以分为单位", defaultValue = "0")
    private Integer relativeLimitfee = 0;

    /** 起定金额，以分为单位 */
    @MpField(value = "minimum_amount", columnType = "integer", comment = "起定金额，以分为单位", defaultValue = "0")
    private Integer minimumAmount = 0;

    /** 活动后数小时关闭修改 */
    @MpField(value = "close_modify_hours_after_activity", columnType = "integer", comment = "活动后数小时关闭修改", defaultValue = "0")
    private Integer closeModifyHoursAfterActivity = 0;

    /** 状态 active:有效的 cancel:取消 pending:暂停 over:结束 */
    @MpField(value = "status", columnType = "string", comment = "状态 active:有效的 cancel:取消 pending:暂停 over:结束", defaultValue = "active")
    private String status = "active";

    /** 是否共享库存 */
    @MpField(value = "if_share_store", columnType = "boolean", comment = "是否共享库存", defaultValue = "False")
    private Boolean ifShareStore = false;

    /** 价格展示配置 */
    @MpField(value = "price_display_config", columnType = "json_array", nullable = true, comment = "价格展示配置")
    private String priceDisplayConfig;

    /** 优惠说明开关 */
    @MpField(value = "is_discount_description_enabled", columnType = "boolean", comment = "优惠说明开关", defaultValue = "False")
    private Boolean isDiscountDescriptionEnabled = false;

    /** 优惠说明 */
    @MpField(value = "discount_description", columnType = "string", length = 50, comment = "优惠说明")
    private String discountDescription = "";

    /** 是否开启口令通道 */
    @MpField(value = "is_passphrase_enabled", columnType = "boolean", comment = "是否开启口令通道", defaultValue = "False")
    private Boolean isPassphraseEnabled = false;

    /** 购买方式 cash/prepaid_point；旧活动为空 */
    @MpField(value = "purchase_mode", columnType = "string", length = 32, nullable = true, comment = "购买方式 cash/prepaid_point；旧活动为空")
    private String purchaseMode;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
