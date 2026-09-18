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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.popularize.domain.PromoterBrokerageStatistics;
import cn.shopex.ecshopx.popularize.mapper.PromoterBrokerageStatisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PopularizeBrokerageStatisticsIncrementService {

	private static final Set<String> ALLOWED_DELTA_COLUMNS = Set.of(
			"freeze_cash_withdrawal_rebate",
			"payed_rebate",
			"cash_withdrawal_rebate",
			"item_total_price",
			"rebate_total",
			"no_close_rebate",
			"recharge_rebate",
			"point_total",
			"no_close_point",
			"cash_withdrawal_point");

	private final PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper;

	public PopularizeBrokerageStatisticsIncrementService(PromoterBrokerageStatisticsMapper promoterBrokerageStatisticsMapper) {
		this.promoterBrokerageStatisticsMapper = promoterBrokerageStatisticsMapper;
	}

	public long loadCashWithdrawalRebate(long userId, long companyId) {
		LambdaQueryWrapper<PromoterBrokerageStatistics> q = new LambdaQueryWrapper<>();
		q.eq(PromoterBrokerageStatistics::getUserId, userId).eq(PromoterBrokerageStatistics::getCompanyId, companyId);
		PromoterBrokerageStatistics row = promoterBrokerageStatisticsMapper.selectOne(q);
		if (row == null || row.getCashWithdrawalRebate() == null) {
			return 0L;
		}
		return row.getCashWithdrawalRebate();
	}

	public void add(Map<String, Long> columnDeltas, long userId, long companyId) {
		if (columnDeltas == null || columnDeltas.isEmpty()) {
			return;
		}
		for (String col : columnDeltas.keySet()) {
			if (!ALLOWED_DELTA_COLUMNS.contains(col)) {
				throw new IllegalArgumentException("非法列名");
			}
		}
		LambdaQueryWrapper<PromoterBrokerageStatistics> q = new LambdaQueryWrapper<>();
		q.eq(PromoterBrokerageStatistics::getUserId, userId).eq(PromoterBrokerageStatistics::getCompanyId, companyId);
		PromoterBrokerageStatistics existing = promoterBrokerageStatisticsMapper.selectOne(q);
		if (existing == null) {
			PromoterBrokerageStatistics n = new PromoterBrokerageStatistics();
			n.setUserId(userId);
			n.setCompanyId(companyId);
			applyDeltasToRow(n, columnDeltas);
			promoterBrokerageStatisticsMapper.insert(n);
			return;
		}
		LambdaUpdateWrapper<PromoterBrokerageStatistics> uw = new LambdaUpdateWrapper<>();
		uw.eq(PromoterBrokerageStatistics::getUserId, userId).eq(PromoterBrokerageStatistics::getCompanyId, companyId);
		for (Map.Entry<String, Long> e : columnDeltas.entrySet()) {
			Long delta = e.getValue();
			if (delta == null || delta == 0L) {
				continue;
			}
			String col = e.getKey();
			uw.setSql(col + " = " + col + " + (" + delta + ")");
		}
		promoterBrokerageStatisticsMapper.update(null, uw);
	}

	private static void applyDeltasToRow(PromoterBrokerageStatistics n, Map<String, Long> columnDeltas) {
		for (Map.Entry<String, Long> e : columnDeltas.entrySet()) {
			long v = e.getValue() == null ? 0L : e.getValue();
			switch (e.getKey()) {
				case "freeze_cash_withdrawal_rebate" -> n.setFreezeCashWithdrawalRebate(v);
				case "payed_rebate" -> n.setPayedRebate(v);
				case "cash_withdrawal_rebate" -> n.setCashWithdrawalRebate(v);
				case "item_total_price" -> n.setItemTotalPrice(v);
				case "rebate_total" -> n.setRebateTotal(v);
				case "no_close_rebate" -> n.setNoCloseRebate(v);
				case "recharge_rebate" -> n.setRechargeRebate(v);
				case "point_total" -> n.setPointTotal(v);
				case "no_close_point" -> n.setNoClosePoint(v);
				case "cash_withdrawal_point" -> n.setCashWithdrawalPoint(v);
				default -> throw new IllegalStateException();
			}
		}
	}
}
