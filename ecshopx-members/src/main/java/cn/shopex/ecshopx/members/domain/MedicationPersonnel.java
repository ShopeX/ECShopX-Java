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

package cn.shopex.ecshopx.members.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 用药人信息 */
@Data
@MpTable(value = "medication_personnel", comment = "用药人信息", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_user_family_id_card", columns = {"user_family_id_card"}), @MpIndex(name = "idx_is_default", columns = {"is_default"})})
public class MedicationPersonnel {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 用药人姓名 */
    @MpField(value = "user_family_name", columnType = "string", comment = "用药人姓名")
    private String userFamilyName;

    /** 用药人身份证号 */
    @MpField(value = "user_family_id_card", columnType = "string", comment = "用药人身份证号")
    private String userFamilyIdCard = "";

    /** 用药人年龄 */
    @MpField(value = "user_family_age", columnType = "integer", comment = "用药人年龄")
    private Integer userFamilyAge;

    /** 用药人性别1-男，2-女 */
    @MpField(value = "user_family_gender", columnType = "smallint", comment = "用药人性别1-男，2-女")
    private Integer userFamilyGender;

    /** 用药人手机号码 */
    @MpField(value = "user_family_phone", columnType = "string", comment = "用药人手机号码")
    private String userFamilyPhone;

    /** 用药人与问诊人关系(1本人 2父母 3配偶 4子女 5其他) */
    @MpField(value = "relationship", columnType = "smallint", comment = "用药人与问诊人关系(1本人 2父母 3配偶 4子女 5其他)")
    private Integer relationship;

    /** 是否默认 */
    @MpField(value = "is_default", columnType = "smallint", comment = "是否默认", defaultValue = "0")
    private Integer isDefault = 0;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
