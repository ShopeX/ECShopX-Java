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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SSOT §10 / H1 · share_order：好友订单达标，completion 记分享人。 */
@Service
public class MemberTaskShareOrderService {

	private static final Logger log = LoggerFactory.getLogger(MemberTaskShareOrderService.class);

	private final MemberTaskInviterReadPort memberTaskInviterReadPort;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;
	private final MemberTaskFirstOrderCompletionPolicy memberTaskOnceCompletionPolicy;
	private final MemberTaskRewardGrantService memberTaskRewardGrantService;

	public MemberTaskShareOrderService(
			MemberTaskInviterReadPort memberTaskInviterReadPort,
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskRewardGrantMapper memberTaskRewardGrantMapper,
			MemberTaskFirstOrderCompletionPolicy memberTaskOnceCompletionPolicy,
			MemberTaskRewardGrantService memberTaskRewardGrantService) {
		this.memberTaskInviterReadPort = memberTaskInviterReadPort;
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskRewardGrantMapper = memberTaskRewardGrantMapper;
		this.memberTaskOnceCompletionPolicy = memberTaskOnceCompletionPolicy;
		this.memberTaskRewardGrantService = memberTaskRewardGrantService;
	}

	@Transactional
	public void onPaySuccess(
			MemberTask task, MemberTaskOrderSnapshot order, MemberTaskPurchaseRule rule, int nowSec) {
		long sharerUserId =
				memberTaskInviterReadPort.findInviterUserId(order.companyId(), order.userId());
		if (sharerUserId <= 0L || sharerUserId == order.userId()) {
			log.debug(
					"member_task share_order skip companyId={} orderId={} reason=no_sharer buyer={}",
					order.companyId(),
					order.orderId(),
					order.userId());
			return;
		}
		if ("once".equals(task.getFrequencyType())
				&& memberTaskOnceCompletionPolicy.hasActiveFirstOrderCompletion(
						task.getTaskId(), sharerUserId)) {
			return;
		}
		if (!rule.matchesAmount(order.totalFeeFen())) {
			return;
		}
		MemberTaskCompletion completion = new MemberTaskCompletion();
		completion.setCompanyId(order.companyId());
		completion.setTaskId(task.getTaskId());
		completion.setUserId(sharerUserId);
		completion.setCycleKey("once");
		completion.setCompletedAt(nowSec);
		completion.setOrderId(order.orderId());
		completion.setInviteeUserId(order.userId());
		completion.setVoided(0);
		completion.setVoidedAt(0);
		completion.setCreated(nowSec);
		completion.setUpdated(nowSec);
		try {
			memberTaskCompletionMapper.insert(completion);
		} catch (DuplicateKeyException e) {
			log.debug(
					"member_task share_order skip companyId={} orderId={} reason=duplicate_order",
					order.companyId(),
					order.orderId());
			return;
		}
		MemberTaskRewardGrant grant = new MemberTaskRewardGrant();
		grant.setCompanyId(order.companyId());
		grant.setCompletionId(completion.getCompletionId());
		grant.setTaskId(task.getTaskId());
		grant.setUserId(sharerUserId);
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
}
