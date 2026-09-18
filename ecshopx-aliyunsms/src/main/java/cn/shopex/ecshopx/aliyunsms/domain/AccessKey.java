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
 * AccessKey表
 */
@Data
@MpTable(value = "aliyunsms_accesskey", comment = "AccessKey表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class AccessKey {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** accesskey_id */
    @MpField(value = "accesskey_id", columnType = "string", comment = "accesskey_id")
    private String accesskeyId;

    /** accesskey_secret */
    @MpField(value = "accesskey_secret", columnType = "string", comment = "accesskey_secret")
    private String accesskeySecret;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间，可空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
