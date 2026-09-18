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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 激活表
 */
@Data
@MpTable(value = "activate_log", comment = "激活表")
public class ActivateLog {

    /** 激活id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "激活id")
    private Long id;

    /** 资源包id */
    @MpField(value = "resource_id", columnType = "bigint", comment = "资源包id")
    private Long resourceId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 企业id */
    @MpField(value = "eid", columnType = "string", comment = "企业id")
    private String eid;

    @MpField(value = "passport_uid", columnType = "string")
    private String passportUid;

    /** 激活码 */
    @MpField(value = "active_code", columnType = "string", nullable = true, comment = "激活码")
    private String activeCode;

    /** 激活类型 */
    @MpField(value = "active_type", columnType = "string", nullable = true, comment = "激活类型")
    private String activeType;

    /** 激活状态 */
    @MpField(value = "active_status", columnType = "string", nullable = true, comment = "激活状态")
    private String activeStatus;

    /** 激活时间 */
    @MpField(value = "activeAt", columnType = "bigint", comment = "激活时间")
    private Long activeAt;

    /** 过期时间 */
    @MpField(value = "expiredAt", columnType = "bigint", comment = "过期时间")
    private Long expiredAt;
}
