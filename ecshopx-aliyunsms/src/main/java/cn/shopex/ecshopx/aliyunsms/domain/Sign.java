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

package cn.shopex.ecshopx.aliyunsms.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 短信签名表
 */
@Data
@MpTable(value = "aliyunsms_sign", comment = "短信签名表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class Sign {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 签名名称，varchar(20) */
    @MpField(value = "sign_name", columnType = "string", length = 20, comment = "签名名称")
    private String signName;

    /**
     * 签名来源，varchar(2)。0：企事业单位的全称或简称；1：工信部备案网站的全称或简称；2：App 应用的全称或简称；3：公众号或小程序的全称或简称；4：电商平台店铺名的全称或简称；5：商标名的全称或简称
     */
    @MpField(value = "sign_source", columnType = "string", length = 2, comment = "签名来源")
    private String signSource;

    /** 签名申请说明 */
    @MpField(value = "remark", columnType = "string", comment = "签名申请说明")
    private String remark;

    /** 资质证明 */
    @MpField(value = "sign_file", columnType = "text", nullable = true, comment = "资质证明")
    private String signFile;

    /** 委托授权书 */
    @MpField(value = "delegate_file", columnType = "text", nullable = true, comment = "委托授权书")
    private String delegateFile;

    /** 审核状态:0-审核中;1-审核通过;2-审核失败 */
    @MpField(value = "status", columnType = "string", length = 2, comment = "审核状态:0-审核中;1-审核通过;2-审核失败")
    private String status = "0";

    /** 审核备注 */
    @MpField(value = "reason", columnType = "string", nullable = true, comment = "审核备注")
    private String reason = "";

    /** 签名用途 1:他用;0:自用 */
    @MpField(value = "third_party", columnType = "integer", comment = "签名用途 1:他用;0:自用", defaultValue = "0")
    private Integer thirdParty = 0;

    /** 资质ID */
    @MpField(value = "qualification_id", columnType = "string", nullable = true, comment = "资质ID")
    private String qualificationId = "";

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间，可空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
