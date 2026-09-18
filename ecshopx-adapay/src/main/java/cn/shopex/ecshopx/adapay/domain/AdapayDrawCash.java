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
 * adapay提现表
 */
@Data
@MpTable(value = "adapay_draw_cash", comment = "adapay提现表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_order_no", columns = {"order_no"}), @MpIndex(name = "idx_adapay_member_id", columns = {"adapay_member_id"})})
public class AdapayDrawCash {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 提现账号id */
    @MpField(value = "operator_id", columnType = "integer", nullable = true, comment = "提现账号id", defaultValue = "0")
    private Integer operatorId = 0;

    /** 操作账号类型:distributor-店铺;dealer-经销;admin:超级管理员 */
    @MpField(value = "operator_type", columnType = "string", comment = "操作账号类型:distributor-店铺;dealer-经销;admin:超级管理员")
    private String operatorType = "admin";

    /** 操作人 */
    @MpField(value = "operator", columnType = "string", nullable = true, comment = "操作人")
    private String operator;

    /** 应用app_id */
    @MpField(value = "app_id", columnType = "string", length = 100, comment = "应用app_id")
    private String appId;

    /** 请求订单号 */
    @MpField(value = "order_no", columnType = "string", length = 64, comment = "请求订单号")
    private String orderNo;

    /** 取现对象 id */
    @MpField(value = "cash_id", columnType = "string", length = 64, nullable = true, comment = "取现对象 id")
    private String cashId;

    /** 提现银行卡号 */
    @MpField(value = "bank_card_id", columnType = "string", length = 100, nullable = true, comment = "提现银行卡号")
    private String bankCardId;

    /** 银行卡对应的户名 */
    @MpField(value = "bank_card_name", columnType = "string", length = 100, nullable = true, comment = "银行卡对应的户名")
    private String bankCardName;

    /** 取现类型：T1-T+1取现；D1-D+1取现；D0-即时取现 */
    @MpField(value = "cash_type", columnType = "string", length = 10, comment = "取现类型：T1-T+1取现；D1-D+1取现；D0-即时取现")
    private String cashType;

    /** 取现金额，必须大于0，人民币为分 */
    @MpField(value = "cash_amt", columnType = "string", comment = "取现金额，必须大于0，人民币为分")
    private String cashAmt;

    /** 汇付账号id */
    @MpField(value = "adapay_member_id", columnType = "string", comment = "汇付账号id")
    private String adapayMemberId;

    /** 提现状态 */
    @MpField(value = "status", columnType = "string", comment = "提现状态")
    private String status;

    /** 请求参数 json */
    @MpField(value = "request_params", columnType = "text", comment = "请求参数 json")
    private String requestParams;

    /** 回调参数 json */
    @MpField(value = "response_params", columnType = "text", nullable = true, comment = "回调参数 json")
    private String responseParams;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 备注 */
    @MpField(value = "remark", columnType = "string", nullable = true, comment = "备注")
    private String remark = "";

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
