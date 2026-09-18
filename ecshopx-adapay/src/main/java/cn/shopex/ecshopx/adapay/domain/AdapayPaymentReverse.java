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

package cn.shopex.ecshopx.adapay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * adapay支付撤销表
 */
@Data
@MpTable(value = "adapay_payment_reverse", comment = "adapay支付撤销表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_order_no", columns = {"order_no"})})
public class AdapayPaymentReverse {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 订单id */
    @MpField(value = "order_id", columnType = "string", comment = "订单id")
    private String orderId;

    /** Adapay生成的支付对象id 对应order表的transaction_id */
    @MpField(value = "payment_id", columnType = "string", comment = "Adapay生成的支付对象id 对应order表的transaction_id")
    private String paymentId;

    /** 支付撤销id */
    @MpField(value = "payment_reverse_id", columnType = "string", nullable = true, comment = "支付撤销id")
    private String paymentReverseId;

    /** 应用app_id */
    @MpField(value = "app_id", columnType = "string", comment = "应用app_id")
    private String appId;

    /** 支付撤销请求订单号 */
    @MpField(value = "order_no", columnType = "string", comment = "支付撤销请求订单号")
    private String orderNo;

    /** 撤销金额 单位：元 */
    @MpField(value = "reverse_amt", columnType = "string", comment = "撤销金额 单位：元")
    private String reverseAmt;

    /** 状态 */
    @MpField(value = "status", columnType = "string", comment = "状态")
    private String status;

    /** 请求参数 json */
    @MpField(value = "request_params", columnType = "text", comment = "请求参数 json")
    private String requestParams;

    /** 响应参数 json */
    @MpField(value = "response_params", columnType = "text", nullable = true, comment = "响应参数 json")
    private String responseParams;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
