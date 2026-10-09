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

/** 会员任务累计进度（SSOT §3.5 · 切片 0 骨架）。 */
@Data
@MpTable(
		value = "member_task_user_progress",
		comment = "会员任务累计进度",
		uniqueIndexes = {@MpIndex(name = "uk_mtup_task_user", columns = {"task_id", "user_id"})})
public class MemberTaskUserProgress {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "task_id", columnType = "bigint")
	private Long taskId;

	@MpField(value = "user_id", columnType = "bigint")
	private Long userId;

	@MpField(value = "amount_fen", columnType = "bigint", defaultValue = "0")
	private Long amountFen;

	@MpField(value = "updated", columnType = "integer", defaultValue = "0")
	private Integer updated;
}
