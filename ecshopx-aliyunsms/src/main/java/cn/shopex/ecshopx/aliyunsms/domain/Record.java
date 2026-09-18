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
 * 短信发送记录
 */
@Data
@MpTable(value = "aliyunsms_record", comment = "短信发送记录", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class Record {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", comment = "手机号")
    private String mobile;

    /** 场景ID */
    @MpField(value = "scene_id", columnType = "integer", comment = "场景ID")
    private Integer sceneId;

    /** 任务ID */
    @MpField(value = "task_id", columnType = "integer", comment = "任务ID")
    private Integer taskId = 0;

    /** 模板code */
    @MpField(value = "template_code", columnType = "string", comment = "模板code")
    private String templateCode;

    /** 短信内容，可空 */
    @MpField(value = "sms_content", columnType = "string", nullable = true, comment = "短信内容")
    private String smsContent;

    /** 短信类型:0：验证码;1：短信通知;2：推广短信; */
    @MpField(value = "template_type", columnType = "string", comment = "短信类型:0：验证码;1：短信通知;2：推广短信;")
    private String templateType;

    /** 发送状态:1-发送中;2-发送失败;3-发送成功 */
    @MpField(value = "status", columnType = "string", comment = "发送状态:1-发送中;2-发送失败;3-发送成功")
    private String status;

    /** 发送回执ID */
    @MpField(value = "biz_id", columnType = "string", comment = "发送回执ID")
    private String bizId;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间，可空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
