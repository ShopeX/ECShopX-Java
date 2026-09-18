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

import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TurntableLogMapper extends BaseMapper<TurntableLog> {

	@Select(
			"""
			SELECT * FROM promotions_turntable_log
			WHERE status = 'PROCESSING'
			  AND updated <= #{cutoffEpoch}
			ORDER BY updated ASC, id ASC
			LIMIT #{limit}
			""")
	List<TurntableLog> selectStuckProcessing(
			@Param("cutoffEpoch") int cutoffEpoch, @Param("limit") int limit);

	@Select(
			"""
			SELECT COUNT(1) FROM promotions_turntable_log
			WHERE act_id = #{actId}
			  AND status IN ('SUCCESS', 'GRANT_FAILED')
			""")
	Long countAllByActId(@Param("actId") long actId);

	@Select(
			"""
			SELECT COUNT(DISTINCT user_id) FROM promotions_turntable_log
			WHERE act_id = #{actId}
			  AND status IN ('SUCCESS', 'GRANT_FAILED')
			""")
	Long countDistinctUserByActId(@Param("actId") long actId);

	@Select(
			"""
			SELECT COUNT(1) FROM promotions_turntable_log
			WHERE act_id = #{actId}
			  AND status = 'SUCCESS'
			  AND prize_type <> 'thanks'
			""")
	Long countNonThanksByActId(@Param("actId") long actId);

	@Select(
			"""
			SELECT COUNT(DISTINCT user_id) FROM promotions_turntable_log
			WHERE act_id = #{actId}
			  AND status = 'SUCCESS'
			  AND prize_type <> 'thanks'
			""")
	Long countDistinctUserNonThanksByActId(@Param("actId") long actId);
}
