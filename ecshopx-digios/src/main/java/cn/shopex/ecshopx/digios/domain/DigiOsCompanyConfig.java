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
		value = "digios_company_config",
		comment = "DigiOS直连公司配置",
		uniqueIndexes = {
			@MpIndex(name = "uk_digios_company_id", columns = {"company_id"}),
			@MpIndex(name = "uk_digios_app_key", columns = {"app_key"})
		})
public class DigiOsCompanyConfig {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "is_enabled", columnType = "smallint")
	private Integer isEnabled;

	@MpField(value = "base_url", columnType = "string", length = 512, nullable = true)
	private String baseUrl;

	@MpField(value = "app_key", columnType = "string", length = 128, nullable = true)
	private String appKey;

	@MpField(value = "app_secret", columnType = "string", length = 512, nullable = true)
	private String appSecret;

	@MpField(value = "extra_json", columnType = "text", nullable = true)
	private String extraJson;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
