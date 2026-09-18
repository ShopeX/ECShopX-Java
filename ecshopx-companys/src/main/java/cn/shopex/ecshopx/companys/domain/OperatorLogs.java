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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 商家操作日志表
 */
@Data
@MpTable(value = "companys_operator_logs", comment = "商家操作日志表")
public class OperatorLogs {

    @MpId(value = "log_id", type = IdType.AUTO, columnType = "bigint")
    private Long logId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id", defaultValue = "0")
    private Long companyId = 0L;

    /** 操作者id */
    @MpField(value = "operator_id", columnType = "integer", comment = "操作者id", defaultValue = "0")
    private Integer operatorId = 0;

    /** 请求资源信息 */
    @MpField(value = "request_uri", columnType = "string", nullable = true, comment = "请求资源信息")
    private String requestUri;

    /** ip地址 */
    @MpField(value = "ip", columnType = "string", nullable = true, comment = "ip地址")
    private String ip;

    /** 请求参数 */
    @MpField(value = "params", columnType = "json_array", nullable = true, comment = "请求参数")
    private String params;

    /** 操作内容 */
    @MpField(value = "operator_name", columnType = "string", nullable = true, comment = "操作内容")
    private String operatorName;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 操作日志类型 */
    @MpField(value = "log_type", columnType = "string", nullable = true, comment = "操作日志类型", defaultValue = "operator")
    private String logType = "operator";

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;
}
