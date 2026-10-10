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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MemberTaskBrowseCompletionService {

	private final MemberTaskInProgressTasks memberTaskInProgressTasks;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskImmediateCompletionService memberTaskImmediateCompletionService;
	private final ObjectMapper objectMapper;

	public MemberTaskBrowseCompletionService(
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
	public void reportBrowse(long companyId, long userId, long taskId, String page, int staySeconds) {
		if (companyId <= 0L || userId <= 0L || taskId <= 0L || !StringUtils.hasText(page)) {
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		MemberTask task =
				memberTaskInProgressTasks.loadInProgressTask(companyId, taskId, "browse", now);
		if (task == null) {
			return;
		}
		MemberTaskBrowseRule rule = MemberTaskBrowseRule.parse(task.getRuleConfig(), objectMapper);
		if (!rule.matchesReport(page.trim(), Math.max(0, staySeconds))) {
			return;
		}
		Long existing =
				memberTaskCompletionMapper.selectCount(
						new QueryWrapper<MemberTaskCompletion>()
								.eq("task_id", task.getTaskId())
								.eq("user_id", userId)
								.eq("voided", 0));
		if (existing != null && existing > 0L) {
			return;
		}
		memberTaskImmediateCompletionService.tryComplete(task, userId, 0L, 0L, (int) now);
	}
}
