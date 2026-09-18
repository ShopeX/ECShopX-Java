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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 管理员修改员工操作日志表 */
@Data
@MpTable(value = "members_operate_log", comment = "管理员修改员工操作日志表")
public class MemberOperateLog {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** log类型，mobile：修改手机号,grade_id:修改会员等级 */
    @MpField(value = "operate_type", columnType = "string", comment = "log类型，mobile：修改手机号,grade_id:修改会员等级")
    private String operateType = "mobile";

    /** 操作备注 */
    @MpField(value = "remarks", columnType = "string", nullable = true, comment = "操作备注")
    private String remarks;

    /** 修改前历史数据 */
    @MpField(value = "old_data", columnType = "text", nullable = true, comment = "修改前历史数据")
    private String oldData;

    /** 新修改的数据 */
    @MpField(value = "new_data", columnType = "text", comment = "新修改的数据")
    private String newData;

    /** 管理员描述 */
    @MpField(value = "operater", columnType = "text", comment = "管理员描述")
    private String operater;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Long updated;
}
