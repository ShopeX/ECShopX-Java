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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 按订单更新分销佣金计划结算时间（未结算行）。
 */
@Slf4j
@Service
public class BrokeragePlanCloseTimeService {

	private final BrokerageMapper brokerageMapper;

	// TODO: 用商户侧「分销确认天数 / 售后时效天数」替换下列占位配置（计划结算时刻 = 当前纪元秒 + 86400 * (settleDays + aftersaleDays)）。
	@Value("${ecshopx.popularize.brokerage-plan-close.settle-days:0}")
	private int settleDays;

	@Value("${ecshopx.popularize.brokerage-plan-close.aftersale-days:0}")
	private int aftersaleDays;

	public BrokeragePlanCloseTimeService(BrokerageMapper brokerageMapper) {
		this.brokerageMapper = brokerageMapper;
	}

	public void updatePlanCloseTime(long companyId, long orderId) {
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		int nowSec = (int) Instant.now().getEpochSecond();
		int planCloseTime = (int) (nowSec + 86400L * ((long) settleDays + (long) aftersaleDays));
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
}
