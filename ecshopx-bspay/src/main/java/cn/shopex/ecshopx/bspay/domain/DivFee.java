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

package cn.shopex.ecshopx.bspay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 分账金额表
 */
@Data
@MpTable(value = "bspay_div_fee", comment = "分账金额表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "ix_supplier_id", columns = {"supplier_id"}), @MpIndex(name = "ix_order_id", columns = {"order_id"}), @MpIndex(name = "ix_trade_id", columns = {"trade_id"}), @MpIndex(name = "ix_list", columns = {"company_id", "huifu_id"}), @MpIndex(name = "ix_merchant", columns = {"merchant_id"})})
public class DivFee {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 交易单号 */
    @MpField(value = "trade_id", columnType = "string", length = 64, comment = "交易单号")
    private String tradeId;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", length = 64, nullable = true, comment = "订单号")
    private String orderId;

    /** 企业ID */
    @MpField(value = "company_id", columnType = "string", comment = "企业ID")
    private String companyId;

    /** 店铺ID */
    @MpField(value = "distributor_id", columnType = "string", nullable = true, comment = "店铺ID")
    private String distributorId;

    /** 供应商ID */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商ID", defaultValue = "0")
    private Long supplierId = 0L;

    /** 操作者类型:distributor-店铺;dealer-经销;admin:超级管理员;supplier:经销商 */
    @MpField(value = "operator_type", columnType = "string", comment = "操作者类型:distributor-店铺;dealer-经销;admin:超级管理员;supplier:经销商")
    private String operatorType = "admin";

    /** 支付金额 */
    @MpField(value = "pay_fee", columnType = "integer", comment = "支付金额", defaultValue = "0")
    private Integer payFee = 0;

    /** 当前用户的分账金额，以分为单位 */
    @MpField(value = "div_fee", columnType = "integer", nullable = true, comment = "当前用户的分账金额，以分为单位", defaultValue = "0")
    private Integer divFee = 0;

    /** 汇付ID */
    @MpField(value = "huifu_id", columnType = "string", nullable = true, comment = "汇付ID")
    private String huifuId = "";

    /** 商户ID，记录分账对象是商户时的商户ID */
    @MpField(value = "merchant_id", columnType = "string", nullable = true, comment = "商户ID，记录分账对象是商户时的商户ID")
    private String merchantId;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer", comment = "创建时间")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
