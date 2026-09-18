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
 * 斗拱支付确认表
 */
@Data
@MpTable(value = "bspay_paymemt_confirm", comment = "斗拱支付确认表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_order_no", columns = {"order_no"}), @MpIndex(name = "distributor_id", columns = {"distributor_id"})})
public class PaymemtConfirm {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 订单id */
    @MpField(value = "order_id", columnType = "string", comment = "订单id")
    private String orderId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "店铺id")
    private Long distributorId;

    /** 斗拱生成的交易订单号 对应order表的transaction_id */
    @MpField(value = "payment_id", columnType = "string", nullable = true, comment = "斗拱生成的交易订单号 对应order表的transaction_id")
    private String paymentId;

    /** 支付确认id */
    @MpField(value = "payment_confirmation_id", columnType = "string", nullable = true, comment = "支付确认id")
    private String paymentConfirmationId;

    /** 支付确认请求订单号 */
    @MpField(value = "order_no", columnType = "string", nullable = true, comment = "支付确认请求订单号")
    private String orderNo;

    /** 确认金额 单位：元 */
    @MpField(value = "confirm_amt", columnType = "string", nullable = true, comment = "确认金额 单位：元")
    private String confirmAmt;

    /** 分账对象信息列表 json */
    @MpField(value = "div_members", columnType = "text", nullable = true, comment = "分账对象信息列表 json")
    private String divMembers;

    /** 交易状态 */
    @MpField(value = "status", columnType = "string", nullable = true, comment = "交易状态")
    private String status;

    /** 请求参数 json */
    @MpField(value = "request_params", columnType = "text", nullable = true, comment = "请求参数 json")
    private String requestParams;

    /** 响应参数 json */
    @MpField(value = "response_params", columnType = "text", nullable = true, comment = "响应参数 json")
    private String responseParams;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer", comment = "创建时间")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
