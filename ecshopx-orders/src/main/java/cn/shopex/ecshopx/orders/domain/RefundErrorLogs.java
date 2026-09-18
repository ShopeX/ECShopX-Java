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

/** 退款错误日志 */
@Data
@MpTable(value = "refund_error_logs", comment = "退款错误日志", indexes = {@MpIndex(name = "idx_company", columns = {"company_id"}), @MpIndex(name = "idx_merchant_id", columns = {"merchant_id"}), @MpIndex(name = "idx_supplier_id", columns = {"supplier_id"})})
public class RefundErrorLogs {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商id", defaultValue = "0")
    private Long supplierId = 0L;

    /** 小程序appid */
    @MpField(value = "wxa_appid", columnType = "string", nullable = true, comment = "小程序appid")
    private String wxaAppid;

    /** data数据json格式 */
    @MpField(value = "data_json", columnType = "text", nullable = true, comment = "data数据json格式")
    private String dataJson;

    /** 错误状态 */
    @MpField(value = "status", columnType = "string", length = 20, nullable = true, comment = "错误状态")
    private String status;

    /** 错误码 */
    @MpField(value = "error_code", columnType = "string", length = 500, nullable = true, comment = "错误码")
    private String errorCode;

    /** 错误描述 */
    @MpField(value = "error_desc", columnType = "text", nullable = true, comment = "错误描述")
    private String errorDesc;

    /** 是否重新提交 */
    @MpField(value = "is_resubmit", columnType = "boolean", nullable = true, comment = "是否重新提交", defaultValue = "False")
    private Boolean isResubmit = false;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;
}
