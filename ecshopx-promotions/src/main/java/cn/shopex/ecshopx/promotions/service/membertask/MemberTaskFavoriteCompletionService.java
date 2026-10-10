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
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberTaskFavoriteCompletionService {

	private final MemberTaskInProgressTasks memberTaskInProgressTasks;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskImmediateCompletionService memberTaskImmediateCompletionService;
	private final ObjectMapper objectMapper;

	public MemberTaskFavoriteCompletionService(
			MemberTaskInProgressTasks memberTaskInProgressTasks,
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskImmediateCompletionService memberTaskImmediateCompletionService,
			ObjectMapper objectMapper) {
		this.memberTaskInProgressTasks = memberTaskInProgressTasks;
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskImmediateCompletionService = memberTaskImmediateCompletionService;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public void onItemFavorited(long companyId, long userId, long itemId, long distributorId) {
		if (companyId <= 0L || userId <= 0L || itemId <= 0L) {
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		List<MemberTask> tasks =
				memberTaskInProgressTasks.listInProgressByTypeDetail(companyId, "favorite", now);
		for (MemberTask task : tasks) {
			MemberTaskFavoriteRule rule =
					MemberTaskFavoriteRule.parse(task.getRuleConfig(), objectMapper);
			if (!rule.matches(itemId, distributorId)) {
				continue;
			}
			Long existing =
					memberTaskCompletionMapper.selectCount(
							new QueryWrapper<MemberTaskCompletion>()
									.eq("task_id", task.getTaskId())
									.eq("user_id", userId)
									.eq("voided", 0));
			if (existing != null && existing > 0L) {
				continue;
			}
			memberTaskImmediateCompletionService.tryComplete(task, userId, 0L, 0L, (int) now);
		}
	}
}
