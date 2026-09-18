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

package cn.shopex.ecshopx.promotions.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 大转盘奖项日库存占用 */
@Data
@MpTable(
		value = "promotions_turntable_prize_day_stock",
		comment = "大转盘奖项日库存占用",
		uniqueIndexes = {
			@MpIndex(
					name = "uk_turntable_prize_day_stock",
					columns = {"act_id", "prize_id", "day_key"})
		})
public class TurntablePrizeDayStock {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "act_id", columnType = "bigint", comment = "活动ID")
	private Long actId;

	@MpField(value = "prize_id", columnType = "string", length = 64, comment = "奖项稳定ID")
	private String prizeId;

	@MpField(value = "day_key", columnType = "string", length = 16, comment = "业务日YYYY-MM-DD")
	private String dayKey;

	@MpField(value = "reserved_count", columnType = "bigint", comment = "已占用日库存")
	private Long reservedCount = 0L;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
