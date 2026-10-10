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
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 非订单类任务：写 completion + grant 并立即实发。 */
@Service
public class MemberTaskImmediateCompletionService {

	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;
	private final MemberTaskRewardGrantService memberTaskRewardGrantService;

	public MemberTaskImmediateCompletionService(
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskRewardGrantMapper memberTaskRewardGrantMapper,
			MemberTaskRewardGrantService memberTaskRewardGrantService) {
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskRewardGrantMapper = memberTaskRewardGrantMapper;
		this.memberTaskRewardGrantService = memberTaskRewardGrantService;
	}

	@Transactional
	public boolean tryComplete(
			MemberTask task, long rewardUserId, long orderId, long inviteeUserId, int nowSec) {
		if (task == null || task.getTaskId() == null || rewardUserId <= 0L) {
			return false;
		}
		MemberTaskCompletion completion = new MemberTaskCompletion();
		completion.setCompanyId(task.getCompanyId());
		completion.setTaskId(task.getTaskId());
		completion.setUserId(rewardUserId);
		completion.setCycleKey("once");
		completion.setCompletedAt(nowSec);
		completion.setOrderId(orderId);
		completion.setInviteeUserId(inviteeUserId);
		completion.setVoided(0);
		completion.setVoidedAt(0);
		completion.setCreated(nowSec);
		completion.setUpdated(nowSec);
		try {
			memberTaskCompletionMapper.insert(completion);
		} catch (DuplicateKeyException e) {
			return false;
		}
		MemberTaskRewardGrant grant = new MemberTaskRewardGrant();
		grant.setCompanyId(task.getCompanyId());
		grant.setCompletionId(completion.getCompletionId());
		grant.setTaskId(task.getTaskId());
		grant.setUserId(rewardUserId);
		grant.setStatus("pending");
		grant.setRewardSnapshot(task.getRewardConfig());
		grant.setGrantedAt(0);
		grant.setRetryCount(0);
		grant.setCreated(nowSec);
		grant.setUpdated(nowSec);
		try {
			memberTaskRewardGrantMapper.insert(grant);
		} catch (DuplicateKeyException e) {
			return false;
		}
		memberTaskRewardGrantService.grantPendingImmediateForCompletion(
				task.getCompanyId(), rewardUserId, completion.getCompletionId());
		return true;
	}
}
