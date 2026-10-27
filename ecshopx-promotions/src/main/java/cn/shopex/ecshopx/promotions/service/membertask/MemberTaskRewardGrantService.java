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

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskGrantAttemptResult;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskGrantTrigger;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskOrderSnapshot;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberTaskRewardGrantService {

	private static final Logger log = LoggerFactory.getLogger(MemberTaskRewardGrantService.class);

	private final MemberTaskOrderReadPort memberTaskOrderReadPort;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;
	private final MemberTaskPointGrantPort memberTaskPointGrantPort;
	private final MemberTaskCouponGrantPort memberTaskCouponGrantPort;
	private final ObjectMapper objectMapper;

	public MemberTaskRewardGrantService(
			MemberTaskOrderReadPort memberTaskOrderReadPort,
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskRewardGrantMapper memberTaskRewardGrantMapper,
			MemberTaskPointGrantPort memberTaskPointGrantPort,
			MemberTaskCouponGrantPort memberTaskCouponGrantPort,
			ObjectMapper objectMapper) {
		this.memberTaskOrderReadPort = memberTaskOrderReadPort;
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskRewardGrantMapper = memberTaskRewardGrantMapper;
		this.memberTaskPointGrantPort = memberTaskPointGrantPort;
		this.memberTaskCouponGrantPort = memberTaskCouponGrantPort;
		this.objectMapper = objectMapper;
	}

	public MemberTaskGrantAttemptResult grantPendingIfOrderDone(
			long companyId, long orderId, MemberTaskGrantTrigger trigger) {
		MemberTaskOrderSnapshot order = memberTaskOrderReadPort.get(companyId, orderId);
		if (order == null) {
			log.debug("member_task grant skip companyId={} orderId={} reason=no_order", companyId, orderId);
			return MemberTaskGrantAttemptResult.SKIPPED;
		}
		if (order.memberTaskId() <= 0L) {
			log.debug("member_task grant skip companyId={} orderId={} reason=no_member_task_id", companyId, orderId);
			return MemberTaskGrantAttemptResult.SKIPPED;
		}
		if (!"DONE".equals(order.orderStatus()) || order.endTime() <= 0L) {
			log.debug("member_task grant skip companyId={} orderId={} reason=not_done", companyId, orderId);
			return MemberTaskGrantAttemptResult.SKIPPED_GATE;
		}
		return executePendingForOrder(order, trigger, true);
	}

	public MemberTaskGrantAttemptResult grantPendingImmediate(long companyId, long orderId) {
		MemberTaskOrderSnapshot order = memberTaskOrderReadPort.get(companyId, orderId);
		if (order == null || order.memberTaskId() <= 0L) {
			return MemberTaskGrantAttemptResult.SKIPPED;
		}
		return executePendingForOrder(order, MemberTaskGrantTrigger.PAY_SUCCESS_IMMEDIATE, false);
	}

	/** 非订单完成（profile 等）：按 completion 实发 pending grant，order_id=0。 */
	/** Admin 补发：`failed` → 实发，同行 grant，`retry_count` +1。 */
	@Transactional
	public MemberTaskGrantAttemptResult retryAdminFailedGrant(
			MemberTaskRewardGrant grant, MemberTaskCompletion completion) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int reset =
				memberTaskRewardGrantMapper.update(
						null,
						new UpdateWrapper<MemberTaskRewardGrant>()
								.eq("grant_id", grant.getGrantId())
								.eq("status", "failed")
								.set("status", "pending")
								.set("error_message", null)
								.setSql("retry_count = retry_count + 1")
								.set("updated", now));
		if (reset == 0) {
			return MemberTaskGrantAttemptResult.SKIPPED_NO_PENDING;
		}
		grant.setStatus("pending");
		grant.setRetryCount((grant.getRetryCount() != null ? grant.getRetryCount() : 0) + 1);
		MemberTaskOrderSnapshot snap = orderSnapshotForRetry(grant, completion);
		return executeGrant(grant, snap, MemberTaskGrantTrigger.ADMIN_RETRY);
	}

	public MemberTaskGrantAttemptResult grantPendingImmediateForCompletion(
			long companyId, long userId, long completionId) {
		MemberTaskRewardGrant grant =
				memberTaskRewardGrantMapper.selectOne(
						new QueryWrapper<MemberTaskRewardGrant>()
								.eq("completion_id", completionId)
								.last("LIMIT 1"));
		if (grant == null) {
			return MemberTaskGrantAttemptResult.SKIPPED_NO_PENDING;
		}
		if ("granted".equals(grant.getStatus())) {
			return MemberTaskGrantAttemptResult.ALREADY_GRANTED;
		}
		if (!"pending".equals(grant.getStatus())) {
			return MemberTaskGrantAttemptResult.SKIPPED_NO_PENDING;
		}
		MemberTaskOrderSnapshot snap =
				new MemberTaskOrderSnapshot(
						companyId, 0L, userId, grant.getTaskId(), "DONE", 1L, 0L, 0L, 0L);
		return executeGrant(grant, snap, MemberTaskGrantTrigger.PROFILE_COMPLETE);
	}

	private MemberTaskGrantAttemptResult executePendingForOrder(
			MemberTaskOrderSnapshot order, MemberTaskGrantTrigger trigger, boolean requireDone) {
		MemberTaskCompletion completion =
				memberTaskCompletionMapper.selectOne(
						new QueryWrapper<MemberTaskCompletion>()
								.eq("task_id", order.memberTaskId())
								.eq("order_id", order.orderId())
								.eq("voided", 0)
								.last("LIMIT 1"));
		if (completion == null) {
			return MemberTaskGrantAttemptResult.SKIPPED_NO_PENDING;
		}
		MemberTaskRewardGrant grant =
				memberTaskRewardGrantMapper.selectOne(
						new QueryWrapper<MemberTaskRewardGrant>()
								.eq("completion_id", completion.getCompletionId())
								.last("LIMIT 1"));
		if (grant == null) {
			return MemberTaskGrantAttemptResult.SKIPPED_NO_PENDING;
		}
		if ("granted".equals(grant.getStatus())) {
			return MemberTaskGrantAttemptResult.ALREADY_GRANTED;
		}
		if (!"pending".equals(grant.getStatus())) {
			return MemberTaskGrantAttemptResult.SKIPPED_NO_PENDING;
		}
		if (requireDone && (!"DONE".equals(order.orderStatus()) || order.endTime() <= 0L)) {
			return MemberTaskGrantAttemptResult.SKIPPED_GATE;
		}
		return executeGrant(grant, order, trigger);
	}

	@Transactional
	public MemberTaskGrantAttemptResult executeGrant(
			MemberTaskRewardGrant grant, MemberTaskOrderSnapshot order, MemberTaskGrantTrigger trigger) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int claimed =
				memberTaskRewardGrantMapper.update(
						null,
						new UpdateWrapper<MemberTaskRewardGrant>()
								.eq("grant_id", grant.getGrantId())
								.eq("status", "pending")
								.set("status", "granted")
								.set("granted_at", now)
								.set("updated", now));
		if (claimed == 0) {
			return MemberTaskGrantAttemptResult.ALREADY_GRANTED;
		}
		log.info(
				"member_task grant attempt companyId={} orderId={} taskId={} completionId={} grantId={} trigger={}",
				order.companyId(),
				order.orderId(),
				grant.getTaskId(),
				grant.getCompletionId(),
				grant.getGrantId(),
				trigger);
		try {
			applyRewards(grant, order);
			return MemberTaskGrantAttemptResult.GRANTED;
		} catch (Exception e) {
			String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
			if (msg.length() > 512) {
				msg = msg.substring(0, 512);
			}
			memberTaskRewardGrantMapper.update(
					null,
					new UpdateWrapper<MemberTaskRewardGrant>()
							.eq("grant_id", grant.getGrantId())
							.set("status", "failed")
							.set("error_message", msg)
							.set("updated", now));
			log.warn(
					"member_task grant failed companyId={} orderId={} taskId={} completionId={} grantId={} trigger={} error_message={}",
					order.companyId(),
					order.orderId(),
					grant.getTaskId(),
					grant.getCompletionId(),
					grant.getGrantId(),
					trigger,
					msg);
			return MemberTaskGrantAttemptResult.FAILED;
		}
	}

	private MemberTaskOrderSnapshot orderSnapshotForRetry(
			MemberTaskRewardGrant grant, MemberTaskCompletion completion) {
		long companyId =
				grant.getCompanyId() != null ? grant.getCompanyId() : completion.getCompanyId();
		long orderId =
				completion.getOrderId() != null && completion.getOrderId() > 0L
						? completion.getOrderId()
						: 0L;
		if (orderId > 0L) {
			MemberTaskOrderSnapshot order = memberTaskOrderReadPort.get(companyId, orderId);
			if (order != null) {
				return order;
			}
		}
		long userId =
				grant.getUserId() != null && grant.getUserId() > 0L
						? grant.getUserId()
						: completion.getUserId() != null ? completion.getUserId() : 0L;
		long taskId = grant.getTaskId() != null ? grant.getTaskId() : completion.getTaskId();
		return new MemberTaskOrderSnapshot(companyId, orderId, userId, taskId, "DONE", 1L, 0L, 0L, 0L);
	}

	private void applyRewards(MemberTaskRewardGrant grant, MemberTaskOrderSnapshot order) {
		String snap = grant.getRewardSnapshot();
		if (snap == null || snap.isBlank()) {
			return;
		}
		long rewardUserId =
				grant.getUserId() != null && grant.getUserId() > 0L
						? grant.getUserId()
						: order.userId();
		try {
			JsonNode node = objectMapper.readTree(snap);
			JsonNode points = node.get("points");
			if (points != null && !points.isNull() && points.isNumber() && points.asInt() > 0) {
				memberTaskPointGrantPort.grantPoints(
						order.companyId(), rewardUserId, points.asInt(), order.orderId());
			}
			JsonNode coupon = node.get("coupon_id");
			if (coupon != null && !coupon.isNull() && coupon.isNumber() && coupon.asLong() > 0L) {
				memberTaskCouponGrantPort.grantCoupon(order.companyId(), rewardUserId, coupon.asLong());
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}
