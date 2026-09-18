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
 * 实名用户对象
 */
@Data
@MpTable(value = "adapay_member", comment = "实名用户对象", indexes = {@MpIndex(name = "idx_tel_no", columns = {"tel_no"}, lengths = {64}), @MpIndex(name = "idx_user_name", columns = {"user_name"}, lengths = {64}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_cert_id", columns = {"cert_id"}, lengths = {64}), @MpIndex(name = "idx_pid", columns = {"pid"}), @MpIndex(name = "idx_operator_id", columns = {"operator_id"}), @MpIndex(name = "idx_audit_state", columns = {"audit_state"})})
public class AdapayMember {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 应用app_id */
    @MpField(value = "app_id", columnType = "string", length = 100, comment = "应用app_id")
    private String appId = "";

    /** 用户地址 */
    @MpField(value = "location", columnType = "string", length = 200, nullable = true, comment = "用户地址")
    private String location = "";

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 父ID */
    @MpField(value = "pid", columnType = "bigint", comment = "父ID", defaultValue = "0")
    private Long pid = 0L;

    /** 是否审核成功后修改 1:是  0:否 */
    @MpField(value = "is_update", columnType = "integer", length = 10, comment = "是否审核成功后修改 1:是  0:否", defaultValue = "0")
    private Integer isUpdate = 0;

    /** 操作者id */
    @MpField(value = "operator_id", columnType = "integer", nullable = true, comment = "操作者id", defaultValue = "0")
    private Integer operatorId = 0;

    /** 操作者类型:distributor-店铺;dealer-经销;promoter-推广员 */
    @MpField(value = "operator_type", columnType = "string", comment = "操作者类型:distributor-店铺;dealer-经销;promoter-推广员")
    private String operatorType;

    /** 用户邮箱 */
    @MpField(value = "email", columnType = "string", length = 100, nullable = true, comment = "用户邮箱")
    private String email = "";

    /** 账户类型：person-个人；corp-企业。默认 person */
    @MpField(value = "member_type", columnType = "string", length = 20, nullable = true, comment = "账户类型", defaultValue = "person")
    private String memberType = "person";

    /** 性别：MALE-男；FEMALE-女。为空表示未填写 */
    @MpField(value = "gender", columnType = "string", length = 50, nullable = true, comment = "性别，为空时表示未填写")
    private String gender = "";

    /** 用户昵称 */
    @MpField(value = "nickname", columnType = "string", length = 50, nullable = true, comment = "用户昵称")
    private String nickname = "";

    /** 用户手机号 */
    @MpField(value = "tel_no", columnType = "string", length = 255, nullable = true, comment = "用户手机号")
    private String telNo = "";

    /** 用户姓名 */
    @MpField(value = "user_name", columnType = "string", length = 500, nullable = true, comment = "用户姓名")
    private String userName = "";

    /** 证件类型：00-身份证（仅支持此值） */
    @MpField(value = "cert_type", columnType = "string", length = 10, nullable = true, comment = "证件类型，仅支持：00-身份证", defaultValue = "00")
    private String certType = "00";

    /** 证件号 */
    @MpField(value = "cert_id", columnType = "string", length = 255, nullable = true, comment = "证件号")
    private String certId = "";

    /** 是否短信提醒: 1:是  0:否 */
    @MpField(value = "is_sms", columnType = "string", length = 20, nullable = true, comment = "是否短信提醒: 1:是  0:否")
    private String isSms;

    /** 审核状态，状态包括：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功 */
    @MpField(value = "audit_state", columnType = "string", length = 50, nullable = true, comment = "审核状态，状态包括：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功")
    private String auditState = "";

    /** 审核结果描述 */
    @MpField(value = "audit_desc", columnType = "string", length = 500, nullable = true, comment = "审核结果描述")
    private String auditDesc = "";

    /** 当前交易状态：pending-处理中；succeeded-成功；failed-失败 */
    @MpField(value = "status", columnType = "string", length = 50, nullable = true, comment = "当前交易状态")
    private String status = "";

    /** 错误描述 */
    @MpField(value = "error_info", columnType = "string", length = 500, nullable = true, comment = "错误描述")
    private String errorInfo = "";

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;

    /** 是否点过结算中心 */
    @MpField(value = "valid", columnType = "boolean", nullable = true, comment = "是否点过结算中心", defaultValue = "0")
    private Boolean valid = false;

    /** 会员是否创建成功 */
    @MpField(value = "is_created", columnType = "boolean", comment = "会员是否创建成功", defaultValue = "0")
    private Boolean isCreated = false;
}
