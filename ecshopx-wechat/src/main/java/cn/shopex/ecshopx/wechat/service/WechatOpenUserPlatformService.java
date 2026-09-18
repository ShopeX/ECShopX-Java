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
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.service.openplatform.WechatOpenPlatformAccountClient;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatOpenUserPlatformService {

	private static final Logger log = LoggerFactory.getLogger(WechatOpenUserPlatformService.class);

	private final WechatAuthQueryService wechatAuthQueryService;
	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final WechatOpenPlatformAccountClient accountClient;

	public WechatOpenUserPlatformService(
			WechatAuthQueryService wechatAuthQueryService,
			WechatOpenPlatformAuthorizerTokenService tokenService,
			WechatOpenPlatformAccountClient accountClient) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.tokenService = tokenService;
		this.accountClient = accountClient;
	}

	public void openCreate(long companyId, String authorizerAppidFromJwtOrNull) {
		String s = authorizerAppidFromJwtOrNull == null ? "" : authorizerAppidFromJwtOrNull.trim();
		if (!hasEffectiveAuthorizerAppid(s)) {
			String fromDb = wechatAuthQueryService.findBoundMiniProgramAuthorizerAppid(companyId);
			s = (fromDb == null) ? "" : fromDb.trim();
		}
		String appid = s;
		if (!hasEffectiveAuthorizerAppid(appid)) {
			throw new BadRequestException("未绑定公众号或小程序");
		}

		String token = tokenService.getAuthorizerAccessToken(appid);

		JsonNode getRes = null;
		try {
			getRes = accountClient.openGet(token, appid);
		} catch (Exception ex) {
			getRes = null;
		}

		String openAppid = null;
		if (getRes != null && getRes.path("errcode").asInt(0) == 0) {
			String cand = getRes.path("open_appid").asText(null);
			if (StringUtils.hasText(cand)) {
				openAppid = cand.trim();
			}
		}

		if (!StringUtils.hasText(openAppid)) {
			JsonNode cr = accountClient.openCreate(token, appid);
			if (cr == null
					|| cr.isMissingNode()
					|| (cr.hasNonNull("errcode") && cr.get("errcode").asInt(0) != 0)) {
				String msg = (cr != null && !cr.isMissingNode())
						? cr.path("errmsg").asText("创建开放平台账号失败")
						: "创建开放平台账号失败";
				throw new ResourceException(StringUtils.hasText(msg) ? msg : "创建开放平台账号失败");
			}
			String created = cr.path("open_appid").asText(null);
			if (!StringUtils.hasText(created)) {
				throw new ResourceException("创建开放平台账号失败");
			}
			openAppid = created.trim();
		}

		List<WechatAuth> list = wechatAuthQueryService.listBoundMiniProgramsForCompany(companyId);
		if (list == null || list.isEmpty()) {
			return;
		}
		for (WechatAuth row : list) {
			Integer v = row.getVerifyTypeInfo();
			if (v == null || v == -1) {
				continue;
			}
			String wxaAppid = row.getAuthorizerAppid();
			if (!hasEffectiveAuthorizerAppid(wxaAppid)) {
				continue;
			}
			bindWxa(wxaAppid.trim(), openAppid);
		}
	}

	private void bindWxa(String wxaAppid, String openAppid) {
		String wxaToken = tokenService.getAuthorizerAccessToken(wxaAppid);
		JsonNode bindCheck = null;
		try {
			bindCheck = accountClient.openGet(wxaToken, wxaAppid);
		} catch (Exception ex) {
			log.debug("open get before bind skipped", ex);
		}
		if (bindCheck != null && bindCheck.path("errcode").asInt(-1) == 0) {
			String existing = bindCheck.path("open_appid").asText("");
			if (openAppid.equals(existing)) {
				return;
			}
			if (StringUtils.hasText(existing) && !openAppid.equals(existing)) {
				accountClient.openUnbind(wxaToken, wxaAppid, existing);
			}
		}
		JsonNode bindResult = accountClient.openBind(wxaToken, wxaAppid, openAppid);
		if (bindResult == null || bindResult.isMissingNode()) {
			throw new ResourceException("绑定失败！");
		}
		if (bindResult.hasNonNull("errcode") && bindResult.get("errcode").asInt(0) != 0) {
			log.info("bindWxa_error:{}", bindResult);
			throw new ResourceException("绑定失败！");
		}
	}

	public static boolean hasEffectiveAuthorizerAppid(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		if ("false".equalsIgnoreCase(t)) {
			return false;
		}
		if ("[]".equals(t) || "{}".equals(t)) {
			return false;
		}
		return true;
	}
}
