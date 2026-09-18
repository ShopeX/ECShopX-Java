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

package cn.shopex.ecshopx.supplier.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 供应商
 *
 * <p>索引：idx_supplier_name（supplier_name）、idx_mobile（mobile）、idx_is_check（is_check）
 */
@Data
@MpTable(value = "supplier", comment = "供应商", indexes = {@MpIndex(name = "idx_supplier_name", columns = {"supplier_name"}), @MpIndex(name = "idx_mobile", columns = {"mobile"}), @MpIndex(name = "idx_is_check", columns = {"is_check"})})
public class Supplier {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 商户id */
    @MpField(value = "company_id", columnType = "bigint", comment = "商户id")
    private Long companyId;

    /** 供应商名称 */
    @MpField(value = "supplier_name", columnType = "string", length = 100, nullable = true, comment = "供应商名称")
    private String supplierName;

    /** 联系人 */
    @MpField(value = "contact", columnType = "string", length = 30, nullable = true, comment = "联系人")
    private String contact = "";

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", length = 30, nullable = true, comment = "手机号")
    private String mobile = "";

    /** 营业执照 */
    @MpField(value = "business_license", columnType = "string", length = 500, nullable = true, comment = "营业执照")
    private String businessLicense;

    /** 企业微信二维码 */
    @MpField(value = "wechat_qrcode", columnType = "string", length = 500, nullable = true, comment = "企业微信二维码")
    private String wechatQrcode;

    /** 客服电话 */
    @MpField(value = "service_tel", columnType = "string", length = 30, nullable = true, comment = "客服电话")
    private String serviceTel;

    /** 收款银行 */
    @MpField(value = "bank_name", columnType = "string", length = 50, nullable = true, comment = "收款银行")
    private String bankName;

    /** 收款账号 */
    @MpField(value = "bank_account", columnType = "string", length = 50, nullable = true, comment = "收款账号")
    private String bankAccount;

    /** 是否审核 */
    @MpField(value = "is_check", columnType = "bigint", comment = "是否审核")
    private Long isCheck;

    /** 审核备注 */
    @MpField(value = "audit_remark", columnType = "string", length = 500, nullable = true, comment = "审核备注")
    private String auditRemark;

    /** 汇付商户ID */
    @MpField(value = "adapay_mch_id", columnType = "string", length = 50, nullable = true, comment = "汇付商户ID")
    private String adapayMchId;

    /** 微信公众号openid */
    @MpField(value = "wx_openid", columnType = "string", length = 100, nullable = true, comment = "微信公众号openid")
    private String wxOpenid;

    /** 管理员id */
    @MpField(value = "operator_id", columnType = "bigint", comment = "管理员id")
    private Long operatorId;

    /** 创建时间 */
    @MpField(value = "add_time", columnType = "datetime", nullable = true, comment = "创建时间")
    private LocalDateTime addTime;

    /** 更新时间 */
    @MpField(value = "modify_time", columnType = "datetime", nullable = true, comment = "更新时间")
    private LocalDateTime modifyTime;
}
