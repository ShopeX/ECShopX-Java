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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Resolves {@code member_info} headimgurl/nickname for拼团团员，与 PHP {@code PromotionGroupsTeamMemberService::createGroupsTeamMember}
 * 一致：优先微信资料；微信昵称为占位符时回退会员姓名/用户名（PHP 列表接口对空昵称亦有 username 回退）。
 */
@Service
public class PromotionGroupsTeamMemberInfoResolver {

	public record Profile(String headimgurl, String nickname) {}

	private static final String DEFAULT_NICKNAME = "用户";

	private final MemberAccountService memberAccountService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public PromotionGroupsTeamMemberInfoResolver(
			MemberAccountService memberAccountService, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.memberAccountService = memberAccountService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Profile resolve(long companyId, long userId) {
		Map<String, Object> wxFilter = new LinkedHashMap<>();
		wxFilter.put("user_id", userId);
		wxFilter.put("company_id", companyId);
		Map<String, Object> wxUser = memberAccountService.getWechatUserInfo(wxFilter);

		String wxAvatar = "";
		String wxNickname = "";
		if (wxUser != null && !wxUser.isEmpty()) {
			wxAvatar = stringVal(wxUser.get("headimgurl"));
			wxNickname = stringVal(wxUser.get("nickname"));
		}

		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		String memberAvatar = stringVal(memberInfo.get("avatar"));
		if (memberAvatar.isEmpty()) {
			memberAvatar = stringVal(memberInfo.get("headimgurl"));
		}
		String memberName = stringVal(memberInfo.get("name"));
		String memberUsername = decryptUsername(stringVal(memberInfo.get("username")));

		String nickname = pickNickname(wxNickname, memberName, memberUsername);
		String headimgurl = pickHeadimgurl(wxAvatar, memberAvatar);
		return new Profile(headimgurl, nickname);
	}

	public static boolean isUsableDisplayNickname(String nickname) {
		if (!StringUtils.hasText(nickname)) {
			return false;
		}
		String t = nickname.trim();
		return !"-".equals(t) && !"微信用户".equals(t);
	}

	private static String pickNickname(String wxNickname, String memberName, String memberUsername) {
		if (isUsableDisplayNickname(wxNickname)) {
			return wxNickname.trim();
		}
		if (isUsableDisplayNickname(memberName)) {
			return memberName.trim();
		}
		if (isUsableDisplayNickname(memberUsername)) {
			return memberUsername.trim();
		}
		return DEFAULT_NICKNAME;
	}

	private static String pickHeadimgurl(String wxAvatar, String memberAvatar) {
		if (StringUtils.hasText(wxAvatar)) {
			return wxAvatar.trim();
		}
		return memberAvatar == null ? "" : memberAvatar.trim();
	}

	private String decryptUsername(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		String decrypted = sensitiveFieldEncryptor.decrypt(raw.trim());
		return decrypted == null ? "" : decrypted.trim();
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
