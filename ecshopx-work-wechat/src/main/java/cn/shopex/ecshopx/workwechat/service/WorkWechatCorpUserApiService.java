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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.thirdparty.service.workwechat.WorkWechatAccessTokenProvider;
import cn.shopex.ecshopx.workwechat.wxjava.WorkWechatWxCpRuntime;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.constant.WxCpApiPathConsts;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WorkWechatCorpUserApiService {

	private final WorkWechatAccessTokenProvider workWechatAccessTokenProvider;
	private final WorkWechatWxCpRuntime workWechatWxCpRuntime;
	private final ObjectMapper objectMapper;

	public WorkWechatCorpUserApiService(
			WorkWechatAccessTokenProvider workWechatAccessTokenProvider,
			WorkWechatWxCpRuntime workWechatWxCpRuntime,
			ObjectMapper objectMapper) {
		this.workWechatAccessTokenProvider = workWechatAccessTokenProvider;
		this.workWechatWxCpRuntime = workWechatWxCpRuntime;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getUser(long companyId, String userid) {
		return fetchUserGetAndParse(companyId, userid, false);
	}

	public Map<String, Object> getUserForOperatorWorkWechatOauth(long companyId, String userid) {
		return fetchUserGetAndParse(companyId, userid, true);
	}

	private Map<String, Object> fetchUserGetAndParse(long companyId, String userid, boolean operatorWorkWechatOauth) {
		WxCpService cp = cpBearer(companyId);
		String uid = userid == null ? "" : userid.trim();
		if (!StringUtils.hasText(uid)) {
			throw new ResourceException("企业微信接口调用失败：userid 为空");
		}
		String body =
				wxGet(cp, WxCpApiPathConsts.User.USER_GET, uid, "user/get", operatorWorkWechatOauth);
		JsonNode root = readJsonRoot(body, "user/get");
		int errcode = root.path("errcode").asInt(0);
		if (errcode != 0) {
			String errmsg = root.path("errmsg").asText("");
			log.warn("work wechat user/get errcode={} errmsg={}", errcode, errmsg);
			if (operatorWorkWechatOauth) {
				throw new ResourceException("该账号不在店务应用可见范围内");
			}
			throw new ResourceException("企业微信错误: errcode=" + errcode + ", errmsg=" + errmsg);
		}
		if (!root.isObject()) {
			throw new ResourceException("企业微信接口调用失败");
		}
		Map<String, Object> m = objectMapper.convertValue(root, new TypeReference<>() {});
		return m == null ? new LinkedHashMap<>() : m;
	}

	public Map<String, Object> getExternalContact(long companyId, String externalUserid) {
		WxCpService cp = cpBearer(companyId);
		String ext = externalUserid == null ? "" : externalUserid.trim();
		if (!StringUtils.hasText(ext)) {
			throw new ResourceException("企业微信接口调用失败");
		}
		String body =
				wxGet(cp, WxCpApiPathConsts.ExternalContact.GET_CONTACT_DETAIL, ext, "externalcontact/get", false);
		return parseUserPayload(body, "externalcontact/get");
	}

	public List<String> listExternalUserIds(long companyId, String followUserid) {
		WxCpService cp = cpBearer(companyId);
		String uid = followUserid == null ? "" : followUserid.trim();
		if (!StringUtils.hasText(uid)) {
			throw new ResourceException("企业微信接口调用失败");
		}
		try {
			List<String> ids = cp.getExternalContactService().listExternalContacts(uid);
			return ids == null ? List.of() : ids;
		} catch (WxErrorException e) {
			log.warn(
					"work wechat externalcontact/list wx err errcode={}",
					e.getError() != null ? e.getError().getErrorCode() : null);
			throw new ResourceException("企业微信接口调用失败");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("work wechat externalcontact/list request failed", e);
			throw new ResourceException("企业微信接口调用失败");
		}
	}

	public List<Map<String, Object>> getDetailedDepartmentUsers(long companyId, long departmentId) {
		WxCpService cp = cpBearer(companyId);
		String q = departmentId + "&fetch_child=0";
		String body =
				wxGet(cp, WxCpApiPathConsts.User.USER_LIST, q, "user/list", false);
		JsonNode root = readJsonRoot(body, "user/list");
		assertWechatOk(root, "user/list");
		JsonNode listNode = root.get("userlist");
		if (listNode == null || !listNode.isArray()) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (JsonNode n : listNode) {
			if (n == null || !n.isObject()) {
				continue;
			}
			Map<String, Object> m = objectMapper.convertValue(n, new TypeReference<>() {});
			out.add(m);
		}
		return out;
	}

	public List<Map<String, Object>> listDepartments(long companyId) {
		WxCpService cp = cpBearer(companyId);
		String body = wxGet(cp, WxCpApiPathConsts.Department.DEPARTMENT_LIST, "", "department/list", false);
		JsonNode root = readJsonRoot(body, "department/list");
		assertWechatOk(root, "department/list");
		JsonNode deptNode = root.get("department");
		if (deptNode == null || !deptNode.isArray()) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (JsonNode n : deptNode) {
			if (n == null || !n.isObject()) {
				continue;
			}
			Map<String, Object> m = objectMapper.convertValue(n, new TypeReference<>() {});
			out.add(m);
		}
		return out;
	}

	private WxCpService cpBearer(long companyId) {
		String token =
				workWechatAccessTokenProvider
						.getAccessToken(companyId)
						.filter(StringUtils::hasText)
						.orElseThrow(() -> new ResourceException("企业微信 access_token 不可用，请检查配置"));
		return workWechatWxCpRuntime.cpBearer(token);
	}

	private String wxGet(
			WxCpService cp,
			String path,
			String suffixQuery,
			String apiLabel,
			boolean operatorWorkWechatOauth) {
		try {
			return cp.get(path, suffixQuery == null ? "" : suffixQuery);
		} catch (WxErrorException e) {
			int code = e.getError() != null ? e.getError().getErrorCode() : -1;
			String msg = e.getError() != null ? e.getError().getErrorMsg() : e.getMessage();
			log.warn("work wechat {} wx err errcode={} errmsg={}", apiLabel, code, msg);
			if (operatorWorkWechatOauth && "user/get".equals(apiLabel)) {
				throw new ResourceException("该账号不在店务应用可见范围内");
			}
			if ("user/get".equals(apiLabel)) {
				throw new ResourceException("企业微信错误: errcode=" + code + ", errmsg=" + msg);
			}
			throw new ResourceException("企业微信接口调用失败");
		} catch (ResourceException ex) {
			throw ex;
		} catch (Exception e) {
			log.warn("work wechat {} request failed", apiLabel, e);
			throw new ResourceException("企业微信接口调用失败");
		}
	}

	private JsonNode readJsonRoot(String body, String apiLabel) {
		try {
			return objectMapper.readTree(body);
		} catch (Exception e) {
			log.warn("work wechat {} json parse failed body={}", apiLabel, body, e);
			throw new ResourceException("企业微信接口调用失败");
		}
	}

	private void assertWechatOk(JsonNode root, String apiLabel) {
		if (root == null) {
			throw new ResourceException("企业微信接口调用失败");
		}
		int errcode = root.path("errcode").asInt(0);
		if (errcode != 0) {
			String errmsg = root.path("errmsg").asText("");
			log.warn("work wechat {} errcode={} errmsg={}", apiLabel, errcode, errmsg);
			throw new ResourceException("企业微信错误: errcode=" + errcode + ", errmsg=" + errmsg);
		}
	}

	private Map<String, Object> parseUserPayload(String body, String apiLabel) {
		JsonNode root = readJsonRoot(body, apiLabel);
		assertWechatOk(root, apiLabel);
		if (!root.isObject()) {
			throw new ResourceException("企业微信接口调用失败");
		}
		Map<String, Object> m = objectMapper.convertValue(root, new TypeReference<>() {});
		return m == null ? new LinkedHashMap<>() : m;
	}
}
