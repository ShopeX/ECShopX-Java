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

package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import cn.shopex.ecshopx.thirdparty.domain.DmCrmLog;
import cn.shopex.ecshopx.thirdparty.mapper.DmCrmLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class DmCrmApiLogService {

	private final DmCrmLogMapper dmCrmLogMapper;
	private final ObjectMapper objectMapper;

	public DmCrmApiLogService(DmCrmLogMapper dmCrmLogMapper, ObjectMapper objectMapper) {
		this.dmCrmLogMapper = dmCrmLogMapper;
		this.objectMapper = objectMapper;
	}

	public void recordApiCall(
			long companyId,
			String worker,
			Object requestParams,
			Object responseBody,
			String apiType,
			String status,
			int runtimeSeconds) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		DmCrmLog log = new DmCrmLog();
		log.setCompanyId(companyId);
		log.setWorker(worker);
		log.setParams(toJsonString(requestParams));
		log.setResult(toJsonString(responseBody));
		log.setApiType(apiType);
		log.setStatus(status);
		log.setRuntime(String.valueOf(runtimeSeconds));
		log.setCreated(now);
		log.setUpdated(now);
		dmCrmLogMapper.insert(log);
	}

	private String toJsonString(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof String s) {
			return s;
		}
		try {
			return objectMapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			return String.valueOf(o);
		}
	}
}
