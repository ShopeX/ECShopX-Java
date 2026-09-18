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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 问诊单 */
@Data
@MpTable(value = "orders_diagnosis", comment = "问诊单", indexes = {@MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "idx_status", columns = {"status"})})
public class OrdersDiagnosis {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", length = 64, comment = "自增id")
    private Long id;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", length = 64, comment = "订单号")
    private String orderId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 580门店Id */
    @MpField(value = "kuaizhen_store_id", columnType = "bigint", comment = "580门店Id")
    private Long kuaizhenStoreId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id")
    private Long distributorId;

    /** 服务类型：0 图文，1 视频 */
    @MpField(value = "service_type", columnType = "smallint", comment = "服务类型，0为图文，1为视频")
    private Integer serviceType;

    /** 是否需要审方：0 不需要，1 需要 */
    @MpField(value = "is_examine", columnType = "smallint", comment = "是否需要审方（0为不需要，1为需要）")
    private Integer isExamine;

    /** 用药人是否孕妇：0 否，1 是 */
    @MpField(value = "is_pregnant_woman", columnType = "smallint", comment = "用药人是否孕妇（0为否，1为是）")
    private Integer isPregnantWoman;

    /** 用药人是否哺乳期：0 否，1 是 */
    @MpField(value = "is_lactation", columnType = "smallint", comment = "用药人是否哺乳期0为否，1为是")
    private Integer isLactation;

    /** 来源：0 微信小程序，1 APP，2 H5，3 支付宝小程序 */
    @MpField(value = "souce_from", columnType = "smallint", comment = "来源（0为微信小程序，1为APP，2为H5，3为支付宝小程序）", defaultValue = "0")
    private Integer souceFrom = 0;

    /** 用药人姓名 */
    @MpField(value = "user_family_name", columnType = "string", comment = "用药人姓名")
    private String userFamilyName;

    /** 用药人身份证号 */
    @MpField(value = "user_family_id_card", columnType = "string", comment = "用药人身份证号")
    private String userFamilyIdCard;

    /** 用药人年龄 */
    @MpField(value = "user_family_age", columnType = "integer", comment = "用药人年龄")
    private Integer userFamilyAge;

    /** 用药人性别：1 男，2 女 */
    @MpField(value = "user_family_gender", columnType = "smallint", comment = "用药人性别1-男，2-女")
    private Integer userFamilyGender;

    /** 用药人手机号码 */
    @MpField(value = "user_family_phone", columnType = "string", comment = "用药人手机号码")
    private String userFamilyPhone;

    /** 用药人与问诊人关系：1 本人，2 父母，3 配偶，4 子女，5 其他 */
    @MpField(value = "relationship", columnType = "smallint", comment = "用药人与问诊人关系(1本人 2父母 3配偶 4子女 5其他)")
    private Integer relationship;

    /** AI问诊前5道题 */
    @MpField(value = "before_ai_data_list", columnType = "text", nullable = true, comment = "AI问诊前5道题")
    private String beforeAiDataList;

    /** 开方状态：1 未开方，2 已开方，3 医生拒绝开方 */
    @MpField(value = "prescription_status", columnType = "smallint", comment = "开方状态，1未开方，2已开方，3医生拒绝开方", defaultValue = "1")
    private Integer prescriptionStatus;

    /** 拒绝开方原因 */
    @MpField(value = "prescription_refuse_reason", columnType = "string", comment = "拒绝开方原因")
    private String prescriptionRefuseReason;

    /** 跳转问诊H5页面地址 */
    @MpField(value = "location_url", columnType = "text", comment = "跳转问诊H5页面地址")
    private String locationUrl;

    /** 问诊单状态：1 进行中，2 已完成，5 AI问诊时取消 */
    @MpField(value = "status", columnType = "smallint", comment = "问诊单状态（1为进行中，2为已完成,5-AI问诊时取消）", defaultValue = "1")
    private Integer status;

    /** 问诊结束时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "问诊结束时间", defaultValue = "0")
    private Integer endTime;

    /** 问诊取消时间 */
    @MpField(value = "cancel_time", columnType = "integer", comment = "问诊取消时间", defaultValue = "0")
    private Integer cancelTime;

    /** 医生科室 */
    @MpField(value = "doctor_office", columnType = "string", comment = "医生科室")
    private String doctorOffice;

    /** 医生姓名 */
    @MpField(value = "doctor_name", columnType = "string", comment = "医生姓名")
    private String doctorName;

    /** 互联网医院名称 */
    @MpField(value = "hospital_name", columnType = "string", comment = "互联网医院名称")
    private String hospitalName;

    /** 首诊信息 */
    @MpField(value = "first_visit_list", columnType = "text", nullable = true, comment = "首诊信息")
    private String firstVisitList;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
