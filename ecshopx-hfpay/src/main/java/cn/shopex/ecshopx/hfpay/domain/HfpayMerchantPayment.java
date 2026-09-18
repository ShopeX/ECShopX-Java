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
 * 汇付平台转账记录表
 */
@Data
@MpTable(value = "hfpay_merchant_payment", comment = "汇付平台转账记录表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_rel_scene_id_rel_scene_name", columns = {"rel_scene_id", "rel_scene_name"})})
public class HfpayMerchantPayment {

    /**
     * 汇付取现银行卡表id
     */
    @MpId(value = "hfpay_merchant_payment_id", type = IdType.AUTO, columnType = "bigint", comment = "汇付取现银行卡表id")
    private Long hfpayMerchantPaymentId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 业务场景值id */
    @MpField(value = "rel_scene_id", columnType = "bigint", comment = "业务场景值id")
    private Long relSceneId;

    /** 业务场景值名称 */
    @MpField(value = "rel_scene_name", columnType = "string", comment = "业务场景值名称")
    private String relSceneName;

    /** 汇付平台客户号 */
    @MpField(value = "mer_cust_id", columnType = "string", comment = "汇付平台客户号")
    private String merCustId;

    /** 汇付客户号 */
    @MpField(value = "user_cust_id", columnType = "string", comment = "汇付客户号")
    private String userCustId;

    /** 汇付账户号 */
    @MpField(value = "acct_id", columnType = "string", comment = "汇付账户号")
    private String acctId;

    /** 转账金额 */
    @MpField(value = "trans_amt", columnType = "bigint", comment = "转账金额")
    private Long transAmt;

    /**
     * 状态 0 未提交 1转账成功 2转账失败，默认 0
     */
    @MpField(value = "status", columnType = "integer", comment = "状态 0 未提交 1转账成功 2转账失败", defaultValue = "0")
    private Integer status = 0;

    /** 汇付接口请求order_id，可为空 */
    @MpField(value = "hf_order_id", columnType = "string", nullable = true, comment = "汇付接口请求order_id")
    private String hfOrderId;

    /** 汇付接口请求order_date，可为空 */
    @MpField(value = "hf_order_date", columnType = "string", nullable = true, comment = "汇付接口请求order_date")
    private String hfOrderDate;

    /** 汇付接口返回码，可为空 */
    @MpField(value = "resp_code", columnType = "string", nullable = true, comment = "汇付接口返回码")
    private String respCode;

    /** 汇付接口返回码描述，可为空 */
    @MpField(value = "resp_desc", columnType = "string", nullable = true, comment = "汇付接口返回码描述")
    private String respDesc;

    /** 创建时间 */
    @MpField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
