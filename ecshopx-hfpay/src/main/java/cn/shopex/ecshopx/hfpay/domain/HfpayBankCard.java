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
 * 汇付取现银行卡表
 */
@Data
@MpTable(value = "hfpay_bank_card", comment = "汇付取现银行卡表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"})})
public class HfpayBankCard {

    /** 汇付取现银行卡表id */
    @MpId(value = "hfpay_bank_card_id", type = IdType.AUTO, columnType = "bigint", comment = "汇付取现银行卡表id")
    private Long hfpayBankCardId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 分销商id，可为空 */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "分销商id")
    private Long distributorId;

    /** 用户id，可为空 */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 用户客户号，可为空 */
    @MpField(value = "user_cust_id", columnType = "string", nullable = true, comment = "用户客户号")
    private String userCustId;

    /**
     * 绑卡类型，可为空。0 对公；1 对私
     */
    @MpField(value = "card_type", columnType = "string", nullable = true, comment = "绑卡类型")
    private String cardType;

    /** 银行代号，可为空 */
    @MpField(value = "bank_id", columnType = "string", nullable = true, comment = "银行代号")
    private String bankId;

    /** 银行名称，可为空 */
    @MpField(value = "bank_name", columnType = "string", nullable = true, comment = "银行名称")
    private String bankName;

    /** 银行卡号，可为空 */
    @MpField(value = "card_num", columnType = "string", nullable = true, comment = "银行卡号")
    private String cardNum;

    /** 汇付绑定id，可为空 */
    @MpField(value = "bind_card_id", columnType = "string", nullable = true, comment = "汇付绑定id")
    private String bindCardId;

    /**
     * 是否取现卡，可为空，默认 1。1 取现卡；2 非取现卡
     */
    @MpField(value = "is_cash", columnType = "string", nullable = true, comment = "是否取现卡", defaultValue = "1")
    private String isCash = "1";

    /** 创建时间 */
    @MpField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
