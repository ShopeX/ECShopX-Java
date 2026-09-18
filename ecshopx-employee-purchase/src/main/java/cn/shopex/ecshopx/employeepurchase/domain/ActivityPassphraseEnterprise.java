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

package cn.shopex.ecshopx.employeepurchase.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 内购活动口令企业配置 */
@Data
@MpTable(
		value = "employee_purchase_activity_passphrase_enterprises",
		comment = "内购活动口令企业配置",
		indexes = {
			@MpIndex(name = "idx_company_id", columns = {"company_id"}),
			@MpIndex(name = "idx_activity_id", columns = {"activity_id"}),
			@MpIndex(name = "idx_enterprise_id", columns = {"enterprise_id"})
		},
		uniqueIndexes = {
			@MpIndex(name = "uk_activity_enterprise", columns = {"activity_id", "enterprise_id"}),
			@MpIndex(name = "uk_activity_code", columns = {"activity_id", "passphrase_code"})
		})
public class ActivityPassphraseEnterprise {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "activity_id", columnType = "bigint")
	private Long activityId;

	@MpField(value = "enterprise_id", columnType = "bigint")
	private Long enterpriseId;

	@MpField(value = "participate_quota", columnType = "integer", comment = "可参与名额")
	private Integer participateQuota;

	@MpField(value = "passphrase_limitfee", columnType = "integer", comment = "口令通道额度（分）")
	private Integer passphraseLimitfee;

	@MpField(value = "passphrase_code", columnType = "string", length = 64, comment = "口令编码")
	private String passphraseCode;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
