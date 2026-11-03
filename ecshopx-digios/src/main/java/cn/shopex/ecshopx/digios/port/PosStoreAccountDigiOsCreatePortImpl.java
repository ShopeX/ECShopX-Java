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

package cn.shopex.ecshopx.digios.port;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.port.digios.PosStoreAccountDigiOsCreatePort;
import cn.shopex.ecshopx.digios.client.DigiOsAccountResponse;
import cn.shopex.ecshopx.digios.client.DigiOsGatewayClient;
import cn.shopex.ecshopx.digios.client.DigiOsGatewayResult;
import cn.shopex.ecshopx.digios.service.DigiOsAccountMapper;
import cn.shopex.ecshopx.digios.service.DigiOsConfigService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Primary
@Component
public class PosStoreAccountDigiOsCreatePortImpl implements PosStoreAccountDigiOsCreatePort {

	private final DigiOsConfigService configService;
	private final DigiOsGatewayClient gatewayClient;

	public PosStoreAccountDigiOsCreatePortImpl(
			DigiOsConfigService configService, DigiOsGatewayClient gatewayClient) {
		this.configService = configService;
		this.gatewayClient = gatewayClient;
	}

	@Override
	public void ensureSyncedOnCreate(
			long companyId, String employeeNo, String name, String mobile, String email) {
		if (!configService.resolve(companyId).enabled()) {
			return;
		}
		if (!StringUtils.hasText(mobile)) {
			throw new BadRequestException("手机号必填");
		}
		Map<String, Object> pos = new LinkedHashMap<>();
		pos.put("employee_no", employeeNo);
		pos.put("name", name);
		pos.put("mobile", mobile);
		pos.put("email", email == null ? "" : email);
		Map<String, String> payload = DigiOsAccountMapper.mapPos(pos);
		if (payload == null) {
			throw new BadRequestException("手机号必填");
		}
		Map<String, Object> biz = new LinkedHashMap<>(payload);
		DigiOsGatewayResult result = gatewayClient.put(companyId, "account.upsert", biz);
		DigiOsAccountResponse.CreateResult parsed =
				DigiOsAccountResponse.resolveCreateResult(result, payload.get("acc"));
		if (!parsed.ok()) {
			throw new BadRequestException("DigiOS同步失败：" + frontendMessage(parsed.error()));
		}
	}

	static String frontendMessage(String error) {
		if (!StringUtils.hasText(error)) {
			return "未知错误";
		}
		String trimmed = error.trim();
		int idx = trimmed.indexOf(": ");
		if (idx > 0 && idx <= 4 && Character.isDigit(trimmed.charAt(0))) {
			return trimmed.substring(idx + 2);
		}
		return trimmed;
	}
}
