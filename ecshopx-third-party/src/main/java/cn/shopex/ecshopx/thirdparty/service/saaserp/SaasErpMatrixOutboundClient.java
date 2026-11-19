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

package cn.shopex.ecshopx.thirdparty.service.saaserp;

import cn.shopex.ecshopx.common.systemlink.OmsQueueLogWritePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Outbound Matrix client matching PHP {@code ThirdPartyBundle\Services\SaasErpCentre\Request}.
 */
@Service
public class SaasErpMatrixOutboundClient {

	private static final Logger log = LoggerFactory.getLogger(SaasErpMatrixOutboundClient.class);

	private static final DateTimeFormatter MATRIX_TS =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static final Set<String> CHECK_METHODS =
			Set.of(
					"store.trade.add",
					"store.trade.update",
					"store.trade.refund.add",
					"store.trade.aftersale.add",
					"store.trade.aftersale.status.update",
					"store.trade.aftersale.logistics.update");

	private static final Map<Integer, String> MENU_TYPE_TO_MODEL =
			Map.of(1, "all", 2, "b2c", 3, "platform", 4, "standard", 5, "in_purchase");

	private final SaasErpMatrixCertCredentialsReadService credentialsReadService;
	private final OmsQueueLogWritePort omsQueueLogWritePort;
	private final ObjectMapper objectMapper;
	private final JdbcTemplate jdbcTemplate;
	private final RestTemplate restTemplate;
	private final String matrixApiUrl;
	private final String verifyAppId;
	private final String defaultProductModel;

	public SaasErpMatrixOutboundClient(
			SaasErpMatrixCertCredentialsReadService credentialsReadService,
			OmsQueueLogWritePort omsQueueLogWritePort,
			ObjectMapper objectMapper,
			JdbcTemplate jdbcTemplate,
			@Value("${common.matrix-api-url:http://matrix.ecos.shopex.cn/sync}") String matrixApiUrl,
			@Value("${common.verify-app-id:ecos.ecshopx}") String verifyAppId,
			@Value("${common.product-model:platform}") String defaultProductModel) {
		this.credentialsReadService = credentialsReadService;
		this.omsQueueLogWritePort = omsQueueLogWritePort;
		this.objectMapper = objectMapper;
		this.jdbcTemplate = jdbcTemplate;
		this.matrixApiUrl = matrixApiUrl == null ? "" : matrixApiUrl.trim();
		this.verifyAppId = verifyAppId == null ? "ecos.ecshopx" : verifyAppId.trim();
		this.defaultProductModel =
				StringUtils.hasText(defaultProductModel) ? defaultProductModel.trim() : "platform";
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
		factory.setReadTimeout((int) Duration.ofSeconds(3).toMillis());
		this.restTemplate = new RestTemplate(factory);
	}

