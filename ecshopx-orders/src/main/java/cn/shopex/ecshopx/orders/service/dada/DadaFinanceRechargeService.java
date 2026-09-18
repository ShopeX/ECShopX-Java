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

package cn.shopex.ecshopx.orders.service.dada;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.localdelivery.LocalDeliveryDadaRechargePort;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DadaFinanceRechargeService {

	private final LocalDeliveryDadaRechargePort localDeliveryDadaRechargePort;
	private final String localDeliveryDriver;

	public DadaFinanceRechargeService(
			LocalDeliveryDadaRechargePort localDeliveryDadaRechargePort,
			@Value("${common.local-delivery-dirver:dada}") String localDeliveryDriver) {
		this.localDeliveryDadaRechargePort = localDeliveryDadaRechargePort;
		this.localDeliveryDriver = localDeliveryDriver;
	}

	public String recharge(long companyId, String amountForNotifyAndBody, String notifyUrl) {
		String driver = localDeliveryDriver == null ? "dada" : localDeliveryDriver.trim().toLowerCase(Locale.ROOT);
		if (!"dada".equals(driver) && !"shansong".equals(driver)) {
			throw new ResourceException("同城配仅支持达达和闪送");
		}
		if ("shansong".equals(driver)) {
			throw new ResourceException("请前往闪送商户后台进行充值");
		}
		return localDeliveryDadaRechargePort.recharge(companyId, amountForNotifyAndBody, notifyUrl);
	}
}
