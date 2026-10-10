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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskDisplayStatus;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MemberTaskInProgressTasks {

	private final MemberTaskMapper memberTaskMapper;

	public MemberTaskInProgressTasks(MemberTaskMapper memberTaskMapper) {
		this.memberTaskMapper = memberTaskMapper;
	}

	public List<MemberTask> listInProgressByTypeDetail(long companyId, String typeDetail, long nowEpochSec) {
		List<MemberTask> rows =
				memberTaskMapper.selectList(
						new QueryWrapper<MemberTask>()
								.eq("company_id", companyId)
								.eq("type_detail", typeDetail)
								.eq("published", 1)
								.eq("off_shelf", 0));
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		List<MemberTask> out = new ArrayList<>();
		for (MemberTask task : rows) {
			if (isInProgress(task, nowEpochSec)) {
				out.add(task);
			}
		}
		return out;
	}

	public MemberTask loadInProgressTask(long companyId, long taskId, String typeDetail, long nowEpochSec) {
		MemberTask task =
				memberTaskMapper.selectOne(
						new QueryWrapper<MemberTask>()
								.eq("company_id", companyId)
								.eq("task_id", taskId)
								.last("LIMIT 1"));
		if (task == null || !typeDetail.equals(task.getTypeDetail()) || !isInProgress(task, nowEpochSec)) {
			return null;
		}
		return task;
	}

	static boolean isInProgress(MemberTask task, long nowEpochSec) {
		int published = task.getPublished() != null ? task.getPublished() : 0;
		int offShelf = task.getOffShelf() != null ? task.getOffShelf() : 0;
		int begin = task.getBeginTime() != null ? task.getBeginTime() : 0;
		int end = task.getEndTime() != null ? task.getEndTime() : 0;
		return MemberTaskDisplayStatus.IN_PROGRESS.equals(
				MemberTaskDisplayStatus.resolve(published, offShelf, begin, end, nowEpochSec));
	}
}
