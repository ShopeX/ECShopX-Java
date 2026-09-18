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

/** 内购活动参与名额已占用用户 */
@Data
@MpTable(
		value = "employee_purchase_activity_enterprise_participate_user",
		comment = "内购活动参与名额已占用用户",
		indexes = {
			@MpIndex(name = "idx_activity_id", columns = {"activity_id"}),
			@MpIndex(name = "idx_enterprise_id", columns = {"enterprise_id"})
		},
		uniqueIndexes = {
			@MpIndex(
					name = "uk_company_activity_enterprise_user",
					columns = {"company_id", "activity_id", "enterprise_id", "user_id"})
		})
public class ActivityEnterpriseParticipateUser {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "activity_id", columnType = "bigint")
	private Long activityId;

	@MpField(value = "enterprise_id", columnType = "bigint")
	private Long enterpriseId;

	@MpField(value = "user_id", columnType = "bigint")
	private Long userId;

	@MpField(value = "created", columnType = "integer")
	private Integer created;
}
