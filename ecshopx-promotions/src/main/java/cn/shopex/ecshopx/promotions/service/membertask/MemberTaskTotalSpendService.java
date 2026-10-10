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
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskGrantTrigger;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskOrderSnapshot;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SSOT §10.5 · total_spend 累计进度与达标 completion。 */
@Service
public class MemberTaskTotalSpendService {

	private static final Logger log = LoggerFactory.getLogger(MemberTaskTotalSpendService.class);

	private final MemberTaskUserProgressService memberTaskUserProgressService;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;
	private final MemberTaskFirstOrderCompletionPolicy memberTaskOnceCompletionPolicy;
	private final MemberTaskRewardGrantService memberTaskRewardGrantService;

	public MemberTaskTotalSpendService(
			MemberTaskUserProgressService memberTaskUserProgressService,
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskRewardGrantMapper memberTaskRewardGrantMapper,
			MemberTaskFirstOrderCompletionPolicy memberTaskOnceCompletionPolicy,
			MemberTaskRewardGrantService memberTaskRewardGrantService) {
		this.memberTaskUserProgressService = memberTaskUserProgressService;
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskRewardGrantMapper = memberTaskRewardGrantMapper;
		this.memberTaskOnceCompletionPolicy = memberTaskOnceCompletionPolicy;
		this.memberTaskRewardGrantService = memberTaskRewardGrantService;
	}

	@Transactional
	public void onPaySuccess(
			MemberTask task, MemberTaskOrderSnapshot order, MemberTaskPurchaseRule rule, int nowSec) {
		if (hasProcessedOrder(task.getTaskId(), order.orderId())) {
			log.debug(
					"member_task total_spend skip companyId={} orderId={} reason=order_already_processed",
					order.companyId(),
					order.orderId());
			return;
		}
		if ("once".equals(task.getFrequencyType())
				&& memberTaskOnceCompletionPolicy.hasActiveFirstOrderCompletion(
						task.getTaskId(), order.userId())) {
			return;
		}
		long progress =
				memberTaskUserProgressService.addAmountFen(
						order.companyId(), task.getTaskId(), order.userId(), order.totalFeeFen(), nowSec);
		if (!rule.matchesAmount(progress)) {
			return;
		}
		writeCompletionAndGrant(task, order, rule, nowSec);
	}

	@Transactional
	public void onRefund(
			MemberTask task,
			MemberTaskOrderSnapshot order,
			MemberTaskPurchaseRule rule,
			long refundFeeFen,
			int nowSec) {
		long progress =
				memberTaskUserProgressService.deductAmountFen(
						task.getTaskId(), order.userId(), refundFeeFen, nowSec);
		if (rule.matchesAmount(progress)) {
			return;
		}
		voidPendingCompletionForOrder(order.companyId(), order.orderId(), task.getTaskId(), nowSec);
	}

	private boolean hasProcessedOrder(long taskId, long orderId) {
		Long n =
				memberTaskCompletionMapper.selectCount(
						new QueryWrapper<MemberTaskCompletion>()
								.eq("task_id", taskId)
								.eq("order_id", orderId));
		return n != null && n > 0L;
	}

	private void writeCompletionAndGrant(
			MemberTask task, MemberTaskOrderSnapshot order, MemberTaskPurchaseRule rule, int nowSec) {
		MemberTaskCompletion completion = new MemberTaskCompletion();
		completion.setCompanyId(order.companyId());
		completion.setTaskId(task.getTaskId());
		completion.setUserId(order.userId());
		completion.setCycleKey("once");
		completion.setCompletedAt(nowSec);
		completion.setOrderId(order.orderId());
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
		grant.setCompanyId(order.companyId());
		grant.setCompletionId(completion.getCompletionId());
		grant.setTaskId(task.getTaskId());
		grant.setUserId(order.userId());
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
		if (rule.needReceipt()) {
			if ("DONE".equals(order.orderStatus()) && order.endTime() > 0L) {
				memberTaskRewardGrantService.grantPendingIfOrderDone(
						order.companyId(), order.orderId(), MemberTaskGrantTrigger.PAY_SUCCESS_AFTER_DONE);
			}
			return;
		}
		memberTaskRewardGrantService.grantPendingImmediate(order.companyId(), order.orderId());
	}

	void voidPendingCompletionForOrder(long companyId, long orderId, long taskId, int nowSec) {
		MemberTaskCompletion completion =
				memberTaskCompletionMapper.selectOne(
						new QueryWrapper<MemberTaskCompletion>()
								.eq("task_id", taskId)
								.eq("order_id", orderId)
								.eq("voided", 0)
								.last("LIMIT 1"));
		if (completion == null) {
			return;
		}
		MemberTaskRewardGrant grant =
				memberTaskRewardGrantMapper.selectOne(
						new QueryWrapper<MemberTaskRewardGrant>()
								.eq("completion_id", completion.getCompletionId())
								.last("LIMIT 1"));
		if (grant != null && !"pending".equals(grant.getStatus())) {
			return;
		}
		int rows =
				memberTaskCompletionMapper.update(
						null,
						new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<MemberTaskCompletion>()
								.eq("completion_id", completion.getCompletionId())
								.eq("voided", 0)
								.set("voided", 1)
								.set("voided_at", nowSec)
								.set("void_reason", "refund")
								.set("updated", nowSec));
		if (rows == 0) {
			return;
		}
		if (grant != null) {
			memberTaskRewardGrantMapper.update(
					null,
					new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<MemberTaskRewardGrant>()
							.eq("grant_id", grant.getGrantId())
							.eq("status", "pending")
							.set("status", "cancelled")
							.set("updated", nowSec));
		}
		log.info(
				"member_task total_spend refund void companyId={} orderId={} taskId={} completionId={}",
				companyId,
				orderId,
				taskId,
				completion.getCompletionId());
	}
}
