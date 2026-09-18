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

import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.WechatAuthorizerLookupMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Rehydrates H5 JWT claim maps with WeChat / member fields the same way PHP
 * {@code EspierLocalUserProvider::getUserLoginInfo} does on each request.
 */
@Service
public class H5JwtUserLoginInfoEnrichService {

	private final MemberAccountService memberAccountService;
	private final WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper;
	private final MembersAssociationsMapper membersAssociationsMapper;

	public H5JwtUserLoginInfoEnrichService(
			MemberAccountService memberAccountService,
			WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper,
			MembersAssociationsMapper membersAssociationsMapper) {
		this.memberAccountService = memberAccountService;
		this.wechatAuthorizerLookupMapper = wechatAuthorizerLookupMapper;
		this.membersAssociationsMapper = membersAssociationsMapper;
	}

	/**
	 * Mutates {@code claims} in place: fills {@code open_id}, {@code wxapp_appid}, {@code woa_appid}
	 * and common member fields from DB when the JWT subject is an {@code _espier_} identifier.
	 */
	public void enrichClaims(Map<String, Object> claims) {
		if (claims == null || claims.isEmpty()) {
			return;
		}
		copyClaimIfAbsent(claims, "open_id", "openid");

		EspierSubject subject = parseEspierSubject(str(claims.get("sub")));
		if (subject == null) {
			subject = parseEspierSubject(str(claims.get("id")));
		}
		if (subject == null) {
			return;
		}

		long companyId = 0L;
		String woaAppid = "";
		String openId = "";
		String unionid = "";
		String wxappAppid = "";
		String nickname = "";
		String headimgurl = "";
		String alipayUserId = "";

		if (StringUtils.hasText(subject.openIdPart)
				&& StringUtils.hasText(subject.unionidPart)
				&& !"companyid".equals(subject.openIdPart)) {
			if ("alipay".equals(subject.openIdPart)) {
				MembersAssociations alipayUser =
						membersAssociationsMapper.selectOne(
								new LambdaQueryWrapper<MembersAssociations>()
										.eq(MembersAssociations::getUserId, subject.userId)
										.eq(MembersAssociations::getUserType, "ali")
										.eq(MembersAssociations::getUnionid, subject.unionidPart)
										.last("LIMIT 1"));
				if (alipayUser != null) {
					companyId = alipayUser.getCompanyId() == null ? 0L : alipayUser.getCompanyId();
					alipayUserId = str(alipayUser.getUnionid());
				}
			} else {
				Map<String, Object> filter = new LinkedHashMap<>();
				filter.put("unionid", subject.unionidPart);
				filter.put("open_id", subject.openIdPart);
				Map<String, Object> user = memberAccountService.getWechatUserInfo(filter);
				if (!user.isEmpty()) {
					openId = str(user.get("open_id"));
					unionid = str(user.get("unionid"));
					wxappAppid = str(user.get("authorizer_appid"));
					nickname = str(user.get("nickname"));
					headimgurl = str(user.get("headimgurl"));
					if (StringUtils.hasText(wxappAppid)) {
						Long cid = wechatAuthorizerLookupMapper.selectCompanyIdByAuthorizerAppid(wxappAppid);
						if (cid != null && cid > 0L) {
							companyId = cid;
							String woa =
									wechatAuthorizerLookupMapper.selectServiceAccountAppidByCompanyId(cid);
							if (StringUtils.hasText(woa)) {
								woaAppid = woa.trim();
							}
						}
					}
				}
			}
		} else if (StringUtils.hasText(subject.unionidPart)) {
			try {
				companyId = Long.parseLong(subject.unionidPart.trim());
			} catch (NumberFormatException ignored) {
				companyId = 0L;
			}
		}

		if (companyId <= 0L) {
			companyId = parseLong(claims.get("company_id"));
		}
		if (companyId <= 0L || subject.userId <= 0L) {
			putIfHasText(claims, "open_id", openId);
			putIfHasText(claims, "wxapp_appid", wxappAppid);
			putIfHasText(claims, "woa_appid", woaAppid);
			return;
		}

		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(subject.userId, companyId);
		if (memberInfo.isEmpty()) {
			putIfHasText(claims, "open_id", openId);
			putIfHasText(claims, "wxapp_appid", wxappAppid);
			putIfHasText(claims, "woa_appid", woaAppid);
			return;
		}

		claims.put("user_id", memberInfo.get("user_id") != null ? memberInfo.get("user_id") : subject.userId);
		claims.put("company_id", memberInfo.get("company_id") != null ? memberInfo.get("company_id") : companyId);
		claims.put("disabled", memberInfo.getOrDefault("disabled", 0));
		claims.put("wxapp_appid", wxappAppid);
		claims.put("woa_appid", woaAppid);
		claims.put("open_id", openId);
		claims.put("unionid", unionid);
		claims.put("nickname", nickname);
		claims.put("headimgurl", headimgurl);
		claims.put("grade_id", memberInfo.get("grade_id"));
		claims.put("mobile", memberInfo.get("mobile"));
		claims.put("username", memberInfo.getOrDefault("username", ""));
		claims.put("user_card_code", memberInfo.get("user_card_code"));
		claims.put("offline_card_code", memberInfo.get("offline_card_code"));
		claims.put("operator_type", "user");
		claims.put("inviter_id", memberInfo.get("inviter_id"));
		claims.put("source_id", memberInfo.get("source_id"));
		claims.put("monitor_id", memberInfo.get("monitor_id"));
		claims.put("latest_source_id", memberInfo.get("latest_source_id"));
		claims.put("latest_monitor_id", memberInfo.get("latest_monitor_id"));
		claims.put("alipay_appid", memberInfo.getOrDefault("alipay_appid", ""));
		if (StringUtils.hasText(alipayUserId)) {
			claims.put("alipay_user_id", alipayUserId);
		}
	}

