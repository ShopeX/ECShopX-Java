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

package cn.shopex.ecshopx.bspay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 开户申请表
 */
@Data
@MpTable(value = "bspay_entry_apply", comment = "开户申请表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_user_id", columns = {"user_id"}), @MpIndex(name = "ix_operator_type", columns = {"operator_type"}), @MpIndex(name = "ix_status", columns = {"status"})})
public class EntryApply {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 用户名 */
    @MpField(value = "user_name", columnType = "string", length = 64, comment = "用户名")
    private String userName;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;

    /** 开户进件ID */
    @MpField(value = "user_id", columnType = "string", comment = "开户进件ID")
    private String userId;

    /** 操作者id */
    @MpField(value = "operator_id", columnType = "integer", nullable = true, comment = "操作者id", defaultValue = "0")
    private Integer operatorId = 0;

    /** 操作者类型:distributor-店铺;dealer-经销;promoter-推广员 */
    @MpField(value = "operator_type", columnType = "string", comment = "操作者类型:distributor-店铺;dealer-经销;promoter-推广员")
    private String operatorType;

    /**
     * 进件类型，默认 indv；indv=个人，ent=企业
     */
    @MpField(value = "user_type", columnType = "string", length = 20, comment = "进件类型", defaultValue = "indv")
    private String userType = "indv";

    /** 所属地区 */
    @MpField(value = "address", columnType = "string", nullable = true, comment = "所属地区")
    private String address;

    /** 审批意见 */
    @MpField(value = "comments", columnType = "text", nullable = true, comment = "审批意见")
    private String comments;

    /** WAIT_APPROVE;APPROVED;REJECT */
    @MpField(value = "status", columnType = "string", length = 20, nullable = true, comment = "WAIT_APPROVE;APPROVED;REJECT")
    private String status;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
