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

/** 会员任务完成记录（SSOT §3.5 · 切片 0 骨架）。 */
@Data
@MpTable(
		value = "member_task_completion",
		comment = "会员任务完成记录",
		indexes = {
			@MpIndex(name = "idx_mtc_task_user_cycle", columns = {"task_id", "user_id", "cycle_key", "voided"}),
			@MpIndex(name = "idx_mtc_task_completed", columns = {"task_id", "voided", "completed_at"}),
			@MpIndex(name = "idx_mtc_task_order", columns = {"task_id", "order_id"})
		})
public class MemberTaskCompletion {

	@MpId(value = "completion_id", type = IdType.AUTO, columnType = "bigint")
	private Long completionId;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "task_id", columnType = "bigint")
	private Long taskId;

	@MpField(value = "user_id", columnType = "bigint")
	private Long userId;

	@MpField(value = "cycle_key", columnType = "string", length = 32)
	private String cycleKey;

	@MpField(value = "completed_at", columnType = "integer")
	private Integer completedAt;

	@MpField(value = "order_id", columnType = "bigint", nullable = true)
	private Long orderId;

	@MpField(value = "invitee_user_id", columnType = "bigint", defaultValue = "0")
	private Long inviteeUserId;

	@MpField(value = "voided", columnType = "integer", defaultValue = "0")
	private Integer voided;

	@MpField(value = "voided_at", columnType = "integer", defaultValue = "0")
	private Integer voidedAt;

	@MpField(value = "void_reason", columnType = "string", length = 64, nullable = true)
	private String voidReason;

	@MpField(value = "created", columnType = "integer", defaultValue = "0")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", defaultValue = "0")
	private Integer updated;
}
