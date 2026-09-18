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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.wechat.port.WxappShareByShareIdSalespersonPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WxappGetByShareIdService {

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final WxappShareByShareIdSalespersonPort wxappShareByShareIdSalespersonPort;

	public WxappGetByShareIdService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper,
			WxappShareByShareIdSalespersonPort wxappShareByShareIdSalespersonPort) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
		this.wxappShareByShareIdSalespersonPort = wxappShareByShareIdSalespersonPort;
	}

	public Object getByShareId(long companyId, String shareId) {
		String key = String.valueOf(companyId) + "_" + shareId;
		String raw = sharedStringRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isEmpty()) {
			return new ArrayList<Object>();
		}
		Object decoded;
		try {
			decoded = objectMapper.readValue(raw, Object.class);
		} catch (Exception e) {
			return null;
		}
		if (!(decoded instanceof Map<?, ?> rawMap)) {
			return decoded;
		}
		LinkedHashMap<String, Object> salesperson = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : rawMap.entrySet()) {
			salesperson.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (salesperson.isEmpty()) {
			return new ArrayList<Object>();
		}
		boolean hasGu = salesperson.containsKey("gu") && salesperson.get("gu") != null;
		if (!hasGu) {
			return salesperson;
		}
		String guRaw = String.valueOf(salesperson.get("gu")).trim();
		String workUserid = guRaw.split("_", -1)[0];
		Map<String, Object> salespersonInfo =
				wxappShareByShareIdSalespersonPort.getSalespersonAndDistributorByWorkUserid(companyId, workUserid);
		if (salespersonInfo == null || salespersonInfo.isEmpty()) {
			return salesperson;
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(salesperson);
		Object dInfoObj = salespersonInfo.get("distributorInfo");
		if (dInfoObj != null && dInfoObj instanceof Map<?, ?> dMap && !dMap.isEmpty()) {
			@SuppressWarnings("unchecked")
			Map<String, Object> distributorMap = (Map<String, Object>) dInfoObj;
			out.put("distributorInfo", distributorMap);
		}
		if (salespersonInfo.containsKey("distributor_id")) {
			out.put("distributor_id", salespersonInfo.get("distributor_id"));
		}
		if (salespersonInfo.containsKey("dtid")) {
			out.put("dtid", salespersonInfo.get("dtid"));
		}
		if (salespersonInfo.containsKey("name")) {
			out.put("name", salespersonInfo.get("name"));
		}
		out.put("salespersonInfo", salespersonInfo);
		return out;
	}
}
