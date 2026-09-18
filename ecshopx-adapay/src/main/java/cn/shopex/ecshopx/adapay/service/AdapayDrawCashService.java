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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayMerchantEntry;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantEntryMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.common.port.adapay.AdapayAutoCashConfigReadWritePort;
import cn.shopex.ecshopx.common.port.adapay.AdapayDrawCashQueueEnqueuePort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayDrawCashService {

	private static final long SECONDS_10_DAYS = 10L * 24 * 3600;

	private final AdapayMerchantEntryMapper merchantEntryMapper;
	private final AdapaySettleAccountMapper settleAccountMapper;
	private final AdapayAutoCashConfigReadWritePort autoCashPort;
	private final AdapayDrawCashQueueEnqueuePort enqueuePort;
	private final AdapaySubMerchantDrawCashConfigService subMerchantDrawCashConfigService;
	private final Clock clock;

	public AdapayDrawCashService(
			AdapayMerchantEntryMapper merchantEntryMapper,
			AdapaySettleAccountMapper settleAccountMapper,
			AdapayAutoCashConfigReadWritePort autoCashPort,
			AdapayDrawCashQueueEnqueuePort enqueuePort,
			AdapaySubMerchantDrawCashConfigService subMerchantDrawCashConfigService,
			@Autowired(required = false) Clock clock) {
		this.merchantEntryMapper = merchantEntryMapper;
		this.settleAccountMapper = settleAccountMapper;
		this.autoCashPort = autoCashPort;
		this.enqueuePort = enqueuePort;
		this.subMerchantDrawCashConfigService = subMerchantDrawCashConfigService;
		this.clock = clock == null ? Clock.systemDefaultZone() : clock;
	}

	public int scheduleDrawCashQueue() {
		List<AdapayMerchantEntry> merchantList = merchantEntryMapper.selectList(new LambdaQueryWrapper<>());
		if (merchantList == null || merchantList.isEmpty()) {
			return 0;
		}
		long nowSec = clock.instant().getEpochSecond();
		long defaultNextSec = nowSec + SECONDS_10_DAYS;
		int dispatched = 0;
		for (AdapayMerchantEntry e : merchantList) {
			if (e.getCompanyId() == null) {
				continue;
			}
			long companyId = e.getCompanyId();
			Map<String, Object> autoConfig = new LinkedHashMap<>(autoCashPort.getAutoCashConfig(companyId));
			Object autoDrawObj = autoConfig.get("auto_draw_cash");
			String autoDraw = autoDrawObj == null ? "N" : String.valueOf(autoDrawObj).trim();
			if (!"Y".equals(autoDraw)) {
				continue;
			}
			long nextTimeSec = resolveNextTimeEpochOrDefault(autoConfig, defaultNextSec);
			if (nextTimeSec > nowSec) {
				continue;
			}
			if (!subMerchantDrawCashConfigService.tryAdvanceNextAutoDrawTime(autoConfig, clock)) {
				continue;
			}
			autoCashPort.putAutoCashConfig(companyId, autoConfig);
			enqueuePort.enqueueMainMerchant(companyId);
			dispatched++;
			List<AdapaySettleAccount> accounts =
					settleAccountMapper.selectList(
							new LambdaQueryWrapper<AdapaySettleAccount>()
									.eq(AdapaySettleAccount::getCompanyId, companyId));
			if (accounts == null || accounts.isEmpty()) {
				continue;
			}
			for (AdapaySettleAccount a : accounts) {
				long memberId = a.getMemberId() == null ? 0L : a.getMemberId();
				String settleId = a.getSettleAccountId() == null ? "" : a.getSettleAccountId();
				enqueuePort.enqueueSettleAccount(companyId, memberId, settleId);
				dispatched++;
			}
		}
		return dispatched;
	}

	private static long resolveNextTimeEpochOrDefault(Map<String, Object> config, long defaultEpochSec) {
		Object v = config.get("next_time");
		if (v == null) {
			return defaultEpochSec;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return defaultEpochSec;
			}
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return defaultEpochSec;
			}
		}
		return defaultEpochSec;
	}
}
