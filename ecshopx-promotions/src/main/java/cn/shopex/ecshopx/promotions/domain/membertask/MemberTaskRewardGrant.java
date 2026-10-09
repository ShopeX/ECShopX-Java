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

package cn.shopex.ecshopx.promotions.domain.membertask;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员任务发奖流水（SSOT §3.5 · 切片 0 骨架）。 */
@Data
@MpTable(
		value = "member_task_reward_grant",
		comment = "会员任务发奖流水",
		uniqueIndexes = {@MpIndex(name = "uk_mtrg_completion", columns = {"completion_id"})},
		indexes = {@MpIndex(name = "idx_mtrg_task_status", columns = {"task_id", "status"})})
public class MemberTaskRewardGrant {

	@MpId(value = "grant_id", type = IdType.AUTO, columnType = "bigint")
	private Long grantId;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "completion_id", columnType = "bigint")
	private Long completionId;

	@MpField(value = "task_id", columnType = "bigint")
	private Long taskId;

	@MpField(value = "user_id", columnType = "bigint")
	private Long userId;

	@MpField(value = "status", columnType = "string", length = 16)
	private String status;

	@MpField(value = "error_message", columnType = "string", length = 512, nullable = true)
	private String errorMessage;

	@MpField(value = "reward_snapshot", columnType = "text", nullable = true)
	private String rewardSnapshot;

	@MpField(value = "granted_at", columnType = "integer", defaultValue = "0")
	private Integer grantedAt;

	@MpField(value = "retry_count", columnType = "integer", defaultValue = "0")
	private Integer retryCount;

	@MpField(value = "created", columnType = "integer", defaultValue = "0")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", defaultValue = "0")
	private Integer updated;
}
