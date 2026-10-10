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
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskGrantTrigger;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskOrderSnapshot;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberTaskPaySuccessService {

	private static final Logger log = LoggerFactory.getLogger(MemberTaskPaySuccessService.class);

	private final MemberTaskOrderReadPort memberTaskOrderReadPort;
	private final MemberTaskMapper memberTaskMapper;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;
	private final MemberTaskFirstOrderCompletionPolicy firstOrderCompletionPolicy;
	private final MemberTaskRewardGrantService memberTaskRewardGrantService;
	private final MemberTaskTotalSpendService memberTaskTotalSpendService;
	private final ObjectMapper objectMapper;

	public MemberTaskPaySuccessService(
			MemberTaskOrderReadPort memberTaskOrderReadPort,
			MemberTaskMapper memberTaskMapper,
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskRewardGrantMapper memberTaskRewardGrantMapper,
			MemberTaskFirstOrderCompletionPolicy firstOrderCompletionPolicy,
			MemberTaskRewardGrantService memberTaskRewardGrantService,
			MemberTaskTotalSpendService memberTaskTotalSpendService,
			ObjectMapper objectMapper) {
		this.memberTaskOrderReadPort = memberTaskOrderReadPort;
		this.memberTaskMapper = memberTaskMapper;
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskRewardGrantMapper = memberTaskRewardGrantMapper;
		this.firstOrderCompletionPolicy = firstOrderCompletionPolicy;
		this.memberTaskRewardGrantService = memberTaskRewardGrantService;
		this.memberTaskTotalSpendService = memberTaskTotalSpendService;
		this.objectMapper = objectMapper;
	}

	public void apply(Map<String, Object> payload) {
		long companyId = longVal(payload.get("company_id"));
		long orderId = longVal(payload.get("order_id"));
		log.debug("member_task pay_success companyId={} orderId={}", companyId, orderId);
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		MemberTaskOrderSnapshot order = memberTaskOrderReadPort.get(companyId, orderId);
		if (order == null || order.memberTaskId() <= 0L) {
			log.debug("member_task grant skip companyId={} orderId={} reason=no_member_task_id", companyId, orderId);
			return;
		}
		onPaySuccess(order);
	}

	@Transactional
	public void onPaySuccess(MemberTaskOrderSnapshot order) {
		long now = System.currentTimeMillis() / 1000L;
		MemberTask task =
				memberTaskMapper.selectOne(
						new QueryWrapper<MemberTask>()
								.eq("task_id", order.memberTaskId())
								.eq("company_id", order.companyId())
								.last("LIMIT 1"));
		if (task == null) {
			return;
		}
		int published = task.getPublished() != null ? task.getPublished() : 0;
		int offShelf = task.getOffShelf() != null ? task.getOffShelf() : 0;
		int begin = task.getBeginTime() != null ? task.getBeginTime() : 0;
		int end = task.getEndTime() != null ? task.getEndTime() : 0;
		String display = MemberTaskDisplayStatus.resolve(published, offShelf, begin, end, now);
		if (!MemberTaskDisplayStatus.IN_PROGRESS.equals(display)
				&& !MemberTaskDisplayStatus.PENDING_START.equals(display)) {
			if (!MemberTaskDisplayStatus.IN_PROGRESS.equals(display)) {
				return;
			}
		}
		if (!MemberTaskDisplayStatus.IN_PROGRESS.equals(display)) {
			return;
		}
		MemberTaskPurchaseRule rule = MemberTaskPurchaseRule.parse(task.getRuleConfig(), objectMapper);
		if ("total_spend".equals(task.getTypeDetail())) {
			memberTaskTotalSpendService.onPaySuccess(task, order, rule, (int) now);
			return;
		}
		if ("once".equals(task.getFrequencyType())
				&& firstOrderCompletionPolicy.hasActiveFirstOrderCompletion(task.getTaskId(), order.userId())) {
			log.debug(
					"member_task pay_success skip companyId={} orderId={} reason=once_already_done",
					order.companyId(),
					order.orderId());
			return;
		}
		if (!rule.matchesAmount(order.totalFeeFen())) {
			return;
		}
		int nowSec = (int) now;
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
			log.debug(
					"member_task pay_success skip companyId={} orderId={} reason=duplicate_order",
					order.companyId(),
					order.orderId());
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
