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

import cn.shopex.ecshopx.common.port.popularize.OrderValidityLatestAftersaleDaysReadPort;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 按订单更新分销佣金计划结算时间（未结算行）。与 PHP 一致：
 * {@code plan_close_time = 订单完成时刻 + 86400 * (limit_time + latest_aftersale_time)}，
 * 其中 {@code limit_time} 来自推广配置，{@code latest_aftersale_time} 来自订单时效设置。
 */
@Slf4j
@Service
public class BrokeragePlanCloseTimeService {

	/** 支付生成佣金行时的占位，避免在订单完成前被 settle-brokerage 关单。 */
	public static final int PLAN_CLOSE_TIME_PENDING = Integer.MAX_VALUE;

	private final BrokerageMapper brokerageMapper;
	private final PopularizeSettingSaveService popularizeSettingSaveService;
	private final OrderValidityLatestAftersaleDaysReadPort orderValidityLatestAftersaleDaysReadPort;

	public BrokeragePlanCloseTimeService(
			BrokerageMapper brokerageMapper,
			PopularizeSettingSaveService popularizeSettingSaveService,
			OrderValidityLatestAftersaleDaysReadPort orderValidityLatestAftersaleDaysReadPort) {
		this.brokerageMapper = brokerageMapper;
		this.popularizeSettingSaveService = popularizeSettingSaveService;
		this.orderValidityLatestAftersaleDaysReadPort = orderValidityLatestAftersaleDaysReadPort;
	}

	/** 支付写佣 insert 用，待确认收货后再 {@link #updatePlanCloseTime}。 */
	public int pendingPlanCloseTimeForInsert() {
		return PLAN_CLOSE_TIME_PENDING;
	}

	/**
	 * 与 PHP / {@link #updatePlanCloseTime} 相同口径：推广 {@code limit_time} + 平台 {@code latest_aftersale_time}（天）。
	 */
	public int computePlanCloseTimeEpochSec(long companyId) {
		if (companyId <= 0L) {
			return pendingPlanCloseTimeForInsert();
		}
		Map<String, Object> popularizeCfg = popularizeSettingSaveService.getMergedPopularizeConfig(companyId);
		int limitTimeDays = intVal(popularizeCfg.get("limit_time"));
		int latestAftersaleDays =
				orderValidityLatestAftersaleDaysReadPort.readLatestAftersaleTimeDays(companyId);
		int totalDays = limitTimeDays + latestAftersaleDays;
		int nowSec = (int) Instant.now().getEpochSecond();
		return (int) (nowSec + 86400L * totalDays);
	}

	public void updatePlanCloseTime(long companyId, long orderId) {
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		int nowSec = (int) Instant.now().getEpochSecond();
		int planCloseTime = computePlanCloseTimeEpochSec(companyId);
		String orderIdStr = String.valueOf(orderId);

		LambdaUpdateWrapper<Brokerage> uw = new LambdaUpdateWrapper<>();
		uw.eq(Brokerage::getCompanyId, companyId)
				.eq(Brokerage::getOrderId, orderIdStr)
				.eq(Brokerage::getIsClose, false)
				.set(Brokerage::getPlanCloseTime, planCloseTime)
				.set(Brokerage::getUpdated, nowSec);
		brokerageMapper.update(null, uw);

		runTaskBrokerageFollowup(companyId, orderId);
	}

	private void runTaskBrokerageFollowup(long companyId, long orderId) {
		if (log.isDebugEnabled()) {
			log.debug("task brokerage follow-up not implemented; companyId={} orderId={}", companyId, orderId);
		}
	}

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			String s = v.toString().trim();
			if (s.isEmpty()) {
				return 0;
			}
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
