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

/** 数云开放网关租户配置 */
@Data
@MpTable(
		value = "company_shuyun_open_platform_config",
		comment = "数云开放网关租户配置",
		uniqueIndexes = {
			@MpIndex(name = "uk_shuyun_op_auth_value", columns = {"auth_value"}),
			@MpIndex(name = "uk_shuyun_op_company_id", columns = {"company_id"}),
			@MpIndex(name = "uk_shuyun_op_app_id", columns = {"app_id"})
		})
public class CompanyShuyunOpenPlatformConfig {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "auth_value", columnType = "string", length = 128, nullable = true)
	private String authValue;

	@MpField(value = "plat_code", columnType = "string", length = 64, nullable = true)
	private String platCode;

	@MpField(value = "app_id", columnType = "string", length = 64, nullable = true)
	private String appId;

	@MpField(value = "app_secret", columnType = "string", length = 512, nullable = true)
	private String appSecret;

	@MpField(value = "access_token", columnType = "string", nullable = true)
	private String accessToken;

	@MpField(value = "is_over_due", columnType = "string", length = 8, nullable = true)
	private String isOverDue;

	@MpField(value = "is_enabled", columnType = "integer")
	private Integer isEnabled;

	@MpField(value = "created", columnType = "integer", comment = "添加时间")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
	private Integer updated;
}
