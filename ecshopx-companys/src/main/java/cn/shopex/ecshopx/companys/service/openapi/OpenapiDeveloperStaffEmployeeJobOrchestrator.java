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

package cn.shopex.ecshopx.companys.service.openapi;

import cn.shopex.ecshopx.common.dispatch.CompanysBundleDispatchJobNames;
import cn.shopex.ecshopx.companys.dispatch.EmployeeJobDispatchPublisher;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OpenapiDeveloperStaffEmployeeJobOrchestrator
		implements cn.shopex.ecshopx.common.openapi.OpenapiDeveloperStaffEmployeeJobOrchestrator {

	private final TransactionTemplate transactionTemplate;
	private final OperatorsMapper operatorsMapper;
	private final EmployeeJobDispatchPublisher employeeJobDispatchPublisher;

	public OpenapiDeveloperStaffEmployeeJobOrchestrator(
			PlatformTransactionManager transactionManager,
			OperatorsMapper operatorsMapper,
			EmployeeJobDispatchPublisher employeeJobDispatchPublisher) {
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.operatorsMapper = operatorsMapper;
		this.employeeJobDispatchPublisher = employeeJobDispatchPublisher;
	}

	@Override
	public void updateDeveloperAndScheduleStaffEmployeeJobs(long companyId, Runnable persistDeveloper) {
		transactionTemplate.executeWithoutResult(
				status -> {
					persistDeveloper.run();
					List<Operators> staff =
							operatorsMapper.selectList(
									new LambdaQueryWrapper<Operators>()
											.eq(Operators::getCompanyId, companyId)
											.eq(Operators::getOperatorType, "staff"));
					List<Map<String, Object>> payloads = new ArrayList<>();
					for (Operators op : staff) {
						LinkedHashMap<String, Object> eventPayload = new LinkedHashMap<>();
						eventPayload.put("company_id", op.getCompanyId());
						eventPayload.put("login_name", op.getLoginName());
						eventPayload.put("mobile", op.getMobile());
						eventPayload.put("user_name", op.getUsername());
						eventPayload.put("password", op.getPassword());
						eventPayload.put("synctype", "add");
						payloads.add(Map.copyOf(eventPayload));
					}
					final List<Map<String, Object>> toEnqueue = List.copyOf(payloads);
					TransactionSynchronizationManager.registerSynchronization(
							new TransactionSynchronization() {
								@Override
								public void afterCommit() {
									for (Map<String, Object> eventPayload : toEnqueue) {
										employeeJobDispatchPublisher.enqueueAfterCommit(
												CompanysBundleDispatchJobNames.EMPLOYEE_JOB_JOB196, eventPayload);
									}
								}
							});
				});
	}
}
