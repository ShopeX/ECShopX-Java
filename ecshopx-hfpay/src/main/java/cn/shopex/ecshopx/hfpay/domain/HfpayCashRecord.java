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

package cn.shopex.ecshopx.hfpay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 汇付取现记录表
 */
@Data
@MpTable(value = "hfpay_cash_record", comment = "汇付取现记录表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class HfpayCashRecord {

    /** 汇付取现记录表id */
    @MpId(value = "hfpay_cash_record_id", type = IdType.AUTO, columnType = "bigint", comment = "汇付取现记录表id")
    private Long hfpayCashRecordId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 分销商id，可为空，默认 0 */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", comment = "订单号")
    private String orderId;

    /** 用户id，可为空，默认 0 */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id", defaultValue = "0")
    private Long userId = 0L;

    /** 操作人ID，可为空，默认 0 */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "操作人ID", defaultValue = "0")
    private Long operatorId = 0L;

    /** 汇付商户客户号，可为空 */
    @MpField(value = "user_cust_id", columnType = "string", nullable = true, comment = "汇付商户客户号")
    private String userCustId;

    /** 取现金额 */
    @MpField(value = "trans_amt", columnType = "integer", comment = "取现金额")
    private Integer transAmt;

    /** 取现方式 T0：T0取现; T1：T1取现 D1：D1取现 */
    @MpField(value = "cash_type", columnType = "string", comment = "取现方式 T0：T0取现; T1：T1取现 D1：D1取现")
    private String cashType;

    /** 取现绑卡，可为空 */
    @MpField(value = "bind_card_id", columnType = "string", nullable = true, comment = "取现绑卡")
    private String bindCardId;

    /** 实际到账金额，可为空 */
    @MpField(value = "real_trans_amt", columnType = "integer", nullable = true, comment = "实际到账金额")
    private Integer realTransAmt;

    /** 取现手续费，可为空 */
    @MpField(value = "fee_amt", columnType = "integer", nullable = true, comment = "取现手续费")
    private Integer feeAmt;

    /**
     * 取现状态 0 未提交 1已提交 2取现成功 3取现失败，默认 0
     */
    @MpField(value = "cash_status", columnType = "integer", comment = "取现状态 0 未提交 1已提交 2取现成功 3取现失败", defaultValue = "0")
    private Integer cashStatus = 0;

    /** 汇付接口返回码，可为空 */
    @MpField(value = "resp_code", columnType = "string", nullable = true, comment = "汇付接口返回码")
    private String respCode;

    /** 汇付接口返回码描述，可为空 */
    @MpField(value = "resp_desc", columnType = "string", nullable = true, comment = "汇付接口返回码描述")
    private String respDesc;

    /** 汇付接口请求order_id，可为空 */
    @MpField(value = "hf_order_id", columnType = "string", nullable = true, comment = "汇付接口请求order_id")
    private String hfOrderId;

    /** 汇付接口请求order_date，可为空 */
    @MpField(value = "hf_order_date", columnType = "string", nullable = true, comment = "汇付接口请求order_date")
    private String hfOrderDate;

    /** 创建时间 */
    @MpField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
