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

package cn.shopex.ecshopx.salesperson.mapper;

import cn.shopex.ecshopx.salesperson.domain.SalespersonTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SalespersonTaskMapper extends BaseMapper<SalespersonTask> {

	int countOverlappingAllDistributorTasks(@Param("companyId") long companyId, @Param("taskType") int taskType,
			@Param("startTime") long startTime, @Param("endTime") long endTime,
			@Param("excludeTaskId") Long excludeTaskId);

	int countOverlappingPartialTasks(@Param("companyId") long companyId, @Param("taskType") int taskType,
			@Param("startTime") long startTime, @Param("endTime") long endTime,
			@Param("distributorIds") List<Long> distributorIds,
			@Param("excludeTaskId") Long excludeTaskId);

	@Select("SELECT st.task_id, st.company_id, st.start_time, st.end_time, st.task_name, st.task_type, st.task_quota, "
			+ "st.pics, st.task_content, st.use_all_distributor, st.disabled, st.created, st.updated "
			+ "FROM salesperson_task st "
			+ "LEFT JOIN salesperson_task_rel_distributor strd ON st.task_id = strd.task_id "
			+ "WHERE st.company_id = #{companyId} AND st.start_time <= #{now} AND st.end_time >= #{now} "
			+ "AND st.task_type = #{taskType} "
			+ "AND ((COALESCE(st.use_all_distributor, 0) = 1) OR (COALESCE(st.use_all_distributor, 0) = 0 "
			+ "AND strd.distributor_id = #{distributorId})) "
			+ "LIMIT 1")
	SalespersonTask selectActiveTaskForDistributor(@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("taskType") int taskType,
			@Param("now") long now);
}
