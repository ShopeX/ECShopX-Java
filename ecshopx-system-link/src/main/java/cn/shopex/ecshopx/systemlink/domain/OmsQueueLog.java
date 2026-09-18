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

package cn.shopex.ecshopx.systemlink.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 联通oms通信日志 */
@Data
@MpTable(value = "systemlink_oms_queuelog", comment = "联通oms通信日志")
public class OmsQueueLog {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 日志同步类型, response:响应，request:请求 */
    @MpField(value = "api_type", columnType = "string", comment = "日志同步类型, response:响应，request:请求")
    private String apiType;

    /** api */
    @MpField(value = "worker", columnType = "string", comment = "api")
    private String worker;

    /** 任务参数 */
    @MpField(value = "params", columnType = "json_array", nullable = true, comment = "任务参数")
    private String params;

    /** 返回数据 */
    @MpField(value = "result", columnType = "json_array", nullable = true, comment = "返回数据")
    private String result;

    /** 运行状态：running,success,fail */
    @MpField(value = "status", columnType = "string", comment = "运行状态：running,success,fail", defaultValue = "running")
    private String status = "running";

    /** 运行时间(秒) */
    @MpField(value = "runtime", columnType = "string", nullable = true, comment = "运行时间(秒)")
    private String runtime;

    /** msg_id */
    @MpField(value = "msg_id", columnType = "string", nullable = true, comment = "msg_id")
    private String msgId;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
