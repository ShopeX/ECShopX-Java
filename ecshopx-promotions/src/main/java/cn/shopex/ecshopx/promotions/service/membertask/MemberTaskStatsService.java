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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskStatsMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/** 任务完成统计（ECX-10433 · G31 voided=0）。 */
@Service
public class MemberTaskStatsService {

	private final MemberTaskMapper memberTaskMapper;
	private final MemberTaskStatsMapper memberTaskStatsMapper;
	private final MessageSource messageSource;

	public MemberTaskStatsService(
			MemberTaskMapper memberTaskMapper,
			MemberTaskStatsMapper memberTaskStatsMapper,
			MessageSource messageSource) {
		this.memberTaskMapper = memberTaskMapper;
		this.memberTaskStatsMapper = memberTaskStatsMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> stats(long companyId, long taskId, Locale locale) {
		MemberTask task =
				memberTaskMapper.selectOne(
						new QueryWrapper<MemberTask>()
								.eq("task_id", taskId)
								.eq("company_id", companyId));
		if (task == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.member_task.task_not_found", null, "任务不存在", locale),
					404);
		}
		long completionCount = memberTaskStatsMapper.countCompletions(companyId, taskId);
		long completionUserCount = memberTaskStatsMapper.countCompletionUsers(companyId, taskId);
		Map<String, Long> grantByStatus = new LinkedHashMap<>();
		grantByStatus.put("pending", 0L);
		grantByStatus.put("granted", 0L);
		grantByStatus.put("failed", 0L);
		grantByStatus.put("cancelled", 0L);
		List<Map<String, Object>> rows = memberTaskStatsMapper.countGrantsByStatus(companyId, taskId);
		for (Map<String, Object> row : rows) {
			Object st = row.get("status");
			Object cnt = row.get("cnt");
			if (st == null || cnt == null) {
				continue;
			}
			long n = cnt instanceof Number num ? num.longValue() : Long.parseLong(cnt.toString());
			grantByStatus.put(st.toString(), n);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("task_id", taskId);
		long attributedSpendFen =
				memberTaskStatsMapper.sumAttributedOrderSpendFen(companyId, taskId);
		long attributedJoinCount =
				memberTaskStatsMapper.countAttributedJoinUsers(companyId, taskId);
		out.put("completion_count", completionCount);
		out.put("completion_user_count", completionUserCount);
		out.put("grant_status_counts", grantByStatus);
		out.put("attributed_spend_fen", attributedSpendFen);
		out.put("attributed_join_count", attributedJoinCount);
		return out;
	}
}
