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

/** 内购活动企业行为流水 */
@Data
@MpTable(
		value = "employee_purchase_activity_enterprise_behavior_log",
		comment = "内购活动企业行为流水",
		indexes = {
			@MpIndex(name = "idx_company_id", columns = {"company_id"}),
			@MpIndex(name = "idx_activity_id", columns = {"activity_id"}),
			@MpIndex(name = "idx_enterprise_id", columns = {"enterprise_id"}),
			@MpIndex(name = "idx_behavior_type", columns = {"behavior_type"})
		})
public class ActivityEnterpriseBehaviorLog {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "activity_id", columnType = "bigint")
	private Long activityId;

	@MpField(value = "enterprise_id", columnType = "bigint")
	private Long enterpriseId;

	@MpField(value = "user_id", columnType = "bigint", nullable = true)
	private Long userId;

	@MpField(value = "behavior_type", columnType = "string", length = 32)
	private String behaviorType;

	@MpField(value = "result_status", columnType = "string", length = 16, nullable = true)
	private String resultStatus;

	@MpField(value = "visitor_key", columnType = "string", length = 64, nullable = true)
	private String visitorKey;

	@MpField(value = "ref_id", columnType = "bigint", nullable = true)
	private Long refId;

	@MpField(value = "extra", columnType = "json", nullable = true)
	private String extra;

	@MpField(value = "created", columnType = "integer")
	private Integer created;
}
