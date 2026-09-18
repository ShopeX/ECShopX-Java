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

/** 数云线下权益档案 */
@Data
@MpTable(
		value = "shuyun_offline_benefit",
		comment = "数云线下权益档案",
		uniqueIndexes = {
			@MpIndex(name = "uk_shuyun_offline_benefit_company_benefit", columns = {"company_id", "benefit_id"})
		})
public class ShuyunOfflineBenefit {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "client_id", columnType = "string", length = 128, nullable = true)
	private String clientId;

	@MpField(value = "benefit_id", columnType = "string", length = 128)
	private String benefitId;

	@MpField(value = "benefit_name", columnType = "string", length = 512, nullable = true)
	private String benefitName;

	@MpField(value = "effective_start", columnType = "integer", nullable = true, comment = "权益生效起(秒)")
	private Integer effectiveStart;

	@MpField(value = "effective_end", columnType = "integer", nullable = true, comment = "权益生效止(秒)")
	private Integer effectiveEnd;

	@MpField(value = "claim_start", columnType = "integer", nullable = true, comment = "领取起(秒)")
	private Integer claimStart;

	@MpField(value = "claim_end", columnType = "integer", nullable = true, comment = "领取止(秒)")
	private Integer claimEnd;

	@MpField(value = "condition_limits_json", columnType = "string", nullable = true)
	private String conditionLimitsJson;

	@MpField(value = "local_card_id", columnType = "bigint", nullable = true, comment = "本地券模板/活动键")
	private Long localCardId;

	@MpField(value = "created", columnType = "integer", comment = "添加时间")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
	private Integer updated;
}
