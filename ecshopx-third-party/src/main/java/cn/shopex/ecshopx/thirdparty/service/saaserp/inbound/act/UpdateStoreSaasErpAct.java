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
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpItemStoreSyncPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(SaasErpItemStoreSyncPort.class)
public class UpdateStoreSaasErpAct implements SaasErpActHandler {

	private final SaasErpItemStoreSyncPort itemStoreSyncPort;
	private final ObjectMapper objectMapper;

	public UpdateStoreSaasErpAct(SaasErpItemStoreSyncPort itemStoreSyncPort, ObjectMapper objectMapper) {
		this.itemStoreSyncPort = itemStoreSyncPort;
		this.objectMapper = objectMapper;
	}

	@Override
	public String act() {
		return "update_store";
	}

	@Override
	public SaasErpInboundResult handle(Map<String, String> params, long companyId) {
		String datasRaw = params.get("datas");
		if (datasRaw == null || datasRaw.isBlank()) {
			return SaasErpInboundResult.fail("缺少数据");
		}

		List<Map<String, Object>> rows = parseDatasRows(datasRaw);
		if (rows == null || rows.isEmpty()) {
			return SaasErpInboundResult.fail("数据有误");
		}

		try {
			itemStoreSyncPort.sync(companyId, rows);
		} catch (IllegalStateException e) {
			if ("商品不存在".equals(e.getMessage())) {
				return SaasErpInboundResult.fail("商品不存在");
			}
			throw e;
		}
		return SaasErpInboundResult.ok("操作成功", null);
	}

	private List<Map<String, Object>> parseDatasRows(String datasRaw) {
		List<Map<String, Object>> rows = tryParseDatasRows(datasRaw);
		if (rows != null) {
			return rows;
		}
		return tryParseDatasRows(stripslashes(datasRaw));
	}

	private List<Map<String, Object>> tryParseDatasRows(String raw) {
		try {
			return objectMapper.readValue(raw, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	static String stripslashes(String value) {
		StringBuilder out = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '\\' && i + 1 < value.length()) {
				char next = value.charAt(i + 1);
				if (next == '"' || next == '\'' || next == '\\') {
					out.append(next);
					i++;
					continue;
				}
				if (next == '0') {
					out.append('\0');
					i++;
					continue;
				}
			}
			out.append(c);
		}
		return out.toString();
	}
}
