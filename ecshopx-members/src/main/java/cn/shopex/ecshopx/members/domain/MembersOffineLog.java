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

/** 会员线下实体卡日志 */
@Data
@MpTable(value = "members_offine_log", comment = "会员线下实体卡日志", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class MembersOffineLog {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司_ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司_ID")
    private Long companyId;

    /** 实体卡_编号 */
    @MpField(value = "offline_card_code", columnType = "string", comment = "实体卡_编号")
    private String offlineCardCode;

    /** 姓名 */
    @MpField(value = "username", columnType = "string", length = 50, nullable = true, comment = "姓名")
    private String username;

    /** 性别。0 未知 1 男 2 女 */
    @MpField(value = "sex", columnType = "smallint", nullable = true, comment = "性别。0 未知 1 男 2 女")
    private Integer sex;

    /** 会员等级 */
    @MpField(value = "grade_id", columnType = "string", length = 50, nullable = true, comment = "会员等级")
    private String gradeId;

    /** 出生日期 */
    @MpField(value = "birthday", columnType = "string", length = 100, nullable = true, comment = "出生日期")
    private String birthday;

    /** 家庭住址 */
    @MpField(value = "address", columnType = "string", length = 255, nullable = true, comment = "家庭住址")
    private String address;

    /** 常用邮箱 */
    @MpField(value = "email", columnType = "string", length = 100, nullable = true, comment = "常用邮箱")
    private String email;

    /** 入会日期 */
    @MpField(value = "created_time", columnType = "string", length = 100, nullable = true, comment = "入会日期")
    private String createdTime;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
