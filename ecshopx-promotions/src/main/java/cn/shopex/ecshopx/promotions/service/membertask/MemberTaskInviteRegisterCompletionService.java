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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H2 · invite_register：注册成功且归因任务有效 → 邀请人 completion。 */
@Service
public class MemberTaskInviteRegisterCompletionService {

	private static final Logger log = LoggerFactory.getLogger(MemberTaskInviteRegisterCompletionService.class);

	private final MemberTaskInProgressTasks memberTaskInProgressTasks;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskFirstOrderCompletionPolicy memberTaskOnceCompletionPolicy;
	private final MemberTaskImmediateCompletionService memberTaskImmediateCompletionService;

	public MemberTaskInviteRegisterCompletionService(
			MemberTaskInProgressTasks memberTaskInProgressTasks,
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskFirstOrderCompletionPolicy memberTaskOnceCompletionPolicy,
			MemberTaskImmediateCompletionService memberTaskImmediateCompletionService) {
		this.memberTaskInProgressTasks = memberTaskInProgressTasks;
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskOnceCompletionPolicy = memberTaskOnceCompletionPolicy;
		this.memberTaskImmediateCompletionService = memberTaskImmediateCompletionService;
	}

	@Transactional
	public void apply(Map<String, Object> payload) {
		long companyId = longVal(payload.get("company_id"));
		long inviteeUserId = longVal(payload.get("user_id"));
		long inviterUserId = longVal(payload.get("inviter_id"));
		long taskId = longVal(payload.get("member_task_id"));
		if (companyId <= 0L || inviteeUserId <= 0L || taskId <= 0L) {
			return;
		}
		if (inviterUserId <= 0L || inviterUserId == inviteeUserId) {
			log.debug(
					"member_task invite_register skip companyId={} invitee={} reason=no_inviter",
					companyId,
					inviteeUserId);
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		MemberTask task =
				memberTaskInProgressTasks.loadInProgressTask(
						companyId, taskId, "invite_register", now);
		if (task == null) {
			return;
		}
		if ("once".equals(task.getFrequencyType())
				&& memberTaskOnceCompletionPolicy.hasActiveFirstOrderCompletion(
						task.getTaskId(), inviterUserId)) {
			return;
		}
		Long dupInvitee =
				memberTaskCompletionMapper.selectCount(
						new QueryWrapper<MemberTaskCompletion>()
								.eq("task_id", task.getTaskId())
								.eq("invitee_user_id", inviteeUserId)
								.eq("voided", 0));
		if (dupInvitee != null && dupInvitee > 0L) {
			return;
		}
		memberTaskImmediateCompletionService.tryComplete(
				task, inviterUserId, 0L, inviteeUserId, (int) now);
	}

	private static long longVal(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
