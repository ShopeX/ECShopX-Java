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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.constant.WxMaApiUrlConstants;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaExceptions;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;

@Service
public class WxaOpenPlatformCodeApiClient {

	private static final TypeReference<LinkedHashMap<String, Object>> ROOT_MAP_TYPE = new TypeReference<>() {};

	private final WxJavaMaRuntime wxJavaMaRuntime;
	private final ObjectMapper objectMapper;

	public WxaOpenPlatformCodeApiClient(WxJavaMaRuntime wxJavaMaRuntime, ObjectMapper objectMapper) {
		this.wxJavaMaRuntime = wxJavaMaRuntime;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> queryMergedWxDomainConfig(String wxaAppId) {
		WxMaService svc = wxJavaMaRuntime.ma(wxaAppId.trim());
		Map<String, Object> serverBody = new LinkedHashMap<>();
		serverBody.put("action", "get");
		serverBody.put("requestdomain", new ArrayList<String>());
		serverBody.put("wsrequestdomain", new ArrayList<String>());
		serverBody.put("uploaddomain", new ArrayList<String>());
		serverBody.put("downloaddomain", new ArrayList<String>());
		serverBody.put("udpdomain", new ArrayList<String>());
		serverBody.put("tcpdomain", new ArrayList<String>());
		LinkedHashMap<String, Object> serverMap =
				jsonRootToLinkedMap(postJsonReturnRootLenient(svc, WxMaApiUrlConstants.Setting.MODIFY_DOMAIN_URL, serverBody));

		Map<String, Object> webviewBody = new LinkedHashMap<>();
		webviewBody.put("action", "get");
		LinkedHashMap<String, Object> webviewMap =
				jsonRootToLinkedMap(
						postJsonReturnRootLenient(svc, WxMaApiUrlConstants.Setting.SET_WEB_VIEW_DOMAIN_URL, webviewBody));

		LinkedHashMap<String, Object> out = new LinkedHashMap<>(serverMap);
		out.putAll(webviewMap);
		return out;
	}

	public void modifyDomain(String wxaAppId, Map<String, Object> domainPayload) {
		modifyDomain(wxaAppId, domainPayload, true);
	}

	public void modifyDomain(String wxaAppId, Map<String, Object> domainPayload, boolean failOnWechatErrcode) {
		WxMaService svc = wxJavaMaRuntime.ma(wxaAppId.trim());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("action", "set");
		putDomainArrays(body, domainPayload, "requestdomain");
		putDomainArrays(body, domainPayload, "wsrequestdomain");
		putDomainArrays(body, domainPayload, "uploaddomain");
		putDomainArrays(body, domainPayload, "downloaddomain");
		putDomainArrays(body, domainPayload, "udpdomain");
		putDomainArrays(body, domainPayload, "tcpdomain");
		postJsonExpectOk(svc, WxMaApiUrlConstants.Setting.MODIFY_DOMAIN_URL, body, failOnWechatErrcode);
	}

	public void setWebviewDomainSet(
			String wxaAppId, List<String> webviewDomains, boolean failOnWechatErrcode) {
		WxMaService svc = wxJavaMaRuntime.ma(wxaAppId.trim());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("action", "set");
		body.put("webviewdomain", webviewDomains);
		postJsonExpectOk(svc, WxMaApiUrlConstants.Setting.SET_WEB_VIEW_DOMAIN_URL, body, failOnWechatErrcode);
	}

	public void commit(String wxaAppId, String templateId, String extJson, String userVersion, String userDesc) {
		WxMaService svc = wxJavaMaRuntime.ma(wxaAppId.trim());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("template_id", templateId);
		body.put("ext_json", extJson);
		body.put("user_version", userVersion);
		body.put("user_desc", userDesc);
		postJsonExpectOk(svc, WxMaApiUrlConstants.Code.COMMIT_URL, body, true);
	}

	public JsonNode getCategory(String wxaAppId) {
		return getReturnRoot(wxJavaMaRuntime.ma(wxaAppId.trim()), WxMaApiUrlConstants.Code.GET_CATEGORY_URL);
	}

	public JsonNode submitAudit(String wxaAppId, Map<String, Object> payload) {
		return postJsonReturnRoot(wxJavaMaRuntime.ma(wxaAppId.trim()), WxMaApiUrlConstants.Code.SUBMIT_AUDIT_URL, payload);
	}

	public JsonNode getLatestAuditstatus(String wxaAppId) {
		return getReturnRoot(wxJavaMaRuntime.ma(wxaAppId.trim()), WxMaApiUrlConstants.Code.GET_LATEST_AUDIT_STATUS_URL);
	}

	public JsonNode withdrawAudit(String wxaAppId) {
		return getReturnRoot(wxJavaMaRuntime.ma(wxaAppId.trim()), WxMaApiUrlConstants.Code.UNDO_CODE_AUDIT_URL);
	}

	public JsonNode getRevertCodeRelease(String wxaAppId) {
		try {
			JsonNode root =
					getReturnRoot(wxJavaMaRuntime.ma(wxaAppId.trim()), WxMaApiUrlConstants.Code.REVERT_CODE_RELEASE_URL);
			return root;
		} catch (Exception e) {
			return null;
		}
	}

	public void releaseMiniProgramIgnoring85052(String wxaAppId) {
		WxMaService svc = wxJavaMaRuntime.ma(wxaAppId.trim());
		Map<String, Object> body = new LinkedHashMap<>();
		JsonNode root = postJsonReturnRoot(svc, WxMaApiUrlConstants.Code.RELEASE_URL, body);
		int ec = root.path("errcode").asInt(0);
		if (ec == 0 || ec == 85052) {
			return;
		}
		throw new BadRequestException(root.path("errmsg").asText("微信接口错误"));
	}

	private void putDomainArrays(Map<String, Object> body, Map<String, Object> src, String key) {
		Object v = src == null ? null : src.get(key);
		body.put(key, toStringList(v));
	}

	private static List<String> toStringList(Object v) {
		if (v == null) {
			return new ArrayList<>();
		}
		if (v instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o != null) {
					out.add(String.valueOf(o));
				}
			}
			return out;
		}
		return new ArrayList<>();
	}

	private void postJsonExpectOk(WxMaService svc, String url, Map<String, Object> body, boolean failOnWechatErrcode) {
		JsonNode root = postJsonReturnRoot(svc, url, body);
		int ec = root.path("errcode").asInt(0);
		if (ec != 0 && failOnWechatErrcode) {
			throw new BadRequestException(root.path("errmsg").asText("微信接口错误"), 400);
		}
	}

	private JsonNode postJsonReturnRoot(WxMaService svc, String url, Map<String, Object> body) {
		try {
			String raw = svc.post(url, body);
			return parseRoot(raw);
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toBadRequestOrUnauthorized(e);
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("微信接口请求失败");
		}
	}

	private JsonNode postJsonReturnRootLenient(WxMaService svc, String url, Map<String, Object> body) {
		try {
			String raw = svc.post(url, body);
			return parseRoot(raw);
		} catch (Exception e) {
			return objectMapper.createObjectNode();
		}
	}

	private JsonNode getReturnRoot(WxMaService svc, String url) {
		try {
			String raw = svc.get(url, "");
			return parseRoot(raw);
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toBadRequestOrUnauthorized(e);
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("微信接口请求失败");
		}
	}

	private LinkedHashMap<String, Object> jsonRootToLinkedMap(JsonNode root) {
		if (root == null || !root.isObject()) {
			return new LinkedHashMap<>();
		}
		try {
			LinkedHashMap<String, Object> m = objectMapper.convertValue(root, ROOT_MAP_TYPE);
			return m != null ? m : new LinkedHashMap<>();
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}

	private JsonNode parseRoot(String raw) {
		if (raw == null || raw.isEmpty()) {
			return objectMapper.createObjectNode();
		}
		try {
			return objectMapper.readTree(raw);
		} catch (Exception e) {
			return objectMapper.createObjectNode();
		}
	}
}
