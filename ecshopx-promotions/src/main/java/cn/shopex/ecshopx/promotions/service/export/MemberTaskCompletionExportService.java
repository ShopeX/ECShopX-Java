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

package cn.shopex.ecshopx.promotions.service.export;

import cn.shopex.ecshopx.common.dispatch.MemberTaskExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskGrantStatusDisplay;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class MemberTaskCompletionExportService {

	private final MemberTaskMapper memberTaskMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final MemberTaskExportFileJobDispatchPublisher memberTaskExportFileJobDispatchPublisher;
	private final MessageSource messageSource;

	public MemberTaskCompletionExportService(
			MemberTaskMapper memberTaskMapper,
			OperatorsQueryService operatorsQueryService,
			MemberTaskExportFileJobDispatchPublisher memberTaskExportFileJobDispatchPublisher,
			MessageSource messageSource) {
		this.memberTaskMapper = memberTaskMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.memberTaskExportFileJobDispatchPublisher = memberTaskExportFileJobDispatchPublisher;
		this.messageSource = messageSource;
	}

	public void exportCompletions(
			long companyId,
			long operatorId,
			long merchantId,
			long taskId,
			String grantStatusRaw,
			String datapassBlockHeader,
			Locale locale) {
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
		long supplierId;
		if (operatorId <= 0L) {
			supplierId = 0L;
		} else {
			LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
			filter.put("company_id", companyId);
			filter.put("operator_id", operatorId);
			filter.put("operator_type", "supplier");
			Map<String, Object> supplier = operatorsQueryService.getInfo(filter);
			supplierId = (supplier != null && !supplier.isEmpty()) ? operatorId : 0L;
		}
		String grantStatus =
				MemberTaskGrantStatusDisplay.normalizeGrantStatusFilter(grantStatusRaw);
		memberTaskExportFileJobDispatchPublisher.enqueue(
				companyId,
				operatorId,
				merchantId,
				supplierId,
				taskId,
				datapassBlockHeader,
				grantStatus);
	}
}
