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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskGrantAttemptResult;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin 人工补发（ECX-10431）：仅 `failed` grant，不新建 completion。 */
@Service
public class MemberTaskGrantRetryService {

	private final MemberTaskMapper memberTaskMapper;
	private final MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskRewardGrantService memberTaskRewardGrantService;
	private final MessageSource messageSource;

	public MemberTaskGrantRetryService(
			MemberTaskMapper memberTaskMapper,
			MemberTaskRewardGrantMapper memberTaskRewardGrantMapper,
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskRewardGrantService memberTaskRewardGrantService,
			MessageSource messageSource) {
		this.memberTaskMapper = memberTaskMapper;
		this.memberTaskRewardGrantMapper = memberTaskRewardGrantMapper;
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskRewardGrantService = memberTaskRewardGrantService;
		this.messageSource = messageSource;
	}

	@Transactional
	public Map<String, Object> retry(long companyId, long taskId, long grantId, Locale locale) {
		ensureTaskExists(companyId, taskId, locale);
		MemberTaskRewardGrant grant = loadGrant(companyId, taskId, grantId, locale);
		MemberTaskCompletion completion = loadCompletion(grant, locale);
		if (completion.getVoided() != null && completion.getVoided() != 0) {
			throw new BadRequestException(
					msg("promotions.member_task.grant_retry_not_allowed", "当前奖励不可补发", locale));
		}
		if (!"failed".equals(grant.getStatus())) {
			throw new BadRequestException(
					msg("promotions.member_task.grant_retry_not_allowed", "当前奖励不可补发", locale));
		}
		MemberTaskGrantAttemptResult result =
				memberTaskRewardGrantService.retryAdminFailedGrant(grant, completion);
		MemberTaskRewardGrant fresh =
				memberTaskRewardGrantMapper.selectById(grant.getGrantId());
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("grant_id", grantId);
		out.put("task_id", taskId);
		out.put("status", fresh != null ? fresh.getStatus() : grant.getStatus());
		out.put("retry_count", fresh != null ? fresh.getRetryCount() : grant.getRetryCount());
		out.put("attempt_result", result.name().toLowerCase());
		return out;
	}

	private void ensureTaskExists(long companyId, long taskId, Locale locale) {
		MemberTask row =
				memberTaskMapper.selectOne(
						new QueryWrapper<MemberTask>()
								.eq("task_id", taskId)
								.eq("company_id", companyId));
		if (row == null) {
			throw new ResourceException(
					msg("promotions.member_task.task_not_found", "任务不存在", locale), 404);
		}
	}

	private MemberTaskRewardGrant loadGrant(
			long companyId, long taskId, long grantId, Locale locale) {
		MemberTaskRewardGrant grant =
				memberTaskRewardGrantMapper.selectOne(
						new QueryWrapper<MemberTaskRewardGrant>()
								.eq("grant_id", grantId)
								.eq("task_id", taskId)
								.eq("company_id", companyId));
		if (grant == null) {
			throw new ResourceException(
					msg("promotions.member_task.grant_not_found", "发奖记录不存在", locale), 404);
		}
		return grant;
	}

	private MemberTaskCompletion loadCompletion(MemberTaskRewardGrant grant, Locale locale) {
		MemberTaskCompletion completion =
				memberTaskCompletionMapper.selectById(grant.getCompletionId());
		if (completion == null) {
			throw new ResourceException(
					msg("promotions.member_task.grant_not_found", "发奖记录不存在", locale), 404);
		}
		return completion;
	}

	private String msg(String key, String defaultMessage, Locale locale) {
		return messageSource.getMessage(key, null, defaultMessage, locale);
	}
}
