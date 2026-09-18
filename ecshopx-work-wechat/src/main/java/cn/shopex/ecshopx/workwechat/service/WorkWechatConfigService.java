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

package cn.shopex.ecshopx.workwechat.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.workwechat.config.WorkWechatModuleProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WorkWechatConfigService {

	private static final String ALPHANUM =
			"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final WorkWechatVerifyDomainFileQueryService verifyDomainFileQueryService;
	private final WorkWechatModuleProperties moduleProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	public WorkWechatConfigService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper,
			WorkWechatVerifyDomainFileQueryService verifyDomainFileQueryService,
			WorkWechatModuleProperties moduleProperties) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
		this.verifyDomainFileQueryService = verifyDomainFileQueryService;
		this.moduleProperties = moduleProperties;
	}

	/**
	 * Reads work-wechat config from Redis for admin display; returns default empty structure when missing,
	 * without throwing when not configured.
	 */
	public Map<String, Object> getViewConfig(long companyId) {
		String companyKey = "workwechat:config:" + sha1Hex(String.valueOf(companyId));
		String raw = sharedStringRedisTemplate.opsForValue().get(companyKey);
		Map<String, Object> data;
		if (!StringUtils.hasText(raw)) {
			data = defaultEmptyViewConfig();
		} else {
			try {
				Map<String, Object> parsed =
						objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				if (parsed == null) {
					data = new LinkedHashMap<>();
				} else {
					data = parsed;
					ensureDianwuPlaceholderIfNeeded(data);
				}
			} catch (JsonProcessingException e) {
				log.warn("work wechat getViewConfig json parse failed companyId={}", companyId, e);
				data = new LinkedHashMap<>();
			}
		}
		ensureAvatarUrlDefaults(data);
		return attachConfig(data, companyId);
	}

	private Map<String, Object> defaultEmptyViewConfig() {
		Map<String, Object> app = new LinkedHashMap<>();
		app.put("appid", "");
		app.put("agent_id", "");
		app.put("secret", "");
		app.put("token", "");
		app.put("aes_key", "");

		Map<String, Object> customer = new LinkedHashMap<>();
		customer.put("secret", "");
		customer.put("token", "");
		customer.put("aes_key", "");

		Map<String, Object> report = new LinkedHashMap<>();
		report.put("secret", "");
		report.put("token", "");
		report.put("aes_key", "");

		Map<String, Object> dianwu = new LinkedHashMap<>();
		dianwu.put("agent_id", "");
		dianwu.put("secret", "");

		Map<String, Object> agents = new LinkedHashMap<>();
		agents.put("app", app);
		agents.put("customer", customer);
		agents.put("report", report);
		agents.put("dianwu", dianwu);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("avatar_url", "");
		data.put("bg_avatar_url", "");
		data.put("corpid", "");
		data.put("agents", agents);
		return data;
	}

	/** Align PHP WorkWechatService::getViewConfig avatar defaults. */
	private static void ensureAvatarUrlDefaults(Map<String, Object> data) {
		if (!data.containsKey("avatar_url") || data.get("avatar_url") == null) {
			data.put("avatar_url", "");
		}
		if (!data.containsKey("bg_avatar_url") || data.get("bg_avatar_url") == null) {
			data.put("bg_avatar_url", "");
		}
	}

	private void ensureDianwuPlaceholderIfNeeded(Map<String, Object> data) {
		Object agentsObj = data.get("agents");
		if (agentsObj instanceof Map<?, ?> agents) {
			Object dianwuObj = agents.get("dianwu");
			if (dianwuObj instanceof Map<?, ?>) {
				return;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> am = (Map<String, Object>) agents;
			Map<String, Object> dianwu = new LinkedHashMap<>();
			dianwu.put("agent_id", "");
			dianwu.put("secret", "");
			am.put("dianwu", dianwu);
		} else {
			Map<String, Object> agentsMap = new LinkedHashMap<>();
			Map<String, Object> dianwu = new LinkedHashMap<>();
			dianwu.put("agent_id", "");
			dianwu.put("secret", "");
			agentsMap.put("dianwu", dianwu);
			data.put("agents", agentsMap);
		}
	}

	public Map<String, Object> loadParsedWorkWechatConfig(long companyId) {
		String companyKey = "workwechat:config:" + sha1Hex(String.valueOf(companyId));
		String raw = sharedStringRedisTemplate.opsForValue().get(companyKey);
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException("您还没有配置企业微信信息！");
		}
		Map<String, Object> config;
		try {
			config = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			log.warn("work wechat config json parse failed companyId={}", companyId, e);
			throw new ResourceException("企业微信配置无效");
		}
		complementDianwuH5IfPresent(config, companyId);
		return config;
	}

	/**
	 * Ensures work-wechat config exists and {@code show} allows ordinary (code) login.
	 */
	public void assertShowAllowsOrdinaryLogin(long companyId) {
		Map<String, Object> config = loadParsedWorkWechatConfig(companyId);
		if (!workWechatShowIsTruthy(config.get("show"))) {
			throw new BadRequestException("当前状态不允许普通登录!");
		}
	}

	private static boolean workWechatShowIsTruthy(Object show) {
		if (show == null) {
			return false;
		}
		if (show instanceof Boolean b) {
			return b;
		}
		if (show instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (show instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t) || "false".equalsIgnoreCase(t)) {
				return false;
			}
			return true;
		}
		String t = String.valueOf(show).trim();
		if (t.isEmpty() || "0".equals(t) || "false".equalsIgnoreCase(t)) {
			return false;
		}
		return true;
	}

	/**
	 * Loads work-wechat JSON from Redis by corpid key (used by server-side customer-contact callback).
	 */
	public Map<String, Object> loadParsedWorkWechatConfigByCorpid(String corpid) {
		String key = "workwechat:configcropid:" + (corpid == null ? "" : corpid.trim());
		String raw = sharedStringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException("您还没有配置企业微信信息！");
		}
		Map<String, Object> config;
		try {
			config = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			log.warn("work wechat config json parse failed corpid key={}", key, e);
			throw new ResourceException("企业微信配置无效");
		}
		Long companyIdLong = parseCompanyIdLong(config.get("company_id"));
		if (companyIdLong != null) {
			complementDianwuH5IfPresent(config, companyIdLong);
		}
		return config;
	}

	private static Long parseCompanyIdLong(Object companyIdObj) {
		if (companyIdObj == null) {
			return null;
		}
		if (companyIdObj instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(companyIdObj).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Validates {@code corpid} and {@code agents.dianwu} fields for the distributor JS-SDK flow.
	 */
	public void validateDianwuForJsSdk(Map<String, Object> config) {
		String corpid = trimToEmpty(config.get("corpid"));
		if (!StringUtils.hasText(corpid)) {
			throw new ResourceException("未设置企业微信corpid");
		}
		Object agentsObj = config.get("agents");
		if (!(agentsObj instanceof Map<?, ?> agents)) {
			throw new ResourceException("未设置店务助手自建应用");
		}
		Object dianwuObj = agents.get("dianwu");
		if (!(dianwuObj instanceof Map<?, ?> dianwu) || dianwu.isEmpty()) {
			throw new ResourceException("未设置店务助手自建应用");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dianwuMap = (Map<String, Object>) dianwuObj;
		String agentId = trimToEmpty(dianwuMap.get("agent_id"));
		if (!StringUtils.hasText(agentId)) {
			throw new ResourceException("店务助手自建应用AgentID必填");
		}
		String secret = trimToEmpty(dianwuMap.get("secret"));
		if (!StringUtils.hasText(secret)) {
			throw new ResourceException("店务助手自建应用Secret必填");
		}
	}

	/**
	 * Validates {@code agents.dianwu.agent_id} for operator work-wechat web OAuth (authorize URL).
	 */
	public void validateDianwuAgentIdForOperatorWorkWechatOauth(Map<String, Object> viewConfig) {
		Object agentsObj = viewConfig.get("agents");
		if (!(agentsObj instanceof Map<?, ?> agents)) {
			throw new ResourceException("您还没有配置店务端企业微信信息！");
		}
		Object dianwuObj = agents.get("dianwu");
		if (!(dianwuObj instanceof Map<?, ?> dianwu) || dianwu.isEmpty()) {
			throw new ResourceException("您还没有配置店务端企业微信信息！");
		}
		String agentId = dianwu.get("agent_id") == null ? "" : String.valueOf(dianwu.get("agent_id")).trim();
		if (!StringUtils.hasText(agentId)) {
			throw new ResourceException("您还没有配置店务端企业微信信息！");
		}
	}

	public void validateGuideAppAgentForCorpApi(Map<String, Object> config) {
		if (!StringUtils.hasText(trimToEmpty(config.get("corpid")))) {
			throw new ResourceException("未设置企业微信corpid");
		}
		Object agentsObj = config.get("agents");
		if (!(agentsObj instanceof Map<?, ?> agents)) {
			throw new ResourceException("未设置导购小程序");
		}
		Object appObj = agents.get("app");
		if (!(appObj instanceof Map<?, ?>)) {
			throw new ResourceException("未设置导购小程序");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> app = (Map<String, Object>) appObj;
		if (!StringUtils.hasText(trimToEmpty(app.get("agent_id")))) {
			throw new ResourceException("导购小程序AgentID必填");
		}
		if (!StringUtils.hasText(trimToEmpty(app.get("secret")))) {
			throw new ResourceException("导购小程序Secret必填");
		}
	}

	private void complementDianwuH5IfPresent(Map<String, Object> config, long companyId) {
		Object agentsObj = config.get("agents");
		if (!(agentsObj instanceof Map<?, ?> agents)) {
			return;
		}
		if (!agents.containsKey("dianwu")) {
			return;
		}
		Object dianwuObj = agents.get("dianwu");
		if (!(dianwuObj instanceof Map<?, ?>)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dianwu = (Map<String, Object>) dianwuObj;
		String baseUri = moduleProperties.getDisWorkwechatH5BaseUri() != null
				? moduleProperties.getDisWorkwechatH5BaseUri()
				: "";
		dianwu.put("h5_url", baseUri + "?company_id=" + companyId);
		dianwu.put("h5_host", hostPortFromBaseUri(baseUri));
	}

	public Map<String, Object> saveWorkWechatConfig(Long companyId, Map<String, Object> fullRequestBody) {
		Map<String, Object> trimmedMap = buildTrimmedMap(fullRequestBody);
		validateTrimmedMap(trimmedMap);
		Map<String, Object> data = assemblePersistData(companyId, trimmedMap);
		String companyKey = "workwechat:config:" + sha1Hex(String.valueOf(companyId));
		String corpidTrimmed = trimToEmpty(trimmedMap.get("corpid"));
		String cropidKey = "workwechat:configcropid:" + corpidTrimmed;
		String json;
		try {
			json = objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new ResourceException("保存企业微信配置失败");
		}
		sharedStringRedisTemplate.opsForValue().set(companyKey, json);
		sharedStringRedisTemplate.opsForValue().set(cropidKey, json);

		@SuppressWarnings("unchecked")
		Map<String, Object> agents = (Map<String, Object>) data.get("agents");
		Object appidObj = agents.get("app") instanceof Map<?, ?> appMap ? appMap.get("appid") : "";
		String appidKey = "workwechat:configappid:" + trimToEmpty(appidObj);
		sharedStringRedisTemplate.opsForValue().set(appidKey, String.valueOf(companyId));

		if (log.isDebugEnabled()) {
			log.debug("key:{}读取存入数据:{}", companyKey, sharedStringRedisTemplate.opsForValue().get(companyKey));
		}

		return attachConfig(data, companyId);
	}

	private static Map<String, Object> buildTrimmedMap(Map<String, Object> root) {
		Map<String, Object> trimmedMap = new LinkedHashMap<>();
		if (root.containsKey("show")) {
			trimmedMap.put("show", root.get("show"));
		}
		if (root.containsKey("corpid")) {
			trimmedMap.put("corpid", root.get("corpid"));
		}
		Map<String, Object> agentsTrimmed = new LinkedHashMap<>();
		for (String ak : new String[] {"app", "customer", "report", "dianwu"}) {
			Object v = dataGet(root, "agents." + ak);
			if (v != null) {
				agentsTrimmed.put(ak, v);
			}
		}
		if (!agentsTrimmed.isEmpty()) {
			trimmedMap.put("agents", agentsTrimmed);
		}
		return trimmedMap;
	}

	private static Object dataGet(Map<String, Object> root, String dotPath) {
		String[] parts = dotPath.split("\\.");
		Object cur = root;
		for (String p : parts) {
			if (cur == null) {
				return null;
			}
			if (!(cur instanceof Map<?, ?> m)) {
				return null;
			}
			cur = m.get(p);
		}
		return cur;
	}

	private void validateTrimmedMap(Map<String, Object> trimmedMap) {
		if (trimmedMap.containsKey("0") || trimmedMap.containsKey("1")) {
			if (!trimmedMap.containsKey("show")) {
				throw new BadRequestException("是否开启企业微信");
			}
			Object show = trimmedMap.get("show");
			if (!StringUtils.hasText(String.valueOf(show).trim())) {
				throw new BadRequestException("是否开启企业微信");
			}
		}
		String corpid = trimToEmpty(trimmedMap.get("corpid"));
		if (!StringUtils.hasText(corpid)) {
			throw new BadRequestException("企业微信corpid必填");
		}
	}

	private Map<String, Object> assemblePersistData(Long companyId, Map<String, Object> trimmedMap) {
		String corpid = trimToEmpty(trimmedMap.get("corpid"));
		@SuppressWarnings("unchecked")
		Map<String, Object> srcAgents =
				trimmedMap.get("agents") instanceof Map<?, ?> m ? (Map<String, Object>) m : Collections.emptyMap();

		Map<String, Object> appSrc = asStringObjectMap(srcAgents.get("app"));
		Map<String, Object> customerSrc = asStringObjectMap(srcAgents.get("customer"));
		Map<String, Object> reportSrc = asStringObjectMap(srcAgents.get("report"));
		Map<String, Object> dianwuSrc = asStringObjectMap(srcAgents.get("dianwu"));

		Map<String, Object> app = new LinkedHashMap<>();
		app.put("appid", trimField(appSrc, "appid"));
		app.put("agent_id", trimField(appSrc, "agent_id"));
		app.put("secret", trimField(appSrc, "secret"));
		app.put("token", trimField(appSrc, "token"));
		app.put("aes_key", trimField(appSrc, "aes_key"));

		Map<String, Object> customer = new LinkedHashMap<>();
		customer.put("secret", trimField(customerSrc, "secret"));
		customer.put("token", trimField(customerSrc, "token"));
		customer.put("aes_key", trimField(customerSrc, "aes_key"));

		Map<String, Object> report = new LinkedHashMap<>();
		report.put("secret", trimField(reportSrc, "secret"));
		report.put("token", trimField(reportSrc, "token"));
		report.put("aes_key", trimField(reportSrc, "aes_key"));

		Map<String, Object> dianwu = new LinkedHashMap<>();
		dianwu.put("agent_id", trimField(dianwuSrc, "agent_id"));
		dianwu.put("secret", trimField(dianwuSrc, "secret"));

		Map<String, Object> agents = new LinkedHashMap<>();
		agents.put("app", app);
		agents.put("customer", customer);
		agents.put("report", report);
		agents.put("dianwu", dianwu);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("show", "1");
		data.put("corpid", corpid);
		data.put("company_id", companyId);
		data.put("agents", agents);
		return data;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asStringObjectMap(Object o) {
		if (o instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static String trimField(Map<String, Object> src, String key) {
		if (!src.containsKey(key) || src.get(key) == null) {
			return "";
		}
		return String.valueOf(src.get(key)).trim();
	}

	private static String trimToEmpty(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}

	private Map<String, Object> attachConfig(Map<String, Object> data, Long companyId) {
		Map<String, Object> result = deepCopyMap(data);

		Object agentsObj = result.get("agents");
		if (!(agentsObj instanceof Map<?, ?> agentsMapRaw)) {
			return result;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> agentsMap = (Map<String, Object>) agentsMapRaw;

		if (agentsMap.containsKey("customer") && agentsMap.get("customer") instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			Map<String, Object> customer = (Map<String, Object>) agentsMap.get("customer");
			String corpid = trimToEmpty(result.get("corpid"));
			customer.put("URL", "/workwechat/customer/notify/" + corpid);
			String token = trimToEmpty(customer.get("token"));
			if (!StringUtils.hasText(token)) {
				customer.put("token", randomAlphanumeric(9 + secureRandom.nextInt(5)));
			}
			String aesKey = trimToEmpty(customer.get("aes_key"));
			if (!StringUtils.hasText(aesKey)) {
				customer.put("aes_key", randomAlphanumeric(43));
			}
		}

		if (agentsMap.containsKey("dianwu") && agentsMap.get("dianwu") instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			Map<String, Object> dianwu = (Map<String, Object>) agentsMap.get("dianwu");
			String baseUri = moduleProperties.getDisWorkwechatH5BaseUri() != null
					? moduleProperties.getDisWorkwechatH5BaseUri()
					: "";
			dianwu.put("h5_url", baseUri + "?company_id=" + companyId);
			dianwu.put("h5_host", hostPortFromBaseUri(baseUri));
			String verifyFileName = verifyDomainFileQueryService
					.findLatestVerifyFileBaseName(companyId)
					.filter(StringUtils::hasText)
					.map(n -> n + ".txt")
					.orElse("");
			dianwu.put("verify_file_name", verifyFileName);
		}

		return result;
	}

	private static Map<String, Object> deepCopyMap(Map<String, Object> src) {
		Map<String, Object> out = new LinkedHashMap<>(src);
		Object agentsInObj = src.get("agents");
		Map<String, Object> agentsOut = new LinkedHashMap<>();
		if (agentsInObj instanceof Map<?, ?> agentsIn) {
			for (Map.Entry<?, ?> e : agentsIn.entrySet()) {
				String key = String.valueOf(e.getKey());
				if (e.getValue() instanceof Map<?, ?> inner) {
					Map<String, Object> innerCopy = new LinkedHashMap<>();
					for (Map.Entry<?, ?> ie : inner.entrySet()) {
						innerCopy.put(String.valueOf(ie.getKey()), ie.getValue());
					}
					agentsOut.put(key, innerCopy);
				} else {
					agentsOut.put(key, e.getValue());
				}
			}
		}
		out.put("agents", agentsOut);
		return out;
	}

	private String randomAlphanumeric(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			sb.append(ALPHANUM.charAt(secureRandom.nextInt(ALPHANUM.length())));
		}
		return sb.toString();
	}

	/**
	 * Extracts host[:port] from a base URI without {@link java.net.URI#getHost()}, so Unicode
	 * hostnames (IDN) match {@link java.net.URI} parsing behavior.
	 */
	private static String hostPortFromBaseUri(String baseUri) {
		if (baseUri == null) {
			return "";
		}
		String s = baseUri.trim();
		if (!StringUtils.hasText(s)) {
			return "";
		}
		if (!s.contains("://")) {
			s = "http://" + s;
		}
		int schemeSep = s.indexOf("://");
		if (schemeSep < 0) {
			return "";
		}
		int start = schemeSep + 3;
		int len = s.length();
		int end = len;
		for (int j = start; j < len; j++) {
			char c = s.charAt(j);
			if (c == '/' || c == '?' || c == '#') {
				end = j;
				break;
			}
		}
		if (start >= end) {
			return "";
		}
		String authority = s.substring(start, end);
		int at = authority.lastIndexOf('@');
		if (at >= 0) {
			authority = authority.substring(at + 1);
		}
		return authority;
	}
}
