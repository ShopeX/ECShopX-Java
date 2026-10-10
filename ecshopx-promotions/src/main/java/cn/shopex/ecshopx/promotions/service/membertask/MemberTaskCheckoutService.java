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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateMemberTaskExtendPort;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskDisplayStatus;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class MemberTaskCheckoutService implements OrderCreateMemberTaskExtendPort {

	private final MemberTaskMapper memberTaskMapper;
	private final MemberTaskFirstOrderCompletionPolicy firstOrderCompletionPolicy;
	private final MessageSource messageSource;

	public MemberTaskCheckoutService(
			MemberTaskMapper memberTaskMapper,
			MemberTaskFirstOrderCompletionPolicy firstOrderCompletionPolicy,
			MessageSource messageSource) {
		this.memberTaskMapper = memberTaskMapper;
		this.firstOrderCompletionPolicy = firstOrderCompletionPolicy;
		this.messageSource = messageSource;
	}

	@Override
	public void applyBeforePersist(NormalOrderCreateParams p) {
		Map<String, Object> od = p.getOrderData();
		Map<String, Object> pr = p.getParams();
		if (od == null) {
			return;
		}
		Object raw = pr != null && pr.get("member_task_id") != null ? pr.get("member_task_id") : od.get("member_task_id");
		long taskId = longVal(raw);
		if (taskId <= 0L) {
			od.put("member_task_id", 0L);
			return;
		}
		long companyId = longVal(od.get("company_id"));
		long userId = longVal(od.get("user_id"));
		Locale locale = LocaleContextHolder.getLocale();
		MemberTask task =
				memberTaskMapper.selectOne(
						new QueryWrapper<MemberTask>()
								.eq("task_id", taskId)
								.eq("company_id", companyId)
								.last("LIMIT 1"));
		if (task == null) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.member_task.checkout_task_invalid", null, "当前任务不可用于下单", locale));
		}
		long now = System.currentTimeMillis() / 1000L;
		int published = task.getPublished() != null ? task.getPublished() : 0;
		int offShelf = task.getOffShelf() != null ? task.getOffShelf() : 0;
		int begin = task.getBeginTime() != null ? task.getBeginTime() : 0;
		int end = task.getEndTime() != null ? task.getEndTime() : 0;
		String display = MemberTaskDisplayStatus.resolve(published, offShelf, begin, end, now);
		if (!MemberTaskDisplayStatus.IN_PROGRESS.equals(display)) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.member_task.checkout_task_invalid", null, "当前任务不可用于下单", locale));
		}
		if ("first_order".equals(task.getTypeDetail())
				&& userId > 0L
				&& firstOrderCompletionPolicy.hasActiveFirstOrderCompletion(taskId, userId)) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.member_task.checkout_first_order_done",
							null,
							"您已完成该首单任务",
							locale));
		}
		od.put("member_task_id", taskId);
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
