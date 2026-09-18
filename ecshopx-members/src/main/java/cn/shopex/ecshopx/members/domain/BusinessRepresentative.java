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

import java.math.BigDecimal;
import java.time.LocalDate;

/** 业务员 */
@Data
@MpTable(value = "business_representative", comment = "业务员", indexes = {@MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_user_bussiness_id", columns = {"user_id", "business_rep_id"})}, uniqueIndexes = {@MpIndex(name = "user_bussiness_id", columns = {"user_id", "business_rep_id"})})
public class BusinessRepresentative {

    /** 业务员ID（自增主键） */
    @MpId(value = "business_rep_id", type = IdType.AUTO, columnType = "bigint")
    private Long businessRepId;

    /** 用户ID（外键，关联到用户表） */
    @MpField(value = "user_id", columnType = "bigint")
    private Long userId;

    /** 姓名 */
    @MpField(value = "name", columnType = "string", length = 100)
    private String name;

    /** 性别 */
    @MpField(value = "gender", columnType = "string", length = 6, nullable = true)
    private String gender;

    /** 年龄 */
    @MpField(value = "age", columnType = "integer", nullable = true)
    private Integer age;

    /** 入职日期 */
    @MpField(value = "hire_date", columnType = "date")
    private LocalDate hireDate;

    /** 所属部门ID */
    @MpField(value = "department_id", columnType = "integer", nullable = true)
    private Integer departmentId;

    /** 职位 */
    @MpField(value = "job_title", columnType = "string", length = 50)
    private String jobTitle;

    /** 销售业绩 */
    @MpField(value = "sales_performance", columnType = "decimal", precision = 10, scale = 2, defaultValue = "0.00")
    private BigDecimal salesPerformance = new BigDecimal("0.00");

    /** 客户关系维护得分 */
    @MpField(value = "customer_relations_score", columnType = "decimal", precision = 5, scale = 2, defaultValue = "0.00")
    private BigDecimal customerRelationsScore = new BigDecimal("0.00");

    /** 是否在职 */
    @MpField(value = "is_active", columnType = "boolean", defaultValue = "True")
    private Boolean isActive = true;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer")
    private Integer createTime;
}
