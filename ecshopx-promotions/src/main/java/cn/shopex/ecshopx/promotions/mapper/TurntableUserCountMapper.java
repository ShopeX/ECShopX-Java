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

import cn.shopex.ecshopx.promotions.domain.TurntableUserCount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TurntableUserCountMapper extends BaseMapper<TurntableUserCount> {

	@Update(
			"""
			UPDATE promotions_turntable_user_count
			SET total_count = total_count + 1, updated = #{updated}
			WHERE company_id = #{companyId}
			  AND user_id = #{userId}
			  AND act_id = #{actId}
			  AND (#{limitTotal} = 0 OR total_count < #{limitTotal})
			""")
	int casIncrTotal(
			@Param("companyId") long companyId,
			@Param("userId") long userId,
			@Param("actId") long actId,
			@Param("limitTotal") long limitTotal,
			@Param("updated") int updated);

	@Update(
			"""
			UPDATE promotions_turntable_user_count
			SET total_count = total_count - 1, updated = #{updated}
			WHERE company_id = #{companyId}
			  AND user_id = #{userId}
			  AND act_id = #{actId}
			  AND total_count > 0
			""")
	int casDecrTotal(
			@Param("companyId") long companyId,
			@Param("userId") long userId,
			@Param("actId") long actId,
			@Param("updated") int updated);
}
