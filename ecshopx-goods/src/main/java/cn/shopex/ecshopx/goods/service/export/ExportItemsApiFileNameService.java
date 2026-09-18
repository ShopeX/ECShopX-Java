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

package cn.shopex.ecshopx.goods.service.export;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsApiFileNameResult;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsCodeFilterBuildResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExportItemsApiFileNameService {

	private final ExportItemsCodeFilterBuildService exportItemsCodeFilterBuildService;
	private final ObjectMapper exportKeyFilterObjectMapper;
	private final StringRedisTemplate sharedStringRedisTemplate;

	public ExportItemsApiFileNameService(ExportItemsCodeFilterBuildService exportItemsCodeFilterBuildService,
			@Qualifier("exportKeyFilterObjectMapper") ObjectMapper exportKeyFilterObjectMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.exportItemsCodeFilterBuildService = exportItemsCodeFilterBuildService;
		this.exportKeyFilterObjectMapper = exportKeyFilterObjectMapper;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	public ExportItemsApiFileNameResult writeKeyOrEmptyList(long companyId, long operatorId, String operatorType,
			Long merchantId, Map<String, Object> inputData) {
		ExportItemsCodeFilterBuildResult built = exportItemsCodeFilterBuildService.build(companyId, operatorId,
				operatorType, merchantId, inputData);
		if (built instanceof ExportItemsCodeFilterBuildResult.EmptyItemBnList) {
			return new ExportItemsApiFileNameResult.EmptyItemBnList();
		}
		ExportItemsCodeFilterBuildResult.ParamsReady pr = (ExportItemsCodeFilterBuildResult.ParamsReady) built;
		Map<String, Object> filter = new LinkedHashMap<>(pr.params());
		filter.remove("export_type");
		String json;
		try {
			json = exportKeyFilterObjectMapper.writeValueAsString(filter);
		} catch (JsonProcessingException e) {
			throw new ResourceException("导出参数序列化失败");
		}
		String urlSuffix = String.valueOf(Instant.now().getEpochSecond())
				+ ThreadLocalRandom.current().nextInt(111, 1000);
		sharedStringRedisTemplate.opsForValue().set("export_key:" + urlSuffix, json, Duration.ofSeconds(86400));
		return new ExportItemsApiFileNameResult.UrlReady(urlSuffix);
	}
}
