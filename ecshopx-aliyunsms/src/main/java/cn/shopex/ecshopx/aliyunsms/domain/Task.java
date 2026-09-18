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

package cn.shopex.ecshopx.aliyunsms.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 短信群发任务
 */
@Data
@MpTable(value = "aliyunsms_task", comment = "短信群发任务", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class Task {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 任务名称 */
    @MpField(value = "task_name", columnType = "string", comment = "任务名称")
    private String taskName;

    /** 签名ID */
    @MpField(value = "sign_id", columnType = "integer", comment = "签名ID")
    private Integer signId;

    /** 模板id */
    @MpField(value = "template_id", columnType = "integer", comment = "模板id")
    private Integer templateId;

    /** 模板名称 */
    @MpField(value = "template_name", columnType = "string", comment = "模板名称")
    private String templateName;

    /** 会员id */
    @MpField(value = "user_id", columnType = "text", comment = "会员id")
    private String userId;

    /** 发送状态:1-等待中;2-发送成功;3-发送失败;4-已撤销 */
    @MpField(value = "`status`", columnType = "string", comment = "发送状态:1-等待中;2-发送成功;3-发送失败;4-已撤销")
    private String status = "1";

    /** 发送时间 */
    @MpField(value = "send_at", columnType = "integer", comment = "发送时间")
    private Integer sendAt;

    /** 号码数量 */
    @MpField(value = "total_num", columnType = "integer", comment = "号码数量")
    private Integer totalNum;

    /** 失败号码数量 */
    @MpField(value = "failed_num", columnType = "integer", comment = "失败号码数量")
    private Integer failedNum = 0;

    /** 是否已发送 */
    @MpField(value = "is_send", columnType = "integer", comment = "是否已发送")
    private Integer isSend = 0;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间，可空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
