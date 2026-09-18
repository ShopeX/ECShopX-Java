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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.admin.AdminTrustLoginListService;
import cn.shopex.ecshopx.members.service.email.MemberSyntheticMobileService;
import cn.shopex.ecshopx.members.service.trustlogin.SocialOAuthUser;
import cn.shopex.ecshopx.members.service.trustlogin.SocialTrustLoginService;
import cn.shopex.ecshopx.members.service.trustlogin.TrustLoginConfigSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** L1 social_oauth 登录 / 自动建会（对齐 PHP EspierLocalUserProvider::preSocialOAuthLogin）。 */
@Service
public class H5SocialOAuthLoginService {

	private final SocialTrustLoginService socialTrustLoginService;
	private final AdminTrustLoginListService adminTrustLoginListService;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final MembersMapper membersMapper;
	private final MemberSyntheticMobileService memberSyntheticMobileService;
	private final MemberAccountService memberAccountService;

	public H5SocialOAuthLoginService(
			SocialTrustLoginService socialTrustLoginService,
			AdminTrustLoginListService adminTrustLoginListService,
			MembersAssociationsMapper membersAssociationsMapper,
			MembersMapper membersMapper,
			MemberSyntheticMobileService memberSyntheticMobileService,
			MemberAccountService memberAccountService) {
		this.socialTrustLoginService = socialTrustLoginService;
		this.adminTrustLoginListService = adminTrustLoginListService;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.membersMapper = membersMapper;
		this.memberSyntheticMobileService = memberSyntheticMobileService;
		this.memberAccountService = memberAccountService;
	}

	@Transactional(rollbackFor = Exception.class)
	public H5GenericUser loginOrRegister(Map<String, Object> credentials) {
		long companyId = toLong(credentials.get("company_id"));
		String trustloginTag = stringVal(credentials.get("trustlogin_tag"));
		String code = stringVal(credentials.get("code"));
		String versionTag = stringVal(credentials.get("version_tag"));
		if (!StringUtils.hasText(versionTag)) {
			versionTag = "touch";
		}
		if (companyId <= 0L || !StringUtils.hasText(trustloginTag) || !StringUtils.hasText(code)) {
			throw new ResourceException("缺少参数！");
		}
		if (!socialTrustLoginService.isSocialProvider(trustloginTag)) {
			throw new ResourceException("不支持的第三方登录方式");
		}
		Map<String, Object> configRow =
				adminTrustLoginListService.getConfigRow(trustloginTag, versionTag, companyId);
		if (configRow.isEmpty() || !TrustLoginConfigSupport.isEnabled(configRow.get("status"))) {
			throw new ResourceException("该登录方式未开启");
		}
		String h5Host = socialTrustLoginService.resolveH5Host(stringVal(credentials.get("origin")));
		if (!StringUtils.hasText(h5Host)) {
			throw new ResourceException("缺少 H5 域名配置");
		}
		String redirectUri = socialTrustLoginService.buildRedirectUri(h5Host, trustloginTag);
		SocialOAuthUser socialUser =
				socialTrustLoginService.resolveUserFromCode(trustloginTag, configRow, code, redirectUri);
		String unionid = socialUser.unionid();
		String userType = socialUser.userType();

		MembersAssociations assoc =
				membersAssociationsMapper.selectOne(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.eq(MembersAssociations::getUserType, userType)
								.eq(MembersAssociations::getUnionid, unionid)
								.last("LIMIT 1"));

		Map<String, Object> attrs = new HashMap<>();
		attrs.put("company_id", companyId);
		attrs.put("unionid", unionid);
		attrs.put("user_type", userType);
		attrs.put("trustlogin_tag", trustloginTag);
		attrs.put("openid", trustloginTag);
		attrs.put("operator_type", "user");
		attrs.put("is_new", 0);

		if (assoc != null && assoc.getUserId() != null && assoc.getUserId() > 0L) {
			Members member =
					membersMapper.selectOne(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getUserId, assoc.getUserId())
									.eq(Members::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (member == null || member.getUserId() == null) {
				throw new ResourceException("第三方授权信息无效");
			}
			fillMemberAttrs(attrs, member, companyId);
			return new H5GenericUser(attrs);
		}

		String mobile = memberSyntheticMobileService.allocateUnique(companyId);
		String nickname = stringVal(socialUser.nickname());
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("mobile", mobile);
		postData.put("region_mobile", mobile);
		postData.put("mobile_country_code", "86");
		postData.put("company_id", companyId);
		postData.put("wxa_appid", "");
		postData.put("authorizer_appid", "");
		postData.put("sex", 0);
		postData.put("username", StringUtils.hasText(nickname) ? nickname : randomUsername(8));
		postData.put("avatar", stringVal(socialUser.avatar()));
		postData.put("email", stringVal(socialUser.email()));
		postData.put("api_from", "h5app");
		postData.put("auth_type", "social_oauth");
		postData.put("user_type", userType);
		postData.put("unionid", unionid);
		postData.put("open_id", unionid);
		postData.put("force_password", 0);

		Map<String, Object> created = memberAccountService.creatMemberForH5Post(postData, false);
		long userId = toLong(created.get("user_id"));
		Members member =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getUserId, userId)
								.eq(Members::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (member == null) {
			throw new ResourceException("第三方授权信息无效");
		}
		fillMemberAttrs(attrs, member, companyId);
		attrs.put("is_new", 0);
		return new H5GenericUser(attrs);
	}

	private static void fillMemberAttrs(Map<String, Object> attrs, Members member, long companyId) {
		long userId = member.getUserId();
		attrs.put("id", userId + "_espier_companyid_espier_" + companyId);
		attrs.put("user_id", userId);
		attrs.put("disabled", Boolean.TRUE.equals(member.getDisabled()) ? 1 : 0);
		attrs.put("mobile", member.getRegionMobile() != null ? member.getRegionMobile() : "");
		attrs.put("user_card_code", member.getUserCardCode() != null ? member.getUserCardCode() : "");
		attrs.put("offline_card_code", member.getOfflineCardCode() != null ? member.getOfflineCardCode() : "");
	}

	private static String randomUsername(int len) {
		String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			sb.append(chars.charAt((int) (Math.random() * chars.length())));
		}
		return sb.toString();
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
