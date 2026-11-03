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

package cn.shopex.ecshopx.digios.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@MpTable(
		value = "digios_api_log",
		comment = "DigiOS直连API日志",
		indexes = {
			@MpIndex(name = "idx_digios_log_company_created", columns = {"company_id", "created"}),
			@MpIndex(name = "idx_digios_log_method", columns = {"method"})
		})
public class DigiOsApiLog {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "direction", columnType = "string", length = 16)
	private String direction;

	@MpField(value = "method", columnType = "string", length = 128, nullable = true)
	private String method;

	@MpField(value = "http_status", columnType = "smallint", nullable = true)
	private Integer httpStatus;

	@MpField(value = "outcome", columnType = "string", length = 32, nullable = true)
	private String outcome;

	@MpField(value = "runtime_ms", columnType = "integer", nullable = true)
	private Integer runtimeMs;

	@MpField(value = "request_body", columnType = "text", nullable = true)
	private String requestBody;

	@MpField(value = "response_body", columnType = "text", nullable = true)
	private String responseBody;

	@MpField(value = "error_message", columnType = "string", length = 1024, nullable = true)
	private String errorMessage;

	@MpField(value = "created", columnType = "integer")
	private Integer created;
}
