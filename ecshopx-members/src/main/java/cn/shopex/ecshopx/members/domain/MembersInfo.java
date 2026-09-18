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

/** 会员详情信息表 */
@Data
@MpTable(value = "members_info", comment = "会员详情信息表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_dm_card_no", columns = {"dm_card_no"})})
public class MembersInfo {

    /** 用户id */
    @MpId(value = "user_id", type = IdType.INPUT, columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 姓名 */
    @MpField(value = "username", columnType = "string", length = 500, nullable = true, comment = "姓名")
    private String username;

    /** 名称 */
    @MpField(value = "name", columnType = "string", length = 255, nullable = true, comment = "名称")
    private String name;

    /** 头像 */
    @MpField(value = "avatar", columnType = "string", length = 255, nullable = true, comment = "头像")
    private String avatar;

    /** 性别。0 未知 1 男 2 女 */
    @MpField(value = "sex", columnType = "smallint", nullable = true, comment = "性别。0 未知 1 男 2 女", defaultValue = "0")
    private Integer sex = 0;

    /** 出生日期 */
    @MpField(value = "birthday", columnType = "string", length = 100, nullable = true, comment = "出生日期")
    private String birthday;

    /** 家庭住址 */
    @MpField(value = "address", columnType = "string", length = 255, nullable = true, comment = "家庭住址")
    private String address;

    /** 常用邮箱 */
    @MpField(value = "email", columnType = "string", length = 100, nullable = true, comment = "常用邮箱")
    private String email;

    /** 从事行业 */
    @MpField(value = "industry", columnType = "string", nullable = true, comment = "从事行业")
    private String industry;

    /** 年收入 */
    @MpField(value = "income", columnType = "string", length = 50, nullable = true, comment = "年收入")
    private String income;

    /** 学历 */
    @MpField(value = "edu_background", columnType = "string", length = 50, nullable = true, comment = "学历")
    private String eduBackground;

    /** 爱好 */
    @MpField(value = "habbit", columnType = "json_array", nullable = true, comment = "爱好")
    private String habbit;

    /** 是否有消费 */
    @MpField(value = "have_consume", columnType = "boolean", nullable = true, comment = "是否有消费", defaultValue = "False")
    private Boolean haveConsume = false;

    /** 生日年份，默认 0 */
    @MpField(value = "year", columnType = "integer", nullable = true, comment = "生日年份", defaultValue = "0")
    private Integer year = 0;

    /** 生日月份，默认 0 */
    @MpField(value = "month", columnType = "integer", nullable = true, comment = "生日月份", defaultValue = "0")
    private Integer month = 0;

    /** 生日日期，默认 0 */
    @MpField(value = "day", columnType = "integer", nullable = true, comment = "生日日期", defaultValue = "0")
    private Integer day = 0;

    /** 其他参数，透传前端传递进来的参数 */
    @MpField(value = "other_params", columnType = "text", comment = "其他参数，透传前端传递进来的参数")
    private String otherParams;

    /** 达摩CRM会员id */
    @MpField(value = "dm_member_id", columnType = "string", length = 255, nullable = true, comment = "达摩CRM会员id")
    private String dmMemberId;

    /** 达摩CRM会员卡号 */
    @MpField(value = "dm_card_no", columnType = "string", length = 255, nullable = true, comment = "达摩CRM会员卡号")
    private String dmCardNo;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
