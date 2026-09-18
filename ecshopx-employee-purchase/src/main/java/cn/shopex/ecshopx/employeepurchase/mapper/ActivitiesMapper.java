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

package cn.shopex.ecshopx.employeepurchase.mapper;

import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivitiesMapper extends BaseMapper<Activities> {

	/**
	 * 统计日 0 点（Unix 秒）处于有效活动窗口内的内购活动 id 列表。relative_* 为 NULL 时该 OR 支不参与匹配。
	 */
	@Select(
			"SELECT id FROM employee_purchase_activities WHERE status = 'active' AND ("
					+ "(employee_begin_time < #{start} AND employee_end_time > #{start}) OR "
					+ "(relative_begin_time IS NOT NULL AND relative_end_time IS NOT NULL AND "
					+ "relative_begin_time < #{start} AND relative_end_time > #{start}))")
	List<Long> selectActiveIdsForCubeStatistic(@Param("start") long start);
}
