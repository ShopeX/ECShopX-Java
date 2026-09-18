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
 * adapay短信提醒日志表
 */
@Data
@MpTable(value = "adapay_sms_log", comment = "adapay短信提醒日志表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_rel_id", columns = {"rel_id"}), @MpIndex(name = "ix_usr_phone", columns = {"usr_phone"})})
public class AdapaySmsLog {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "string", comment = "公司id")
    private String companyId;

    /** 发送手机号 */
    @MpField(value = "usr_phone", columnType = "string", comment = "发送手机号")
    private String usrPhone;

    /** 如果type(提醒类型)为1或2 此字段为开户的member_id,如果type(提醒类型)为3 此字段为dealer_id */
    @MpField(value = "rel_id", columnType = "string", comment = "如果type(提醒类型)为1或2 此字段为开户的member_id,如果type(提醒类型)为3 此字段为dealer_id")
    private String relId;

    /** 提醒类型 总商户开户提醒:1  子商户开户提醒:2  重置密码提醒:3 */
    @MpField(value = "type", columnType = "string", comment = "提醒类型 总商户开户提醒:1  子商户开户提醒:2  重置密码提醒:3")
    private String type;

    /** 发送内容 */
    @MpField(value = "content", columnType = "text", comment = "发送内容")
    private String content;

    /** 发送状态 1:成功  0:失败 */
    @MpField(value = "status", columnType = "string", nullable = true, comment = "发送状态 1:成功  0:失败")
    private String status;

    /** 错误信息 */
    @MpField(value = "error_info", columnType = "text", nullable = true, comment = "错误信息")
    private String errorInfo;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
