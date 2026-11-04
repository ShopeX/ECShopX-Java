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

package cn.shopex.ecshopx.deposit.integration.alipay;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import cn.shopex.ecshopx.deposit.service.AdapayDepositTradeRechargeCallbackService;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyDepositSidePort;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlipayNotifyDepositSidePortImpl implements AlipayNotifyDepositSidePort {

	private final DepositTradeMapper depositTradeMapper;
	private final AdapayDepositTradeRechargeCallbackService adapayDepositTradeRechargeCallbackService;

	public AlipayNotifyDepositSidePortImpl(
			DepositTradeMapper depositTradeMapper,
			AdapayDepositTradeRechargeCallbackService adapayDepositTradeRechargeCallbackService) {
		this.depositTradeMapper = depositTradeMapper;
		this.adapayDepositTradeRechargeCallbackService = adapayDepositTradeRechargeCallbackService;
	}

	@Override
	public long requireCompanyIdByDepositTradeId(String depositTradeId) {
		if (!StringUtils.hasText(depositTradeId)) {
			throw new BadRequestException("储值单不存在或主键无效");
		}
		DepositTrade row = depositTradeMapper.selectById(depositTradeId.trim());
		if (row == null) {
			throw new BadRequestException("储值单不存在或主键无效");
		}
		String raw = row.getCompanyId();
		if (!StringUtils.hasText(raw)) {
			throw new BadRequestException("储值单企业编号无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("储值单企业编号无效");
		}
		if (companyId <= 0) {
			throw new BadRequestException("储值单企业编号无效");
		}
		return companyId;
	}

	@Override
	public void rechargeCallback(String depositTradeId, String status, Map<String, Object> options) {
		adapayDepositTradeRechargeCallbackService.rechargeCallback(depositTradeId, status, options);
	}
}