	public Map<String, Object> call(long companyId, String method, Map<String, Object> data) {
		if (companyId <= 0 || !StringUtils.hasText(method) || data == null || data.isEmpty()) {
			return Map.of();
		}
		Optional<SaasErpMatrixCertCredentials> credOpt = credentialsReadService.findCredentials(companyId);
		if (credOpt.isEmpty()) {
			log.debug("saaserp outbound skipped: missing ERP bind/cert companyId={} method={}", companyId, method);
			return Map.of();
		}
		if (shouldSkipNonSelfOperated(companyId, method, data)) {
			Object tid = data.get("tid");
			log.debug(
					"saaserp::check::{} 订单号 ===> {}不是自营店铺订单  不同步",
					method,
					tid == null ? "" : tid);
			return Map.of();
		}
		if (!StringUtils.hasText(matrixApiUrl)) {
			log.debug("saaserp outbound skipped: matrix-api-url empty companyId={} method={}", companyId, method);
			return Map.of();
		}

		SaasErpMatrixCertCredentials cred = credOpt.get();
		String now = MATRIX_TS.format(Instant.now());
		LinkedHashMap<String, Object> systemParams = new LinkedHashMap<>();
		systemParams.put("method", method);
		systemParams.put("certi_id", cred.certId());
		systemParams.put("from_node_id", cred.nodeId());
		systemParams.put("from_api_v", "1.0");
		systemParams.put("to_node_id", cred.erpBindNodeId());
		systemParams.put("timestamp", now);
		systemParams.put("format", "json");
		systemParams.put("date", now);
		systemParams.put("refresh_time", now);
		systemParams.put("v", "1.0");
		systemParams.put("app_id", verifyAppId);
		systemParams.put("node_type", "ecos.ome");
		systemParams.put("task", createTaskId());
		systemParams.put("_id", "rel_" + cred.nodeId() + "_" + method + "_" + cred.erpBindNodeId());

		LinkedHashMap<String, Object> queryParams = new LinkedHashMap<>();
		queryParams.putAll(data);
		queryParams.putAll(systemParams);
		queryParams.put("sign", SaasErpMatrixSignSupport.matrixSign(queryParams, cred.token(), objectMapper));

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (Map.Entry<String, Object> e : queryParams.entrySet()) {
			Object v = e.getValue();
			if (v == null) {
				continue;
			}
			form.add(e.getKey(), stringifyFormValue(v));
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		long t1 = System.nanoTime();
		Map<String, Object> result = Map.of();
		try {
			String body =
					restTemplate.postForObject(matrixApiUrl, new HttpEntity<>(form, headers), String.class);
			long runtimeMs = (System.nanoTime() - t1) / 1_000_000L;
			result = parseResult(body);
			saveRequestLog(companyId, method, queryParams, result, runtimeMs / 1000.0);
			log.debug("saaserp::url::{}", matrixApiUrl);
			log.debug("saaserp::params::{}::{}", method, writeJson(queryParams));
			log.debug("saaserp::result::{}::{}", method, writeJson(result));
			return result;
		} catch (RuntimeException e) {
			log.debug(
					"saaserp::error:: companyId={} method={} msg={}",
					companyId,
					method,
					e.getMessage());
			return Map.of();
		}
	}

	/**
	 * PHP {@code Request::_check}: for {@code product_model=platform}, skip sync when order has a
	 * non-empty {@code distributor_id}. Non-platform models always sync.
	 */
	boolean shouldSkipNonSelfOperated(long companyId, String method, Map<String, Object> data) {
		if (!CHECK_METHODS.contains(method)) {
			return false;
		}
		String productModel = resolveProductModel(companyId);
		if (!"platform".equals(productModel)) {
			return false;
		}
		Object tid = data.get("tid");
		if (tid == null || !StringUtils.hasText(String.valueOf(tid))) {
			return false;
		}
		Long distributorId = lookupDistributorId(String.valueOf(tid).trim());
		return distributorId != null && distributorId > 0L;
	}

	private String resolveProductModel(long companyId) {
		try {
			Integer menuType =
					jdbcTemplate.query(
							"SELECT menu_type FROM companys WHERE company_id = ? LIMIT 1",
							rs -> rs.next() ? rs.getObject(1, Integer.class) : null,
							companyId);
			if (menuType == null || menuType == 0) {
				return defaultProductModel;
			}
			return MENU_TYPE_TO_MODEL.getOrDefault(menuType, defaultProductModel);
		} catch (RuntimeException e) {
			log.debug("saaserp product_model lookup failed companyId={} msg={}", companyId, e.getMessage());
			return defaultProductModel;
		}
	}

	private Long lookupDistributorId(String orderId) {
		try {
			return jdbcTemplate.query(
					"SELECT distributor_id FROM orders_normal_orders WHERE order_id = ? LIMIT 1",
					rs -> rs.next() ? rs.getObject(1, Long.class) : null,
					orderId);
		} catch (RuntimeException e) {
			log.debug("saaserp distributor lookup failed orderId={} msg={}", orderId, e.getMessage());
			return null;
		}
	}

	private Map<String, Object> parseResult(String body) {
		if (!StringUtils.hasText(body)) {
			return Map.of();
		}
		try {
			Map<String, Object> result =
					objectMapper.readValue(body, new TypeReference<LinkedHashMap<String, Object>>() {});
			Object data = result.get("data");
			if (data instanceof String s && StringUtils.hasText(s)) {
				try {
					result.put("data", objectMapper.readValue(s, Object.class));
				} catch (JsonProcessingException ignored) {
					// keep string
				}
			}
			return result;
		} catch (JsonProcessingException e) {
			return Map.of();
		}
	}

	private void saveRequestLog(
			long companyId,
			String method,
			Map<String, Object> queryParams,
			Map<String, Object> result,
			double runtimeSec) {
		String status = "fail";
		Object data = result.get("data");
		if (data instanceof Map<?, ?> m) {
			Object rsp = m.get("rsp");
			if ("succ".equals(String.valueOf(rsp))) {
				status = "success";
			}
		}
		Object msgIdObj = result.get("msg_id");
		String msgId = msgIdObj == null ? null : String.valueOf(msgIdObj);
		try {
			omsQueueLogWritePort.insertRequest(
					companyId,
					method,
					writeJson(queryParams),
					writeJson(result),
					status,
					String.valueOf(Math.round(runtimeSec * 1000.0) / 1000.0),
					msgId);
		} catch (RuntimeException e) {
			log.debug("saaserp saveRequestLog failed companyId={} method={} msg={}", companyId, method, e.getMessage());
		}
	}

	private String stringifyFormValue(Object v) {
		if (v instanceof Map<?, ?> || v instanceof List<?> || v instanceof Iterable<?>) {
			return writeJson(v);
		}
		return String.valueOf(v);
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			return String.valueOf(value);
		}
	}

	static String createTaskId() {
		int i = ThreadLocalRandom.current().nextInt(0, 10000);
		return System.currentTimeMillis() / 1000L + String.format("%04d", i);
	}
}
