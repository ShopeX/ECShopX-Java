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

package cn.shopex.ecshopx.promotions.mapper;

import cn.shopex.ecshopx.promotions.domain.TurntablePrizeDayStock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TurntablePrizeDayStockMapper extends BaseMapper<TurntablePrizeDayStock> {

	@Update(
			"""
			UPDATE promotions_turntable_prize_day_stock
			SET reserved_count = reserved_count + 1, updated = #{updated}
			WHERE act_id = #{actId}
			  AND prize_id = #{prizeId}
			  AND day_key = #{dayKey}
			  AND reserved_count < #{dailyStock}
			""")
	int casReserve(
			@Param("actId") long actId,
			@Param("prizeId") String prizeId,
			@Param("dayKey") String dayKey,
			@Param("dailyStock") int dailyStock,
			@Param("updated") int updated);

	@Update(
			"""
			UPDATE promotions_turntable_prize_day_stock
			SET reserved_count = reserved_count - 1, updated = #{updated}
			WHERE act_id = #{actId}
			  AND prize_id = #{prizeId}
			  AND day_key = #{dayKey}
			  AND reserved_count > 0
			""")
	int casRelease(
			@Param("actId") long actId,
			@Param("prizeId") String prizeId,
			@Param("dayKey") String dayKey,
			@Param("updated") int updated);
}
