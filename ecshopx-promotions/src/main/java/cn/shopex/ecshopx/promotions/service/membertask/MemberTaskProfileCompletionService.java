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
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskDisplayStatus;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberTaskProfileCompletionService {

	private static final Logger log = LoggerFactory.getLogger(MemberTaskProfileCompletionService.class);

	private final MemberTaskProfileReadPort memberTaskProfileReadPort;
	private final MemberTaskMapper memberTaskMapper;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;
	private final MemberTaskRewardGrantService memberTaskRewardGrantService;

	public MemberTaskProfileCompletionService(
			MemberTaskProfileReadPort memberTaskProfileReadPort,
			MemberTaskMapper memberTaskMapper,
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskRewardGrantMapper memberTaskRewardGrantMapper,
			MemberTaskRewardGrantService memberTaskRewardGrantService) {
		this.memberTaskProfileReadPort = memberTaskProfileReadPort;
		this.memberTaskMapper = memberTaskMapper;
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskRewardGrantMapper = memberTaskRewardGrantMapper;
		this.memberTaskRewardGrantService = memberTaskRewardGrantService;
	}

	@Transactional
	public void apply(Map<String, Object> payload) {
		long companyId = longVal(payload.get("company_id"));
		long userId = longVal(payload.get("user_id"));
		log.debug("member_task profile update_member companyId={} userId={}", companyId, userId);
		if (companyId <= 0L || userId <= 0L) {
			return;
		}
		if (!MemberTaskProfileCompleteness.isComplete(
				memberTaskProfileReadPort.loadOpenMemberInfoFields(companyId),
				memberTaskProfileReadPort.loadMemberInfo(companyId, userId))) {
			log.debug(
					"member_task profile skip companyId={} userId={} reason=profile_incomplete",
					companyId,
					userId);
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		List<MemberTask> tasks =
				memberTaskMapper.selectList(
						new QueryWrapper<MemberTask>()
								.eq("company_id", companyId)
								.eq("type_detail", "profile")
								.eq("published", 1)
								.eq("off_shelf", 0));
		if (tasks == null || tasks.isEmpty()) {
			return;
		}
		for (MemberTask task : tasks) {
			tryComplete(task, userId, now);
		}
	}

	void tryComplete(MemberTask task, long userId, long nowEpochSec) {
		if (task == null || task.getTaskId() == null) {
			return;
		}
		int published = task.getPublished() != null ? task.getPublished() : 0;
		int offShelf = task.getOffShelf() != null ? task.getOffShelf() : 0;
		int begin = task.getBeginTime() != null ? task.getBeginTime() : 0;
		int end = task.getEndTime() != null ? task.getEndTime() : 0;
		String display = MemberTaskDisplayStatus.resolve(published, offShelf, begin, end, nowEpochSec);
		if (!MemberTaskDisplayStatus.IN_PROGRESS.equals(display)) {
			return;
		}
		Long existing =
				memberTaskCompletionMapper.selectCount(
						new QueryWrapper<MemberTaskCompletion>()
								.eq("task_id", task.getTaskId())
								.eq("user_id", userId)
								.eq("voided", 0));
		if (existing != null && existing > 0L) {
			log.debug(
					"member_task profile skip companyId={} userId={} taskId={} reason=already_completed",
					task.getCompanyId(),
					userId,
					task.getTaskId());
			return;
		}
		int nowSec = (int) nowEpochSec;
		MemberTaskCompletion completion = new MemberTaskCompletion();
		completion.setCompanyId(task.getCompanyId());
		completion.setTaskId(task.getTaskId());
		completion.setUserId(userId);
		completion.setCycleKey("once");
		completion.setCompletedAt(nowSec);
		completion.setOrderId(0L);
		completion.setInviteeUserId(0L);
		completion.setVoided(0);
		completion.setVoidedAt(0);
		completion.setCreated(nowSec);
		completion.setUpdated(nowSec);
		try {
			memberTaskCompletionMapper.insert(completion);
		} catch (DuplicateKeyException e) {
			return;
		}
		MemberTaskRewardGrant grant = new MemberTaskRewardGrant();
		grant.setCompanyId(task.getCompanyId());
		grant.setCompletionId(completion.getCompletionId());
		grant.setTaskId(task.getTaskId());
		grant.setUserId(userId);
		grant.setStatus("pending");
		grant.setRewardSnapshot(task.getRewardConfig());
		grant.setGrantedAt(0);
		grant.setRetryCount(0);
		grant.setCreated(nowSec);
		grant.setUpdated(nowSec);
		try {
			memberTaskRewardGrantMapper.insert(grant);
		} catch (DuplicateKeyException e) {
			return;
		}
		memberTaskRewardGrantService.grantPendingImmediateForCompletion(
				task.getCompanyId(), userId, completion.getCompletionId());
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
