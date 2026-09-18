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

import cn.shopex.ecshopx.promotions.domain.TurntableUserDayCount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TurntableUserDayCountMapper extends BaseMapper<TurntableUserDayCount> {

	@Update(
			"""
			UPDATE promotions_turntable_user_day_count
			SET day_count = day_count + 1, updated = #{updated}
			WHERE company_id = #{companyId}
			  AND user_id = #{userId}
			  AND act_id = #{actId}
			  AND day_key = #{dayKey}
			  AND (#{limitDay} = 0 OR day_count < #{limitDay})
			""")
	int casIncrDay(
			@Param("companyId") long companyId,
			@Param("userId") long userId,
			@Param("actId") long actId,
			@Param("dayKey") String dayKey,
			@Param("limitDay") long limitDay,
			@Param("updated") int updated);

	@Update(
			"""
			UPDATE promotions_turntable_user_day_count
			SET day_count = day_count - 1, updated = #{updated}
			WHERE company_id = #{companyId}
			  AND user_id = #{userId}
			  AND act_id = #{actId}
			  AND day_key = #{dayKey}
			  AND day_count > 0
			""")
	int casDecrDay(
			@Param("companyId") long companyId,
			@Param("userId") long userId,
			@Param("actId") long actId,
			@Param("dayKey") String dayKey,
			@Param("updated") int updated);
}
