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

import java.time.LocalDate;

/** 业务员关联会员 */
@Data
@MpTable(value = "business_rep_user", comment = "业务员关联会员", indexes = {@MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_user_bussiness_id", columns = {"user_id", "business_rep_id"})}, uniqueIndexes = {@MpIndex(name = "user_bussiness_id", columns = {"user_id", "business_rep_id"})})
public class BusinessRepUser {

    /** 关系ID（自增主键） */
    @MpId(value = "relation_id", type = IdType.AUTO, columnType = "bigint", comment = "关系ID（自增主键）")
    private Long relationId;

    /** 业务员ID（外键，关联到业务员表） */
    @MpField(value = "business_rep_id", comment = "业务员ID（外键，关联到业务员表）")
    private Long businessRepId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 指定日期（即业务员开始服务该用户的日期） */
    @MpField(value = "assigned_date", columnType = "date", comment = "指定日期（即业务员开始服务该用户的日期）")
    private LocalDate assignedDate;

    /** 关系描述（可选，用于记录特殊说明） */
    @MpField(value = "description", columnType = "text", nullable = true, comment = "关系描述（可选，用于记录特殊说明）")
    private String description;

    @MpField(value = "create_time", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long createTime;
}
