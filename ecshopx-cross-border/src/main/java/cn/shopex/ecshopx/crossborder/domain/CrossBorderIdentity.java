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

package cn.shopex.ecshopx.crossborder.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 跨境-身份证信息
 */
@Data
@MpTable(value = "crossborder_identity", comment = "跨境-身份证信息", indexes = {@MpIndex(name = "ix_id", columns = {"id"}), @MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_user_id", columns = {"user_id"})})
public class CrossBorderIdentity {

    /** 设置id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "设置id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 身份证 */
    @MpField(value = "identity_id", columnType = "string", length = 18, comment = "身份证")
    private String identityId;

    /** 身份证姓名 */
    @MpField(value = "identity_name", columnType = "string", length = 20, comment = "身份证姓名")
    private String identityName;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