	/**
	 * Anonymous FrontNoAuth fallback: set {@code wxapp_appid} / {@code woa_appid} from
	 * {@code authorizer-appid} header (or {@code appid} query), matching PHP FrontNoAuth catch path.
	 */
	public Map<String, Object> buildAnonymousAuthClaims(
			long companyId, String appid, String openId, String unionid) {
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("api_from", "h5app");
		auth.put("company_id", companyId);
		auth.put("unionid", unionid == null ? "" : unionid);
		auth.put("open_id", openId == null ? "" : openId);
		auth.put("openid", openId == null ? "" : openId);
		auth.put("user_id", 0L);
		auth.put("wxapp_appid", "");
		auth.put("woa_appid", "");
		if (StringUtils.hasText(appid)) {
			String trimmed = appid.trim();
			if (trimmed.startsWith("wx")) {
				auth.put("wxapp_appid", trimmed);
				Long resolved = wechatAuthorizerLookupMapper.selectCompanyIdByAuthorizerAppid(trimmed);
				long cid = companyId;
				if (resolved != null && resolved > 0L) {
					cid = resolved;
					auth.put("company_id", cid);
				}
				String woa = wechatAuthorizerLookupMapper.selectServiceAccountAppidByCompanyId(cid);
				if (StringUtils.hasText(woa)) {
					auth.put("woa_appid", woa.trim());
				}
			} else {
				auth.put("alipay_appid", trimmed);
			}
		}
		return auth;
	}

	private static EspierSubject parseEspierSubject(String identifier) {
		if (!StringUtils.hasText(identifier) || !identifier.contains("_espier_")) {
			return null;
		}
		String[] parts = identifier.split("_espier_", 3);
		if (parts.length < 3) {
			return null;
		}
		long userId;
		try {
			userId = Long.parseLong(parts[0].trim());
		} catch (NumberFormatException e) {
			return null;
		}
		return new EspierSubject(userId, parts[1].trim(), parts[2].trim());
	}

	private static void copyClaimIfAbsent(Map<String, Object> claims, String target, String source) {
		if (StringUtils.hasText(str(claims.get(target)))) {
			return;
		}
		String v = str(claims.get(source));
		if (StringUtils.hasText(v)) {
			claims.put(target, v);
		}
	}

	private static void putIfHasText(Map<String, Object> claims, String key, String value) {
		if (StringUtils.hasText(value)) {
			claims.put(key, value);
		}
	}

	private static String str(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long parseLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private record EspierSubject(long userId, String openIdPart, String unionidPart) {}
}
