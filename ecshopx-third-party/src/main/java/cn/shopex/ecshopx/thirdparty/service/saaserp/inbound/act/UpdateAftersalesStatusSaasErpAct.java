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

package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpActHandler;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpAftersaleStatusPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(SaasErpAftersaleStatusPort.class)
public class UpdateAftersalesStatusSaasErpAct implements SaasErpActHandler {

	private static final String MISSING_AFTERSALE = "未获取到售后单信息";

	private final SaasErpAftersaleStatusPort aftersaleStatusPort;

	public UpdateAftersalesStatusSaasErpAct(SaasErpAftersaleStatusPort aftersaleStatusPort) {
		this.aftersaleStatusPort = aftersaleStatusPort;
	}

	@Override
	public String act() {
		return "get_return_status";
	}

	@Override
	public SaasErpInboundResult handle(Map<String, String> params, long companyId) {
		String aftersaleId = params.get("aftersale_id");
		if (aftersaleId == null || aftersaleId.isBlank()) {
			return SaasErpInboundResult.fail("售后单号缺少！");
		}

		String orderId = params.get("order_id");
		if (orderId == null || orderId.isBlank()) {
			return SaasErpInboundResult.fail("订单号缺少！");
		}

		String status = params.get("status");
		if (status == null || status.isBlank()) {
			return SaasErpInboundResult.fail("售后状态缺少！");
		}

		try {
			List<Object> info = aftersaleStatusPort.update(params);
			return SaasErpInboundResult.ok("操作成功", info);
		} catch (IllegalStateException e) {
			if (MISSING_AFTERSALE.equals(e.getMessage())) {
				return SaasErpInboundResult.fail(MISSING_AFTERSALE);
			}
			return SaasErpInboundResult.ok("操作成功", Collections.emptyList());
		} catch (RuntimeException e) {
			return SaasErpInboundResult.ok("操作成功", Collections.emptyList());
		}
	}
}
