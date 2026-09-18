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

package cn.shopex.ecshopx.community.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 团长提现表
 */
@Data
@MpTable(value = "community_chief_cash_withdrawal", comment = "团长提现表")
public class CommunityChiefCashWithdrawal {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id，为 0 时表示平台的团长申请 */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺id,为0时表示平台的团长申请", defaultValue = "0")
    private Integer distributorId = 0;

    /** 团长ID */
    @MpField(value = "chief_id", columnType = "bigint", comment = "团长ID")
    private Long chiefId;

    /** 提现账号姓名，可为空 */
    @MpField(value = "account_name", columnType = "string", nullable = true, comment = "提现账号姓名")
    private String accountName;

    /** 提现账号：微信为 openid，支付宝为支付宝账号，银行卡为银行卡号 */
    @MpField(value = "pay_account", columnType = "string", comment = "提现账号 微信为openid 支付宝为支付宝账号 银行卡为银行卡号")
    private String payAccount;

    /** 银行名称，可为空 */
    @MpField(value = "bank_name", columnType = "string", nullable = true, comment = "银行名称")
    private String bankName;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", comment = "手机号")
    private String mobile;

    /** 提现金额，以分为单位，无符号，默认 0 */
    @MpField(value = "money", columnType = "integer", comment = "提现金额，以分为单位", defaultValue = "0")
    private Integer money = 0;

    /**
     * 提现状态：apply 待处理；reject 拒绝；success 提现成功；process 处理中；failed 提现失败。
     */
    @MpField(value = "status", columnType = "string", comment = "提现状态：apply->待处理 reject->拒绝 success->提现成功 process->处理中 failed->提现失败")
    private String status;

    /** 备注，可为空 */
    @MpField(value = "remarks", columnType = "string", nullable = true, comment = "备注")
    private String remarks;

    /** 提现支付类型 */
    @MpField(value = "pay_type", columnType = "string", comment = "提现支付类型")
    private String payType;

    /** 提现的小程序 appid */
    @MpField(value = "wxa_appid", columnType = "string", comment = "提现的小程序appid")
    private String wxaAppid;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Integer created;

    /** 更新时间（整型时间戳） */
    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Integer updated;
}
