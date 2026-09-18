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

package cn.shopex.ecshopx.adapay.domain;

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
@MpTable(value = "adapay_entry_apply", comment = "开户申请表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class AdapayEntryApply {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 用户名 */
    @MpField(value = "user_name", columnType = "string", length = 64, comment = "用户名")
    private String userName;

    /** 企业ID */
    @MpField(value = "company_id", columnType = "string", comment = "企业ID")
    private String companyId;

    /** 开户进件ID */
    @MpField(value = "entry_id", columnType = "string", comment = "开户进件ID")
    private String entryId;

    /** 申请类型:dealer;distributor */
    @MpField(value = "apply_type", columnType = "string", comment = "申请类型:dealer;distributor")
    private String applyType;

    /** 所属地区 */
    @MpField(value = "address", columnType = "string", nullable = true, comment = "所属地区")
    private String address;

    /** 审批意见 */
    @MpField(value = "comments", columnType = "text", nullable = true, comment = "审批意见")
    private String comments;

    /** 是否短信提醒: 1:是  0:否 */
    @MpField(value = "is_sms", columnType = "string", length = 20, nullable = true, comment = "是否短信提醒: 1:是  0:否")
    private String isSms;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 状态：WAIT_APPROVE-待审批；APPROVED-已通过；REJECT-已拒绝 */
    @MpField(value = "status", columnType = "string", length = 20, nullable = true, comment = "WAIT_APPROVE;APPROVED;REJECT")
    private String status;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
