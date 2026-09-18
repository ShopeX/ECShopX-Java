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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.salesperson.domain.SalespersonTask;
import cn.shopex.ecshopx.salesperson.domain.SalespersonTaskRecord;
import cn.shopex.ecshopx.salesperson.domain.SalespersonTaskRecordLogs;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskMapper;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskRecordLogsMapper;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalespersonTaskCouponCompleteService {

	public static final int TASK_TYPE_USER_WELFARE = 4;

	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final SalespersonTaskMapper salespersonTaskMapper;

	private final SalespersonTaskRecordMapper salespersonTaskRecordMapper;
	private final SalespersonTaskRecordLogsMapper salespersonTaskRecordLogsMapper;
	private final ObjectMapper objectMapper;

	public SalespersonTaskCouponCompleteService(ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			SalespersonTaskMapper salespersonTaskMapper,
			SalespersonTaskRecordMapper salespersonTaskRecordMapper,
			SalespersonTaskRecordLogsMapper salespersonTaskRecordLogsMapper,
			ObjectMapper objectMapper) {
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.salespersonTaskMapper = salespersonTaskMapper;
		this.salespersonTaskRecordMapper = salespersonTaskRecordMapper;
		this.salespersonTaskRecordLogsMapper = salespersonTaskRecordLogsMapper;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean completeGetCoupon(long companyId, long salespersonId, long userId, String type, long id) {
		if (salespersonId <= 0L) {
			return false;
		}
		ShopsRelSalesperson shopRel = shopsRelSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopsRelSalesperson>()
				.eq(ShopsRelSalesperson::getSalespersonId, salespersonId)
				.eq(ShopsRelSalesperson::getStoreType, "distributor")
				.last("LIMIT 1"));
		if (shopRel == null || shopRel.getShopId() == null) {
			return false;
		}
		long distributorId = shopRel.getShopId();
		long now = System.currentTimeMillis() / 1000L;
		SalespersonTask task = salespersonTaskMapper.selectActiveTaskForDistributor(
				companyId, distributorId, TASK_TYPE_USER_WELFARE, now);
		if (task == null || task.getTaskId() == null) {
			return false;
		}
		long taskId = task.getTaskId();
		Map<String, Object> taskParam = new HashMap<>();
		taskParam.put("company_id", companyId);
		taskParam.put("salesperson_id", salespersonId);
		taskParam.put("task_id", taskId);
		taskParam.put("distributor_id", distributorId);
		upsertTaskRecord(taskParam);
		SalespersonTaskRecordLogs logRow = new SalespersonTaskRecordLogs();
		logRow.setCompanyId(companyId);
		logRow.setSalespersonId(salespersonId);
		logRow.setTaskId(taskId);
		logRow.setDistributorId(distributorId);
		Map<String, Object> remark = new HashMap<>();
		remark.put("user_id", userId);
		remark.put("type", type);
		remark.put("id", id);
		try {
			logRow.setRemark(objectMapper.writeValueAsString(remark));
		} catch (JsonProcessingException e) {
			logRow.setRemark("{}");
		}
		int t = (int) Math.min(now, Integer.MAX_VALUE);
		logRow.setCreated(t);
		logRow.setUpdated(t);
		salespersonTaskRecordLogsMapper.insert(logRow);
		return true;
	}

	private void upsertTaskRecord(Map<String, Object> filter) {
		long companyId = ((Number) filter.get("company_id")).longValue();
		long taskId = ((Number) filter.get("task_id")).longValue();
		long distributorId = ((Number) filter.get("distributor_id")).longValue();
		long salespersonId = ((Number) filter.get("salesperson_id")).longValue();
		SalespersonTaskRecord existing = salespersonTaskRecordMapper.selectOne(new LambdaQueryWrapper<SalespersonTaskRecord>()
				.eq(SalespersonTaskRecord::getCompanyId, companyId)
				.eq(SalespersonTaskRecord::getTaskId, taskId)
				.eq(SalespersonTaskRecord::getSalespersonId, salespersonId)
				.eq(SalespersonTaskRecord::getDistributorId, distributorId)
				.last("LIMIT 1"));
		if (existing == null) {
			SalespersonTaskRecord row = new SalespersonTaskRecord();
			row.setCompanyId(companyId);
			row.setTaskId(taskId);
			row.setDistributorId(distributorId);
			row.setSalespersonId(salespersonId);
			row.setTimes(1);
			salespersonTaskRecordMapper.insert(row);
		} else {
			LambdaUpdateWrapper<SalespersonTaskRecord> u = new LambdaUpdateWrapper<>();
			u.eq(SalespersonTaskRecord::getCompanyId, companyId)
					.eq(SalespersonTaskRecord::getTaskId, taskId)
					.eq(SalespersonTaskRecord::getSalespersonId, salespersonId)
					.eq(SalespersonTaskRecord::getDistributorId, distributorId)
					.setSql("times = IFNULL(times,0) + 1");
			salespersonTaskRecordMapper.update(null, u);
		}
	}
}
