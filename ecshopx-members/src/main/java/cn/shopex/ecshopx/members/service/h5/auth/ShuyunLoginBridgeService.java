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

package cn.shopex.ecshopx.members.service.h5.auth;

import cn.shopex.ecshopx.members.config.H5LocalProperties;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ShuyunLoginBridgeService {

	private final H5LocalProperties h5LocalProperties;

	private final RestClient restClient;

	private final MemberAccountService memberAccountService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public ShuyunLoginBridgeService(
			H5LocalProperties h5LocalProperties,
			@Qualifier("h5LoginRestClient") RestClient restClient,
			MemberAccountService memberAccountService) {
		this.h5LocalProperties = h5LocalProperties;
		this.restClient = restClient;
		this.memberAccountService = memberAccountService;
	}

	public Optional<Map<String, Object>> shuyunMemberSilent(Map<String, Object> inputData, Map<String, Object> params) {
		Object sy = inputData.get("shuyunappid");
		if (sy == null || !StringUtils.hasText(String.valueOf(sy))) {
			return Optional.empty();
		}
		String shuyunAppId = String.valueOf(sy).trim();
		String unionid = stringVal(params.get("unionid"));
		String openId = stringVal(params.get("open_id"));
		Object companyObj = params.get("company_id");
		if (companyObj == null) {
			return Optional.empty();
		}
		long companyId = toLong(companyObj);

		String mobile = silentSearchMobile(companyId, shuyunAppId, unionid);
		if (!StringUtils.hasText(mobile)) {
			return Optional.empty();
		}

		Map<String, Object> wxParams = new HashMap<>();
		wxParams.put("company_id", companyId);
		wxParams.put("open_id", openId);
		wxParams.put("unionid", unionid);
		wxParams.put("source_id", numberOrZero(inputData.get("source_id")));
		wxParams.put("monitor_id", numberOrZero(inputData.get("monitor_id")));
		wxParams.put("inviter_id", numberOrZero(inputData.get("inviter_id")));
		wxParams.put("source_from", stringOrDefault(inputData.get("source_from"), "default"));
		String appid = stringVal(inputData.get("appid"));
		memberAccountService.createWxappFans(appid, wxParams);

		Map<String, Object> member = memberAccountService.getInfoByMobile(companyId, mobile);
		if (member == null || member.isEmpty()) {
			Map<String, Object> reg = new HashMap<>(inputData);
			reg.put("open_id", openId);
			reg.put("unionid", unionid);
			reg.put("mobile", mobile);
			reg.put("company_id", companyId);
			reg.put("user_type", "wechat");
			reg.put("api_from", "wechat");
			member = memberAccountService.registerShuyunMember(reg);
		} else {
			Map<String, Object> assoc = memberAccountService.getMembersAssociationByUserId(companyId, "wechat", toLong(member.get("user_id")));
			if (assoc == null || assoc.isEmpty()) {
				Map<String, Object> reg = new HashMap<>(inputData);
				reg.put("open_id", openId);
				reg.put("unionid", unionid);
				reg.put("mobile", mobile);
				reg.put("company_id", companyId);
				reg.put("user_type", "wechat");
				reg.put("api_from", "wechat");
				member = memberAccountService.registerShuyunMember(reg);
			} else if (!unionid.equals(stringVal(assoc.get("unionid")))) {
				return Optional.empty();
			}
		}
		if (member == null || member.get("user_id") == null) {
			return Optional.empty();
		}
		Map<String, Object> out = new HashMap<>();
		out.put("user_id", toLong(member.get("user_id")));
		out.put("open_id", openId);
		out.put("unionid", unionid);
		return Optional.of(out);
	}

	private String silentSearchMobile(long companyId, String shuyunAppId, String unionid) {
		String base = h5LocalProperties.getShuyunSilentSearchBaseUrl();
		if (!StringUtils.hasText(base)) {
			return null;
		}
		URI uri = UriComponentsBuilder.fromUriString(base.trim())
				.path("/lpee-interfaces-service/v1/spmall/member/silent/search")
				.queryParam("appId", shuyunAppId)
				.queryParam("unionid", unionid)
				.build(true)
				.toUri();
		try {
			String body = restClient.get().uri(uri).retrieve().body(String.class);
			JsonNode root = objectMapper.readTree(body);
			if (root.has("code") && root.get("code").asInt() != 0) {
				return null;
			}
			JsonNode data = root.get("data");
			if (data != null && data.has("mobile")) {
				return data.get("mobile").asText();
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static String stringOrDefault(Object o, String def) {
		String s = stringVal(o);
		return StringUtils.hasText(s) ? s : def;
	}

	private static int numberOrZero(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}
}
