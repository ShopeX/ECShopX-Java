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

package cn.shopex.ecshopx.selfservice.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 用户日常数据记录表 */
@Data
@MpTable(value = "selfservice_user_daily_record", comment = "用户日常数据记录表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class UserDailyRecord {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 操作员id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "操作员id")
    private Long operatorId;

    /** 操作员名称或手机 */
    @MpField(value = "operator", columnType = "string", nullable = true, comment = "操作员名称或手机")
    private String operator;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 记录提交日期 */
    @MpField(value = "record_date", columnType = "integer", comment = "记录提交日期")
    private Integer recordDate;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id")
    private Long shopId;

    /** 模板id */
    @MpField(value = "temp_id", columnType = "bigint", nullable = true, comment = "模板id")
    private Long tempId;

    /** 记录表单内容 */
    @MpField(value = "form_data", columnType = "text", comment = "记录表单内容")
    private String formData;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
