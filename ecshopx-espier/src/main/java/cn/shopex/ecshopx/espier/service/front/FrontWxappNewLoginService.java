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

package cn.shopex.ecshopx.espier.service.front;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.members.service.h5.H5LoginOrchestrator;
import cn.shopex.ecshopx.members.service.h5.dto.H5LoginAttemptResult;
import cn.shopex.ecshopx.members.service.h5.prelogin.H5AliappPreLoginService;
import cn.shopex.ecshopx.members.service.h5.prelogin.H5WxappPreLoginService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class FrontWxappNewLoginService {

	private static final String[] WXAPP_PARAM_KEYS = {
			"appid", "code", "iv", "encryptedData", "signature", "rawData", "uid", "open_id",
			"source_id", "monitor_id", "inviter_id", "source_from", "distributor_id",
			"salesperson_id", "invite_code", "employee_auth", "purchanse_share_code",
			"puid", "gu_user_id", "dtid", "work_userid", "channel", "shuyunappid"
	};

	private static final String[] ALIAPP_PARAM_KEYS = {
			"code", "encryptedData", "appid", "signature", "rawData", "uid",
			"source_id", "monitor_id", "inviter_id", "source_from", "distributor_id",
			"salesperson_id", "invite_code", "employee_auth", "purchanse_share_code",
			"puid", "gu_user_id", "dtid", "work_userid", "channel", "shuyunappid"
	};

	private final H5WxappPreLoginService h5WxappPreLoginService;

	private final H5AliappPreLoginService h5AliappPreLoginService;

	private final H5LoginOrchestrator h5LoginOrchestrator;

	public FrontWxappNewLoginService(
			H5WxappPreLoginService h5WxappPreLoginService,
			H5AliappPreLoginService h5AliappPreLoginService,
			H5LoginOrchestrator h5LoginOrchestrator) {
		this.h5WxappPreLoginService = h5WxappPreLoginService;
		this.h5AliappPreLoginService = h5AliappPreLoginService;
		this.h5LoginOrchestrator = h5LoginOrchestrator;
	}

	public LinkedHashMap<String, Object> login(HttpServletRequest request, Map<String, Object> merged) {
		long companyId = readCompanyId(request);
		int silent = readSilentFromValue(merged.get("silent"));

		Object rawAuthType = merged.get("auth_type");
		String authTypeRawString = rawAuthType == null ? "" : String.valueOf(rawAuthType);
		String authType = StringUtils.trimWhitespace(authTypeRawString);

		Map<String, Object> credentials;
		Map<String, Object> preLoginDataForResponse;

		switch (authType) {
			case "local" -> {
				String username = merged.containsKey("username") && merged.get("username") != null
						? StringUtils.trimWhitespace(String.valueOf(merged.get("username")))
						: "";
				credentials = new LinkedHashMap<>();
				credentials.put("username", username);
				credentials.put("vcode", trimToEmpty(merged.get("vcode")));
				credentials.put("check_type", resolveCheckType(merged));
				credentials.put("password", trimToEmpty(merged.get("password")));
				credentials.put("company_id", companyId);
				credentials.put("auth_type", "local");
				credentials.put("auto_register", readSilentFromValue(merged.get("auto_register")));
				credentials.put("silent", silent);
				preLoginDataForResponse = Map.of("mobile", username);
			}
			case "wx_offiaccount" -> {
				credentials = new LinkedHashMap<>(merged);
				credentials.put("company_id", companyId);
				credentials.put("auth_type", "wx_offiaccount");
				preLoginDataForResponse = null;
			}
			case "wxapp" -> {
				Map<String, Object> params = copyWhitelisted(merged, WXAPP_PARAM_KEYS);
				injectAuthContextParams(request, merged, params, companyId);
				Map<String, Object> wxappPreLoginInfo = h5WxappPreLoginService.wxappPreLogin(params);
				credentials = new LinkedHashMap<>();
				credentials.put("code", trimToEmpty(merged.get("code")));
				credentials.put("appid", trimToEmpty(merged.get("appid")));
				credentials.put("auth_type", "wxapp");
				credentials.put("company_id", companyId);
				credentials.put("origin", merged.containsKey("origin") ? String.valueOf(merged.get("origin")) : "");
				credentials.put(
						"openid",
						StringUtils.trimWhitespace(Objects.toString(wxappPreLoginInfo.get("open_id"), "")));
				credentials.put(
						"unionid",
						StringUtils.trimWhitespace(Objects.toString(wxappPreLoginInfo.get("unionid"), "")));
				preLoginDataForResponse = wxappPreLoginInfo;
			}
			case "aliapp" -> {
				Map<String, Object> params = copyWhitelisted(merged, ALIAPP_PARAM_KEYS);
				injectAuthContextParams(request, merged, params, companyId);
				Map<String, Object> aliappPreLoginInfo = h5AliappPreLoginService.aliappPreLogin(params);
				credentials = new LinkedHashMap<>();
				credentials.put("auth_type", "aliapp");
				credentials.put("company_id", companyId);
				credentials.put("code", trimToEmpty(merged.get("code")));
				credentials.put(
						"alipay_user_id",
						StringUtils.trimWhitespace(Objects.toString(aliappPreLoginInfo.get("alipay_user_id"), "")));
				credentials.put("origin", merged.containsKey("origin") ? String.valueOf(merged.get("origin")) : "");
				preLoginDataForResponse = null;
			}
			case "social_oauth" -> {
				credentials = new LinkedHashMap<>(merged);
				credentials.put("company_id", companyId);
				credentials.put("auth_type", "social_oauth");
				String origin = request.getHeader("Origin");
				credentials.put("origin", origin != null ? origin : "");
				if (!credentials.containsKey("version_tag") || !StringUtils.hasText(String.valueOf(credentials.get("version_tag")))) {
					credentials.put("version_tag", "touch");
				}
				preLoginDataForResponse = null;
			}
			default -> throw new ResourceException("缺少参数，登录失败！");
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("token", null);
		data.put("pre_login_data", preLoginDataForResponse);
		data.put("status", 1);
		data.put("error_message", null);

		try {
			H5LoginAttemptResult r = h5LoginOrchestrator.attempt(credentials);
			if (r.success()) {
				data.put("token", r.jwtToken());
			} else if (r.credentialsRejected()) {
				applySilentAttemptFailure(data, silent);
			} else {
				applySilentAttemptFailure(data, silent);
			}
		} catch (ResourceException ex) {
			if (silent == 0) {
				throw ex;
			}
			data.put("status", 0);
			data.put("error_message", ex.getMessage());
		} catch (IllegalStateException ex) {
			if (silent == 0) {
				throw ex;
			}
			data.put("status", 0);
			data.put("error_message", "error!");
		} catch (Throwable t) {
			if (silent == 0) {
				throw t;
			}
			data.put("status", 0);
			data.put("error_message", "error!");
		}
		return data;
	}

	private static void applySilentAttemptFailure(LinkedHashMap<String, Object> data, int silent) {
		if (silent == 0) {
			throw new ResourceException("缺少参数，登录失败！");
		}
		data.put("status", 0);
		data.put("error_message", "error!");
	}

	private static long readCompanyId(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId = 0L;
		if (raw instanceof Number n) {
			companyId = n.longValue();
		} else if (raw != null) {
			try {
				companyId = Long.parseLong(String.valueOf(raw).trim());
			} catch (NumberFormatException ignored) {
				companyId = 0L;
			}
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}

	private static int readSilentFromValue(Object rawSilent) {
		if (rawSilent == null) {
			return 0;
		}
		if (rawSilent instanceof Number n) {
			return n.intValue();
		}
		String s = StringUtils.trimWhitespace(String.valueOf(rawSilent));
		if (s.isEmpty()) {
			return 0;
		}
		if (s.chars().allMatch(c -> c >= '0' && c <= '9')) {
			try {
				return Integer.parseInt(s);
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	private static String resolveCheckType(Map<String, Object> merged) {
		Object v = merged.get("check_type");
		if (v == null) {
			return "password";
		}
		String s = StringUtils.trimWhitespace(String.valueOf(v));
		return StringUtils.hasText(s) ? s : "password";
	}

	private static String trimToEmpty(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	@SuppressWarnings("unchecked")
	private static void injectAuthContextParams(
			HttpServletRequest request,
			Map<String, Object> merged,
			Map<String, Object> params,
			long companyId) {
		params.put("company_id", companyId);
		Object claimsRaw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (claimsRaw instanceof Map<?, ?> cm && !cm.isEmpty()) {
			Map<String, Object> claims = (Map<String, Object>) cm;
			Object wxa = claims.get("wxapp_appid");
			params.put("wxa_appid", wxa != null ? String.valueOf(wxa) : trimToEmpty(merged.get("appid")));
			Object woa = claims.get("woa_appid");
			params.put("authorizer_appid", woa != null ? String.valueOf(woa) : "");
			Object apiFrom = claims.get("api_from");
			params.put("api_from", apiFrom != null ? String.valueOf(apiFrom) : "h5app");
		} else {
			params.put("wxa_appid", trimToEmpty(merged.get("appid")));
			params.put("authorizer_appid", "");
			params.put("api_from", "h5app");
		}
	}

	private static Map<String, Object> copyWhitelisted(Map<String, Object> merged, String[] keys) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (String k : keys) {
			if (merged.containsKey(k)) {
				out.put(k, merged.get(k));
			}
		}
		return out;
	}
}
