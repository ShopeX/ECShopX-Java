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

/** 订单发票表 */
@Data
@MpTable(value = "orders_invoice", comment = "订单发票表", indexes = {@MpIndex(name = "idx_invoice_apply_bn", columns = {"invoice_apply_bn"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_order_id", columns = {"order_id"})})
public class OrderInvoice {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", length = 64, comment = "自增id")
    private Long id;

    /** 发票申请单号 */
    @MpField(value = "invoice_apply_bn", columnType = "string", length = 64, comment = "发票申请单号")
    private String invoiceApplyBn;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 区域id */
    @MpField(value = "regionauth_id", columnType = "bigint", nullable = true, comment = "区域id")
    private Long regionauthId;

    /** 订单id，多订单以英文逗号分隔 */
    @MpField(value = "order_id", columnType = "string", length = 255, comment = "订单id，多订单以英文逗号分隔")
    private String orderId;

    /** 开票类型，企业enterprise,和个人individual */
    @MpField(value = "invoice_type", columnType = "string", length = 20, comment = "开票类型，企业enterprise,和个人individual")
    private String invoiceType;

    /** 开票类型编码，01:增值税专用发票,02:增值税普通发票 */
    @MpField(value = "invoice_type_code", columnType = "string", length = 20, comment = "开票类型编码，01:增值税专用发票,02:增值税普通发票")
    private String invoiceTypeCode;

    /** 公司抬头 */
    @MpField(value = "company_title", columnType = "string", length = 255, comment = "公司抬头")
    private String companyTitle;

    /** 公司税号 */
    @MpField(value = "company_tax_number", columnType = "string", length = 64, nullable = true, comment = "公司税号")
    private String companyTaxNumber;

    /** 公司地址 */
    @MpField(value = "company_address", columnType = "string", length = 255, nullable = true, comment = "公司地址")
    private String companyAddress;

    /** 公司电话 */
    @MpField(value = "company_telephone", columnType = "string", length = 20, nullable = true, comment = "公司电话")
    private String companyTelephone;

    /** 开户银行 */
    @MpField(value = "bank_name", columnType = "string", length = 255, nullable = true, comment = "开户银行")
    private String bankName;

    /** 开户账号 */
    @MpField(value = "bank_account", columnType = "string", length = 64, nullable = true, comment = "开户账号")
    private String bankAccount;

    /** 电子邮箱 */
    @MpField(value = "email", columnType = "string", length = 255, nullable = true, comment = "电子邮箱")
    private String email;

    /** 手机号码 */
    @MpField(value = "mobile", columnType = "string", length = 20, nullable = true, comment = "手机号码")
    private String mobile;

    /** 开票状态：待开票：pending，开票中：inProgress，开票成功：success，已作废：waste，开票失败：failed */
    @MpField(value = "invoice_status", columnType = "string", length = 20, comment = "开票状态：待开票：pending，开票中：inProgress，开票成功：success，已作废：waste，开票失败：failed", defaultValue = "pending")
    private String invoiceStatus = "pending";

    /** 重试次数 */
    @MpField(value = "try_times", columnType = "integer", comment = "重试次数", defaultValue = "0")
    private Integer tryTimes = 0;

    /** 开票金额，以分为单位 */
    @MpField(value = "invoice_amount", columnType = "integer", nullable = true, comment = "开票金额，以分为单位", defaultValue = "0")
    private Integer invoiceAmount = 0;

    /** 发票文件地址 */
    @MpField(value = "invoice_file_url", columnType = "string", length = 255, nullable = true, comment = "发票文件地址")
    private String invoiceFileUrl;

    /** 红票文件地址 */
    @MpField(value = "invoice_file_url_red", columnType = "string", length = 255, nullable = true, comment = "红票文件地址")
    private String invoiceFileUrlRed;

    /** 开票类型：线上和线下 */
    @MpField(value = "invoice_method", columnType = "string", length = 20, comment = "开票类型：线上和线下", defaultValue = "online")
    private String invoiceMethod = "online";

    /** 开票来源：user客户端,oms */
    @MpField(value = "invoice_source", columnType = "string", length = 20, comment = "开票来源：user客户端,oms")
    private String invoiceSource = "user";

    /** 备注 */
    @MpField(value = "remark", columnType = "string", length = 255, nullable = true, comment = "备注")
    private String remark;

    /** 是否推送OMS，0是未推送，1是已推送 */
    @MpField(value = "is_oms", columnType = "integer", comment = "是否推送OMS，0是未推送，1是已推送", defaultValue = "0")
    private Integer isOms = 0;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;

    /** 订单完成时间 */
    @MpField(value = "end_time", columnType = "integer", nullable = true, comment = "订单完成时间")
    private Integer endTime;

    /** 售后截止时间 */
    @MpField(value = "close_aftersales_time", columnType = "integer", nullable = true, comment = "售后截止时间")
    private Integer closeAftersalesTime;

    /** 查询内容 */
    @MpField(value = "query_content", columnType = "json_array", nullable = true, comment = "查询内容")
    private String queryContent;

    /** 冲红内容 */
    @MpField(value = "red_content", columnType = "json_array", nullable = true, comment = "冲红内容")
    private String redContent;

    /** 发票流水号 */
    @MpField(value = "serial_no", columnType = "string", length = 255, nullable = true, comment = "发票流水号")
    private String serialNo;

    /** 红冲流水号 */
    @MpField(value = "red_serial_no", columnType = "string", length = 255, nullable = true, comment = "红冲流水号")
    private String redSerialNo;

    /** 红冲申请单号 */
    @MpField(value = "red_apply_bn", columnType = "string", length = 255, nullable = true, comment = "红冲申请单号")
    private String redApplyBn;

    /** 订单店铺id */
    @MpField(value = "order_shop_id", columnType = "string", length = 20, nullable = true, comment = "订单店铺id")
    private String orderShopId;

    /** 用户卡号 */
    @MpField(value = "user_card_code", columnType = "string", length = 50, nullable = true, comment = "用户卡号")
    private String userCardCode;
}
