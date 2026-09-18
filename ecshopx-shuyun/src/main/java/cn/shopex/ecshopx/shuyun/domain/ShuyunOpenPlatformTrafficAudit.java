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

package cn.shopex.ecshopx.shuyun.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 数云开放网关/回调排障审计 */
@Data
@MpTable(
		value = "shuyun_open_platform_traffic_audit",
		comment = "数云开放网关/回调排障审计（轻量）",
		indexes = {
			@MpIndex(name = "idx_shuyun_op_traffic_company_created", columns = {"company_id", "created"}),
			@MpIndex(name = "idx_shuyun_op_traffic_correlation", columns = {"correlation_id"})
		})
public class ShuyunOpenPlatformTrafficAudit {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "direction", columnType = "string", length = 16)
	private String direction;

	@MpField(value = "correlation_id", columnType = "string", length = 128)
	private String correlationId;

	@MpField(value = "http_verb", columnType = "string", length = 16)
	private String httpVerb;

	@MpField(value = "action_method", columnType = "string", length = 255, nullable = true)
	private String actionMethod;

	@MpField(value = "http_status", columnType = "integer", nullable = true)
	private Integer httpStatus;

	@MpField(value = "outcome", columnType = "string", length = 32)
	private String outcome;

	@MpField(value = "request_headers_json", columnType = "string")
	private String requestHeadersJson;

	@MpField(value = "request_body", columnType = "string", nullable = true)
	private String requestBody;

	@MpField(value = "response_body", columnType = "string", nullable = true)
	private String responseBody;

	@MpField(value = "error_message", columnType = "string", length = 1024, nullable = true)
	private String errorMessage;

	@MpField(value = "created", columnType = "integer", comment = "添加时间")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
	private Integer updated;
}
