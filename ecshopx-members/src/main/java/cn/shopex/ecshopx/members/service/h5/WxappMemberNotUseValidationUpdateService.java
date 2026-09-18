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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WxappMemberNotUseValidationUpdateService {

	private final MemberAccountService memberAccountService;

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateMemberNotUseValidationConfig(
			long companyId,
			long userId,
			Map<String, Object> authClaims,
			Map<String, Object> requestOnlyUsernameAvatar) {
		Map<String, Object> post =
				requestOnlyUsernameAvatar == null ? new LinkedHashMap<>() : requestOnlyUsernameAvatar;
		memberAccountService.memberInfoUpdate(userId, companyId, post);

		Map<String, Object> claims = authClaims != null ? authClaims : Collections.emptyMap();
		String unionid = stringVal(claims.get("unionid"));
		String openId = stringVal(claims.get("open_id"));
		if (StringUtils.hasText(unionid) && StringUtils.hasText(openId)) {
			LinkedHashMap<String, Object> wechatPatch = new LinkedHashMap<>();
			if (post.containsKey("avatar")) {
				wechatPatch.put("headimgurl", post.get("avatar"));
			}
			if (post.containsKey("username")) {
				wechatPatch.put("nickname", post.get("username"));
			}
			memberAccountService.updateWechatUserProfileByOpenUnion(
					companyId, claims, wechatPatch, "", "", "", "");
		}

		Map<String, Object> result = memberAccountService.getMemberInfo(userId, companyId);
		normalizeEmptyOtherParamsForMemberinfoResponse(result);
		return result;
	}

	/**
	 * Stored {@code other_params} JSON {@code []} is surfaced as an empty JSON array on this route.
	 * The shared {@code getMemberInfo} mapper expands that storage form into a default object; collapse
	 * that default-empty object (or an already-empty list) here so other callers stay unchanged.
	 */
	private static void normalizeEmptyOtherParamsForMemberinfoResponse(Map<String, Object> member) {
		if (member == null || member.isEmpty()) {
			return;
		}
		if (!isEmptyOtherParamsEchoShape(member.get("other_params"))) {
			return;
		}
		member.put("other_params", new ArrayList<Object>());
	}

	private static boolean isEmptyOtherParamsEchoShape(Object op) {
		if (op instanceof Collection<?> list) {
			return list.isEmpty();
		}
		if (!(op instanceof Map<?, ?> m)) {
			return false;
		}
		Object customData = m.get("custom_data");
		boolean customEmpty =
				customData == null
						|| (customData instanceof Collection<?> c && c.isEmpty())
						|| (customData instanceof Map<?, ?> cm && cm.isEmpty());
		if (!customEmpty) {
			return false;
		}
		Object wx = m.get("isGetWxInfo");
		boolean wxUnsetOrFalse =
				wx == null
						|| Boolean.FALSE.equals(wx)
						|| "false".equalsIgnoreCase(String.valueOf(wx).trim());
		if (!wxUnsetOrFalse) {
			return false;
		}
		for (Object k : m.keySet()) {
			if (k == null) {
				return false;
			}
			String ks = String.valueOf(k);
			if (!"custom_data".equals(ks) && !"isGetWxInfo".equals(ks)) {
				return false;
			}
		}
		return true;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : Objects.toString(o, "").trim();
	}
}
