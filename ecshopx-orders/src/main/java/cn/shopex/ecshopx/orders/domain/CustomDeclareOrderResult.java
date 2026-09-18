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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 订单附加信息提交信息接口请求结果表 */
@Data
@MpTable(value = "custom_declare_order_result", comment = "订单附加信息提交信息接口请求结果表")
public class CustomDeclareOrderResult {

    /** 订单号 */
    @MpId(value = "order_id", type = IdType.INPUT, columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 交易单号 */
    @MpField(value = "trade_id", columnType = "string", length = 64, comment = "交易单号")
    private String tradeId;

    /** 状态码 UNDECLARED未申报 SUBMITTED申报已提交 PROCESSING申报中 SUCCESS申报成功 FAIL申报失败 EXCEPT海关接口异常 */
    @MpField(value = "state", columnType = "string", comment = "状态码")
    private String state;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 微信支付返回的订单号 */
    @MpField(value = "transaction_id", columnType = "string", length = 28, comment = "微信支付返回的订单号")
    private String transactionId;

    /** 子订单号 */
    @MpField(value = "sub_order_no", columnType = "string", length = 64, nullable = true, comment = "子订单号")
    private String subOrderNo;

    /** 微信子订单号 */
    @MpField(value = "sub_order_id", columnType = "string", length = 64, nullable = true, comment = "微信子订单号")
    private String subOrderId;

    /** 最后更新时间 */
    @MpField(value = "modify_time", columnType = "string", length = 14, comment = "最后更新时间")
    private String modifyTime;

    /** 订购人和支付人身份信息校验结果 UNCHECKED商户未上传 SAME一致 DIFFERENT不一致 */
    @MpField(value = "cert_check_result", columnType = "string", length = 256, comment = "订购人和支付人身份信息校验结果")
    private String certCheckResult;

    /** 验证机构 银联-UNIONPAY 网联-NETSUNION 其他-OTHERS */
    @MpField(value = "verify_department", columnType = "string", length = 16, comment = "验证机构")
    private String verifyDepartment;

    /** 验核机构交易流水号 */
    @MpField(value = "verify_department_trade_id", columnType = "string", length = 64, comment = "验核机构交易流水号")
    private String verifyDepartmentTradeId;

    /** 请求时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "请求时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
