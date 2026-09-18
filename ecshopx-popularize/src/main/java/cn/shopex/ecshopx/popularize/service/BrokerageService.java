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

import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 分销佣金领域服务。定时「计划结算关单 / 可提现与积分统计」见 {@link #scheduleSettleRebate}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerageService {

	private static final String TYPE_MONEY = "money";

	private final BrokerageMapper brokerageMapper;
	private final PopularizePromoterCountWriteService popularizePromoterCountWriteService;
	private final PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final PlatformTransactionManager platformTransactionManager;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	public void scheduleSettleRebate() {
		int nowSec = (int) Instant.now().getEpochSecond();
		LambdaQueryWrapper<Brokerage> filter = new LambdaQueryWrapper<Brokerage>()
				.le(Brokerage::getPlanCloseTime, nowSec)
				.eq(Brokerage::getIsClose, false);
		long totalCount = brokerageMapper.selectCount(filter);
		if (totalCount == 0) {
			return;
		}
		int totalPage = (int) Math.ceil(totalCount / 100.0);
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		TransactionTemplate rowTx = new TransactionTemplate(platformTransactionManager, def);
		for (int p = 1; p <= totalPage; p++) {
			LambdaQueryWrapper<Brokerage> w =
					new LambdaQueryWrapper<Brokerage>()
							.le(Brokerage::getPlanCloseTime, nowSec)
							.eq(Brokerage::getIsClose, false)
							.orderByDesc(Brokerage::getCreated)
							.last("LIMIT 100");
			List<Brokerage> list = brokerageMapper.selectList(w);
			for (Brokerage row : list) {
				try {
					rowTx.executeWithoutResult(
							transactionStatus -> settleOneRebateRow(row, nowSec));
				} catch (Throwable t) {
					Throwable ex = t;
					if (t.getCause() != null) {
						ex = t.getCause();
					}
					if (log.isDebugEnabled()) {
						log.debug("定时执行佣金结算失败=>{}", ex.getMessage());
						log.debug("定时执行佣金结算失败, id={} orderId={} userId={} companyId={} type={} rebate={} rebatePoint={}", row.getId(), row.getOrderId(), row.getUserId(), row.getCompanyId(), row.getCommissionType(), row.getRebate(), row.getRebatePoint());
					}
				}
			}
		}
	}

	/**
	 * 单行关单后结算。调用方须将本方法置于行级新事务中执行。
	 */
	void settleOneRebateRow(Brokerage row, int nowSec) {
		if (row.getId() == null) {
			return;
		}
		UpdateWrapper<Brokerage> closeUw = new UpdateWrapper<Brokerage>()
				.eq("id", row.getId())
				.set("is_close", true)
				.set("updated", nowSec);
		brokerageMapper.update(null, closeUw);
		if (row.getUserId() == null || row.getCompanyId() == null) {
			return;
		}
		String commissionType = row.getCommissionType();
		if (TYPE_MONEY.equals(commissionType)) {
			long rebateMinor = row.getRebate() == null ? 0L : row.getRebate();
			popularizePromoterCountWriteService.addSettleRebateForSchedule(
					row.getCompanyId(), row.getUserId(), rebateMinor);
			return;
		}
		if (oemShuyun) {
			return;
		}
		int pointDelta = parseRebatePointSigned(row);
		long pd = pointDelta;
		popularizeBrokerageStatisticsIncrementService.add(
				Map.of("cash_withdrawal_point", pd, "no_close_point", -pd), row.getUserId(), row.getCompanyId());
		if (log.isDebugEnabled()) {
			log.debug("分发积分start:{}", row.getOrderId());
		}
		pointMemberAddPointService.addPointForPopularizeSettle(
				row.getUserId(), row.getCompanyId(), pointDelta, nullSafeString(row.getOrderId()));
	}

	private static String nullSafeString(String s) {
		return s == null ? "" : s;
	}

	private static int parseRebatePointSigned(Brokerage row) {
		String s = row.getRebatePoint();
		if (s == null) {
			return 0;
		}
		s = s.trim();
		if (s.isEmpty() || "money".equalsIgnoreCase(s)) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
