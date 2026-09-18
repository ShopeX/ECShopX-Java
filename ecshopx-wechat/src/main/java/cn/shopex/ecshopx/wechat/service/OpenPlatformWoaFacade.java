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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenPlatformWoaFacade {

	private static final Logger log = LoggerFactory.getLogger(OpenPlatformWoaFacade.class);

	private static final String MODE_AUTHORIZED = "authorized";
	private static final String MODE_DIRECT = "direct";

	private final WechatAuthQueryService wechatAuthQueryService;
	private final TrustLoginWeixinTouchConfigService trustLoginWeixinTouchConfigService;
	private final ObjectMapper objectMapper;

	public OpenPlatformWoaFacade(
			WechatAuthQueryService wechatAuthQueryService,
			TrustLoginWeixinTouchConfigService trustLoginWeixinTouchConfigService,
			ObjectMapper objectMapper) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.trustLoginWeixinTouchConfigService = trustLoginWeixinTouchConfigService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getWoaApp(String companyIdRaw, String trustloginTag, String versionTag) {
		if (companyIdRaw == null || companyIdRaw.isEmpty() || "0".equals(companyIdRaw)) {
			throw new BadRequestException("缺少参数", 400);
		}
		String authorizerAppid = wechatAuthQueryService.getAuthorizerAppidForWoaQuery(companyIdRaw);
		if (StringUtils.hasText(authorizerAppid)) {
			Map<String, Object> authorized = new LinkedHashMap<>();
			authorized.put("mode", MODE_AUTHORIZED);
			authorized.put("authorizerAppid", authorizerAppid.trim());
			return authorized;
		}
		Map<String, Object> row = fetchTrustLoginConfigRowOrEmpty(trustloginTag, versionTag, companyIdRaw);
		if (!row.isEmpty() && hasNonEmptyStatus(row)) {
			Map<String, Object> direct = new LinkedHashMap<>();
			direct.put("mode", MODE_DIRECT);
			direct.putAll(row);
			return direct;
		}
		throw logWoaAppErrorAndThrow(companyIdRaw, trustloginTag, versionTag, authorizerAppid, row);
	}

	public Map<String, Object> getWoaApp(Long companyId, String trustloginTag, String versionTag) {
		if (companyId == null) {
			throw new BadRequestException("当前页面companyId必填");
		}
		return getWoaApp(String.valueOf(companyId), trustloginTag, versionTag);
	}

	public Map<String, Object> getWoaAppForWxappOrderJsPay(long companyId) {
		if (companyId <= 0L) {
			throw new BadRequestException("当前页面companyId必填");
		}
		String appId = wechatAuthQueryService.getAuthorizerAppidForWoaQuery(String.valueOf(companyId));
		if (!StringUtils.hasText(appId)) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定");
		}
		Map<String, Object> authorized = new LinkedHashMap<>();
		authorized.put("mode", MODE_AUTHORIZED);
		authorized.put("authorizerAppid", appId.trim());
		return authorized;
	}

	private Map<String, Object> fetchTrustLoginConfigRowOrEmpty(
			String trustloginTag, String versionTag, String companyIdRaw) {
		return trustLoginWeixinTouchConfigService.getConfigRow(trustloginTag, versionTag, companyIdRaw);
	}

	private ResourceException logWoaAppErrorAndThrow(
			String companyIdRaw,
			String trustloginTag,
			String versionTag,
			String authorizerAppid,
			Map<String, Object> trustLoginRow) {
		try {
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("appid", authorizerAppid);
			Map<String, Object> filter = new LinkedHashMap<>();
			filter.put("company_id", companyIdRaw);
			filter.put("trustlogin_tag", trustloginTag);
			filter.put("version_tag", versionTag);
			payload.put("filter", filter);
			payload.put("configInfo", trustLoginRow.isEmpty() ? null : trustLoginRow);
			log.info("woaAppError:{}", objectMapper.writeValueAsString(payload));
		} catch (Exception e) {
			log.info(
					"woaAppError: appid={} companyIdRaw={} trustloginTag={} versionTag={} configEmpty={}",
					authorizerAppid,
					companyIdRaw,
					trustloginTag,
					versionTag,
					trustLoginRow.isEmpty(),
					e);
		}
		return new ResourceException("公众号信息有误！");
	}

	/** True when {@code status} is present and not blank, zero, or false. */
	private static boolean hasNonEmptyStatus(Map<String, Object> row) {
		Object st = row.get("status");
		if (st == null) {
			return false;
		}
		if (st instanceof Boolean b) {
			return b;
		}
		if (st instanceof Number n) {
			return n.longValue() != 0;
		}
		String s = String.valueOf(st).trim();
		return !s.isEmpty() && !"0".equals(s);
	}
}
