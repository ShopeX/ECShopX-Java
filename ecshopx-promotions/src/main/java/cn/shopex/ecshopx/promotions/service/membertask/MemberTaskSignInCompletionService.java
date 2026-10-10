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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** G27 · sign_in：活动期内成功签到 1 次即完成。 */
@Service
public class MemberTaskSignInCompletionService {

	private final MemberTaskInProgressTasks memberTaskInProgressTasks;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskImmediateCompletionService memberTaskImmediateCompletionService;

	public MemberTaskSignInCompletionService(
			MemberTaskInProgressTasks memberTaskInProgressTasks,
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskImmediateCompletionService memberTaskImmediateCompletionService) {
		this.memberTaskInProgressTasks = memberTaskInProgressTasks;
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskImmediateCompletionService = memberTaskImmediateCompletionService;
	}

	@Transactional
	public void onSignInSuccess(long companyId, long userId) {
		if (companyId <= 0L || userId <= 0L) {
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		List<MemberTask> tasks =
				memberTaskInProgressTasks.listInProgressByTypeDetail(companyId, "sign_in", now);
		for (MemberTask task : tasks) {
			tryCompleteOnce(task, userId, (int) now);
		}
	}

	void tryCompleteOnce(MemberTask task, long userId, int nowSec) {
		Long existing =
				memberTaskCompletionMapper.selectCount(
						new QueryWrapper<MemberTaskCompletion>()
								.eq("task_id", task.getTaskId())
								.eq("user_id", userId)
								.eq("voided", 0));
		if (existing != null && existing > 0L) {
			return;
		}
		memberTaskImmediateCompletionService.tryComplete(task, userId, 0L, 0L, nowSec);
	}
}
