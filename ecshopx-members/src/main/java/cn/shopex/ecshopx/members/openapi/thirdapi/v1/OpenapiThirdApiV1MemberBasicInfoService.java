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

package cn.shopex.ecshopx.members.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.integration.kaquan.AdminMemberListVipGradeRowGetPort;
import cn.shopex.ecshopx.members.integration.kaquan.OpenapiMemberBasicInfoGradePort;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV1MemberBasicInfoService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");
	private static final Pattern INTEGER_STRING = Pattern.compile("-?(0|[1-9]\\d*)");

	private final MemberAccountService memberAccountService;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final OpenapiMemberBasicInfoGradePort openapiMemberBasicInfoGradePort;
	private final AdminMemberListVipGradeRowGetPort adminMemberListVipGradeRowGetPort;

	public OpenapiThirdApiV1MemberBasicInfoService(
			MemberAccountService memberAccountService,
			MembersAssociationsMapper membersAssociationsMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			OpenapiMemberBasicInfoGradePort openapiMemberBasicInfoGradePort,
			AdminMemberListVipGradeRowGetPort adminMemberListVipGradeRowGetPort) {
		this.memberAccountService = memberAccountService;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.openapiMemberBasicInfoGradePort = openapiMemberBasicInfoGradePort;
		this.adminMemberListVipGradeRowGetPort = adminMemberListVipGradeRowGetPort;
	}

	public Object executeOpenapiBasicInfo(
			long companyId,
			String mobileQueryParam,
			String unionidQueryParam,
			String externalMemberIdQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw,
			boolean mobileTruthy,
			boolean externalMemberIdPresent,
			String externalMemberIdRaw,
			boolean externalMemberIdTruthy,
			boolean unionidPresent,
			String unionidRaw) {
		if (body != null
				&& body.containsKey("unionid")
				&& body.get("unionid") != null
				&& !(body.get("unionid") instanceof String)) {
			throw new ResourceException("请填写unionid");
		}

		if (body != null
				&& body.containsKey("external_member_id")
				&& body.get("external_member_id") != null
				&& !(body.get("external_member_id") instanceof String)) {
			throw new ResourceException("会员id");
		}

		if (mobilePresent && (mobileRaw == null || !MOBILE_PATTERN.matcher(mobileRaw).matches())) {
			throw new ResourceException("请填写正确的手机号");
		}

		if (isPhpEmptyInline(mobileQueryParam, body, "mobile")
				&& isPhpEmptyInline(unionidQueryParam, body, "unionid")
				&& isPhpEmptyInline(externalMemberIdQueryParam, body, "external_member_id")) {
			throw new ResourceException("unionid或者手机号或会员id必填");
		}

		Map<String, Object> memberInfo;
		if (mobileTruthy || externalMemberIdTruthy) {
			memberInfo =
					memberAccountService.getMemberInfoForOpenapiFilter(
							companyId, mobileRaw, externalMemberIdRaw);
		} else {
			MembersAssociations assoc =
					membersAssociationsMapper.selectOne(
							new LambdaQueryWrapper<MembersAssociations>()
									.eq(MembersAssociations::getCompanyId, companyId)
									.eq(MembersAssociations::getUnionid, unionidRaw)
									.eq(MembersAssociations::getUserType, "wechat")
									.last("LIMIT 1"));
			if (assoc == null) {
				return Collections.emptyList();
			}
			memberInfo =
					memberAccountService.getMemberInfoForOpenapiFilter(
							companyId, null, String.valueOf(assoc.getUserId()));
		}

		if (memberInfo == null || memberInfo.isEmpty()) {
			return Collections.emptyList();
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("user_id", memberInfo.get("user_id"));
		data.put("mobile", decryptMembersMobileColumn(String.valueOf(memberInfo.get("mobile"))));
		if (memberInfo.containsKey("username")) {
			data.put(
					"username",
					sensitiveFieldEncryptor.decrypt(String.valueOf(memberInfo.get("username"))));
		} else {
			data.put("username", null);
		}
		data.put("birthday", memberInfo.get("birthday"));
		Object created = memberInfo.get("created");
		data.put("create_time", created != null ? created : "");

		long gradeId = resolveGradeIdLong(memberInfo.get("grade_id"));
		Map<String, Object> gradeInfo =
				gradeId > 0L
						? openapiMemberBasicInfoGradePort.getGradeByGradeIdForOpenapi(companyId, gradeId)
						: null;
		data.put("gradeInfo", gradeInfo);

		long userId = ((Number) memberInfo.get("user_id")).longValue();
		Map<String, Object> vipgrade = adminMemberListVipGradeRowGetPort.userVipGradeGet(companyId, userId, false);
		data.put("vipgrade", vipgrade);

		return data;
	}

	private static String decryptMembersMobileColumn(String storedMobile) {
		if (!StringUtils.hasText(storedMobile)) {
			return "";
		}
		String plain = LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(storedMobile);
		return plain != null ? plain : "";
	}

	private static boolean isPhpEmptyInline(String queryParam, Map<String, Object> body, String key) {
		Object raw = null;
		if (body != null && body.containsKey(key)) {
			raw = body.get(key);
		} else if (queryParam != null) {
			raw = queryParam;
		}
		if (raw == null) {
			return true;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		return false;
	}

	private static long resolveGradeIdLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return 0L;
			}
			if (!INTEGER_STRING.matcher(t).matches()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		if (raw instanceof Number n) {
			if (n.longValue() == 0L) {
				return 0L;
			}
			return n.longValue();
		}
		return 0L;
	}
}
