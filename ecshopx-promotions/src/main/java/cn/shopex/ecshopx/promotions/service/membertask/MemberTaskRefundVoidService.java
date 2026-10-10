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
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskOrderSnapshot;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MemberTaskRefundVoidService {

	private static final Logger log = LoggerFactory.getLogger(MemberTaskRefundVoidService.class);

	private final MemberTaskOrderReadPort memberTaskOrderReadPort;
	private final MemberTaskMapper memberTaskMapper;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;
	private final MemberTaskTotalSpendService memberTaskTotalSpendService;
	private final ObjectMapper objectMapper;

	public MemberTaskRefundVoidService(
			MemberTaskOrderReadPort memberTaskOrderReadPort,
			MemberTaskMapper memberTaskMapper,
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskRewardGrantMapper memberTaskRewardGrantMapper,
			MemberTaskTotalSpendService memberTaskTotalSpendService,
			ObjectMapper objectMapper) {
		this.memberTaskOrderReadPort = memberTaskOrderReadPort;
		this.memberTaskMapper = memberTaskMapper;
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskRewardGrantMapper = memberTaskRewardGrantMapper;
		this.memberTaskTotalSpendService = memberTaskTotalSpendService;
		this.objectMapper = objectMapper;
	}

	public void apply(Map<String, Object> payload) {
		long companyId = longVal(payload.get("company_id"));
		long orderId = longVal(payload.get("order_id"));
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		if (!StringUtils.hasText(String.valueOf(payload.getOrDefault("refund_success_time", "")).trim())) {
			return;
		}
		long refundFee = longVal(payload.get("refund_fee"));
		if (refundFee <= 0L) {
			refundFee = longVal(payload.get("refunded_fee"));
		}
		voidPendingIfNeeded(companyId, orderId, refundFee, longVal(payload.get("refunded_fee")));
	}

	@Transactional
	public void voidPendingIfNeeded(long companyId, long orderId, long refundFeeFen, long refundedFeeFen) {
		MemberTaskOrderSnapshot order = memberTaskOrderReadPort.get(companyId, orderId);
		if (order == null || order.memberTaskId() <= 0L) {
			return;
		}
		MemberTask task =
				memberTaskMapper.selectOne(
						new QueryWrapper<MemberTask>()
								.eq("task_id", order.memberTaskId())
								.eq("company_id", companyId)
								.last("LIMIT 1"));
		MemberTaskPurchaseRule rule =
				task == null
						? new MemberTaskPurchaseRule(false, 0L)
						: MemberTaskPurchaseRule.parse(task.getRuleConfig(), objectMapper);
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (task != null && "total_spend".equals(task.getTypeDetail())) {
			memberTaskTotalSpendService.onRefund(task, order, rule, refundFeeFen, now);
			return;
		}
		if (rule.matchesRemainingAfterRefund(order.totalFeeFen(), refundedFeeFen)) {
			log.debug(
					"member_task refund skip void companyId={} orderId={} reason=still_meets_amount remaining={}",
					companyId,
					orderId,
					Math.max(0L, order.totalFeeFen() - Math.max(0L, refundedFeeFen)));
			return;
		}
		MemberTaskCompletion completion =
				memberTaskCompletionMapper.selectOne(
						new QueryWrapper<MemberTaskCompletion>()
								.eq("task_id", order.memberTaskId())
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
						new UpdateWrapper<MemberTaskCompletion>()
								.eq("completion_id", completion.getCompletionId())
								.eq("voided", 0)
								.set("voided", 1)
								.set("voided_at", now)
								.set("void_reason", "refund")
								.set("updated", now));
		if (rows == 0) {
			return;
		}
		if (grant != null) {
			memberTaskRewardGrantMapper.update(
					null,
					new UpdateWrapper<MemberTaskRewardGrant>()
							.eq("grant_id", grant.getGrantId())
							.eq("status", "pending")
							.set("status", "cancelled")
							.set("updated", now));
		}
		log.info(
				"member_task refund void companyId={} orderId={} taskId={} completionId={}",
				companyId,
				orderId,
				order.memberTaskId(),
				completion.getCompletionId());
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
