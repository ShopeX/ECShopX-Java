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

package cn.shopex.ecshopx.orders.dispatch.profit;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.orders.TradeFinishProfitSalespersonSideEffectsPort;
import cn.shopex.ecshopx.salesperson.domain.Leaderboard;
import cn.shopex.ecshopx.salesperson.domain.LeaderboardDistributor;
import cn.shopex.ecshopx.salesperson.domain.SalespersonTask;
import cn.shopex.ecshopx.salesperson.domain.SalespersonTaskRecord;
import cn.shopex.ecshopx.salesperson.domain.SalespersonTaskRecordLogs;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.LeaderboardDistributorMapper;
import cn.shopex.ecshopx.salesperson.mapper.LeaderboardMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskMapper;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskRecordLogsMapper;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TradeFinishProfitSalespersonSideEffectsService implements TradeFinishProfitSalespersonSideEffectsPort {

	private static final int LEADERBOARD_DEFAULT_YM = 202000;
	private static final int TASK_TYPE_USER_ORDER = 3;

	private final LeaderboardMapper leaderboardMapper;
	private final LeaderboardDistributorMapper leaderboardDistributorMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final SalespersonTaskMapper salespersonTaskMapper;
	private final SalespersonTaskRecordMapper salespersonTaskRecordMapper;
	private final SalespersonTaskRecordLogsMapper salespersonTaskRecordLogsMapper;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;

	public TradeFinishProfitSalespersonSideEffectsService(
			LeaderboardMapper leaderboardMapper,
			LeaderboardDistributorMapper leaderboardDistributorMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			SalespersonTaskMapper salespersonTaskMapper,
			SalespersonTaskRecordMapper salespersonTaskRecordMapper,
			SalespersonTaskRecordLogsMapper salespersonTaskRecordLogsMapper,
			ObjectMapper objectMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.leaderboardMapper = leaderboardMapper;
		this.leaderboardDistributorMapper = leaderboardDistributorMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.salespersonTaskMapper = salespersonTaskMapper;
		this.salespersonTaskRecordMapper = salespersonTaskRecordMapper;
		this.salespersonTaskRecordLogsMapper = salespersonTaskRecordLogsMapper;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	@Override
	public void applyOnPopularizeSeller(
			long companyId,
			long orderDistributorId,
			long popularizeSellerId,
			long payFeeFen,
			long userId,
			long orderId) {
		addSalespersonLeaderboard(companyId, orderDistributorId, popularizeSellerId, payFeeFen);
		addDistributorLeaderboard(companyId, orderDistributorId, payFeeFen);
		transactionTemplate.executeWithoutResult(
				status -> completeUserOrderTaskInTransaction(companyId, popularizeSellerId, userId, orderId));
	}

	private void addSalespersonLeaderboard(long companyId, long distributorId, long salespersonId, long sales) {
		incrementSalespersonBoard(companyId, distributorId, salespersonId, LEADERBOARD_DEFAULT_YM, sales);
		incrementSalespersonBoard(companyId, distributorId, salespersonId, currentYearMonthKey(), sales);
	}

	private void addDistributorLeaderboard(long companyId, long distributorId, long sales) {
		incrementDistributorBoard(companyId, distributorId, LEADERBOARD_DEFAULT_YM, sales);
		incrementDistributorBoard(companyId, distributorId, currentYearMonthKey(), sales);
	}

	private static int currentYearMonthKey() {
		YearMonth ym = YearMonth.now();
		return ym.getYear() * 100 + ym.getMonthValue();
	}

	private void incrementSalespersonBoard(
			long companyId, long distributorId, long salespersonId, int dateKey, long salesDelta) {
		Leaderboard existing =
				leaderboardMapper.selectOne(
						new LambdaQueryWrapper<Leaderboard>()
								.eq(Leaderboard::getCompanyId, companyId)
								.eq(Leaderboard::getDistributorId, distributorId)
								.eq(Leaderboard::getSalespersonId, salespersonId)
								.eq(Leaderboard::getDate, dateKey)
								.last("LIMIT 1"));
		if (existing == null) {
			Leaderboard row = new Leaderboard();
			row.setCompanyId(companyId);
			row.setDistributorId(distributorId);
			row.setSalespersonId(salespersonId);
			row.setDate(dateKey);
			row.setSales(salesDelta);
			row.setNumber(1L);
			leaderboardMapper.insert(row);
		} else {
			LambdaUpdateWrapper<Leaderboard> u = new LambdaUpdateWrapper<>();
			u.eq(Leaderboard::getId, existing.getId())
					.setSql("sales = IFNULL(sales,0) + " + salesDelta)
					.setSql("number = IFNULL(number,0) + 1");
			leaderboardMapper.update(null, u);
		}
	}

	private void incrementDistributorBoard(long companyId, long distributorId, int dateKey, long salesDelta) {
		LeaderboardDistributor existing =
				leaderboardDistributorMapper.selectOne(
						new LambdaQueryWrapper<LeaderboardDistributor>()
								.eq(LeaderboardDistributor::getCompanyId, companyId)
								.eq(LeaderboardDistributor::getDistributorId, distributorId)
								.eq(LeaderboardDistributor::getDate, dateKey)
								.last("LIMIT 1"));
		if (existing == null) {
			LeaderboardDistributor row = new LeaderboardDistributor();
			row.setCompanyId(companyId);
			row.setDistributorId(distributorId);
			row.setDate(dateKey);
			row.setSales(salesDelta);
			row.setNumber(1L);
			leaderboardDistributorMapper.insert(row);
		} else {
			LambdaUpdateWrapper<LeaderboardDistributor> u = new LambdaUpdateWrapper<>();
			u.eq(LeaderboardDistributor::getId, existing.getId())
					.setSql("sales = IFNULL(sales,0) + " + salesDelta)
					.setSql("number = IFNULL(number,0) + 1");
			leaderboardDistributorMapper.update(null, u);
		}
	}

	private void completeUserOrderTaskInTransaction(long companyId, long popularizeSellerId, long userId, long orderId) {
		ShopsRelSalesperson shopRel =
				shopsRelSalespersonMapper.selectOne(
						new LambdaQueryWrapper<ShopsRelSalesperson>()
								.eq(ShopsRelSalesperson::getSalespersonId, popularizeSellerId)
								.eq(ShopsRelSalesperson::getStoreType, "distributor")
								.last("LIMIT 1"));
		if (shopRel == null || shopRel.getShopId() == null) {
			return;
		}
		long distributorId = shopRel.getShopId();
		long now = System.currentTimeMillis() / 1000L;
		SalespersonTask task =
				salespersonTaskMapper.selectActiveTaskForDistributor(
						companyId, distributorId, TASK_TYPE_USER_ORDER, now);
		if (task == null || task.getTaskId() == null) {
			return;
		}
		long taskId = task.getTaskId();
		Map<String, Object> taskParam = new HashMap<>();
		taskParam.put("company_id", companyId);
		taskParam.put("salesperson_id", popularizeSellerId);
		taskParam.put("task_id", taskId);
		taskParam.put("distributor_id", distributorId);
		try {
			upsertTaskRecord(taskParam);
			SalespersonTaskRecordLogs logRow = new SalespersonTaskRecordLogs();
			logRow.setCompanyId(companyId);
			logRow.setSalespersonId(popularizeSellerId);
			logRow.setTaskId(taskId);
			logRow.setDistributorId(distributorId);
			Map<String, Object> remark = new HashMap<>();
			remark.put("user_id", userId);
			remark.put("order_id", orderId);
			try {
				logRow.setRemark(objectMapper.writeValueAsString(remark));
			} catch (JsonProcessingException e) {
				logRow.setRemark("{}");
			}
			int t = (int) Math.min(now, Integer.MAX_VALUE);
			logRow.setCreated(t);
			logRow.setUpdated(t);
			salespersonTaskRecordLogsMapper.insert(logRow);
		} catch (Exception e) {
			throw new ResourceException("任务记录保存失败");
		}
	}

	private void upsertTaskRecord(Map<String, Object> filter) {
		long companyId = ((Number) filter.get("company_id")).longValue();
		long taskId = ((Number) filter.get("task_id")).longValue();
		long distributorId = ((Number) filter.get("distributor_id")).longValue();
		long salespersonId = ((Number) filter.get("salesperson_id")).longValue();
		SalespersonTaskRecord existing =
				salespersonTaskRecordMapper.selectOne(
						new LambdaQueryWrapper<SalespersonTaskRecord>()
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
