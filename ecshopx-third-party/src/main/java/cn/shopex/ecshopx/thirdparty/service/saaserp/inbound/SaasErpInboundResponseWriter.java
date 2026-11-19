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

package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.systemlink.OmsQueueLogWritePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SaasErpInboundResponseWriter {

	private static final Logger log = LoggerFactory.getLogger(SaasErpInboundResponseWriter.class);

	private static final String CONTENT_TYPE = "text/html;charset=utf-8";
	private static final String CACHE_CONTROL = "no-cache,no-store , must-revalidate";

	private final OmsQueueLogWritePort omsQueueLogWritePort;
	private final ObjectMapper objectMapper;

	public SaasErpInboundResponseWriter(OmsQueueLogWritePort omsQueueLogWritePort, ObjectMapper objectMapper) {
		this.omsQueueLogWritePort = omsQueueLogWritePort;
		this.objectMapper = objectMapper;
	}

	public void write(HttpServletResponse response, SaasErpInboundResult result, long logId) throws IOException {
		String mappedResult = mapResultStatus(result.status());
		long shopexTime = Instant.now().getEpochSecond();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("result", mappedResult);
		body.put("code", 0);
		body.put("shopex_time", shopexTime);
		body.put("msg", result.msg());
		body.put("info", result.info());

		String bodyJson;
		try {
			bodyJson = objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException e) {
			throw new IOException("Failed to serialize SaaS ERP inbound response", e);
		}

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(CONTENT_TYPE);
		response.setHeader("Cache-Control", CACHE_CONTROL);
		String expires = DateTimeFormatter.RFC_1123_DATE_TIME
				.withZone(ZoneOffset.UTC)
				.format(Instant.now().plusSeconds(20));
		response.setHeader("Expires", expires);
		response.getWriter().write(bodyJson);

		if (logId > 0) {
			try {
				omsQueueLogWritePort.updateResult(logId, mappedResult, bodyJson);
			} catch (RuntimeException e) {
				log.error("Failed to update OMS queue log result for logId={}", logId, e);
			}
		}
	}

	private static String mapResultStatus(String status) {
		if ("true".equals(status)) {
			return "success";
		}
		return status;
	}
}
