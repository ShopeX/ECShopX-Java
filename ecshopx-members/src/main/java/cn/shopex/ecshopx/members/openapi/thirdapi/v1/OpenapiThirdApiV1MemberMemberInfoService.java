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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.admin.MemberUnionidByUserIdLookupPort;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV1MemberMemberInfoService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MemberAccountService memberAccountService;
	private final MemberUnionidByUserIdLookupPort memberUnionidByUserIdLookupPort;
	private final MembersAssociationsMapper membersAssociationsMapper;

	public OpenapiThirdApiV1MemberMemberInfoService(
			MemberAccountService memberAccountService,
			MemberUnionidByUserIdLookupPort memberUnionidByUserIdLookupPort,
			MembersAssociationsMapper membersAssociationsMapper) {
		this.memberAccountService = memberAccountService;
		this.memberUnionidByUserIdLookupPort = memberUnionidByUserIdLookupPort;
		this.membersAssociationsMapper = membersAssociationsMapper;
	}

	public Map<String, Object> executeOpenapiMemberInfo(
			long companyId,
			String mobileQueryParam,
			String unionidQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw,
			boolean mobileTruthy,
			boolean unionidPresent,
			String unionidRaw,
			boolean unionidTruthy) {
		if (body != null
				&& body.containsKey("unionid")
				&& body.get("unionid") != null
				&& !(body.get("unionid") instanceof String)) {
			throw new ResourceException("请填写unionid");
		}

		if (mobilePresent && (mobileRaw == null || !MOBILE_PATTERN.matcher(mobileRaw).matches())) {
			throw new ResourceException("请填写正确的手机号");
		}

		if (isPhpEmptyInline(mobileQueryParam, body, "mobile")
				&& isPhpEmptyInline(unionidQueryParam, body, "unionid")) {
			throw new ResourceException("unionid或者手机号必填");
		}

		if (mobileTruthy) {
			return executeMobileBranch(companyId, mobileRaw);
		}
		if (unionidTruthy) {
			return executeUnionidBranch(companyId, unionidRaw);
		}
		return defaultNotMemberReturn();
	}

	private Map<String, Object> executeMobileBranch(long companyId, String mobileRaw) {
		Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobileRaw);
		if (member == null) {
			return defaultNotMemberReturn();
		}
		Optional<String> unionOpt =
				memberUnionidByUserIdLookupPort.findUnionidByUserId(companyId, member.getUserId());
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("is_member", "Y");
		data.put("mobile", decryptMembersMobileColumn(member.getMobile()));
		data.put("unionid", unionOpt.orElse(null) != null ? unionOpt.get() : Boolean.FALSE);
		data.put("uid", member.getUserId());
		return data;
	}

	private Map<String, Object> executeUnionidBranch(long companyId, String unionidRaw) {
		MembersAssociations assoc =
				membersAssociationsMapper.selectOne(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.eq(MembersAssociations::getUnionid, unionidRaw)
								.eq(MembersAssociations::getUserType, "wechat")
								.last("LIMIT 1"));
		if (assoc == null) {
			return defaultNotMemberReturn();
		}
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(assoc.getUserId(), companyId);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("is_member", "Y");
		data.put("mobile", resolveMobileForUnionidBranch(memberInfo));
		data.put("unionid", assoc.getUnionid());
		data.put("uid", assoc.getUserId());
		return data;
	}

	private static LinkedHashMap<String, Object> defaultNotMemberReturn() {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("is_member", "N");
		data.put("mobile", "");
		data.put("unionid", "");
		data.put("uid", "");
		return data;
	}

	private static String decryptMembersMobileColumn(String storedMobile) {
		if (!StringUtils.hasText(storedMobile)) {
			return "";
		}
		String plain = LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(storedMobile);
		return plain != null ? plain : "";
	}

	private static Object resolveMobileForUnionidBranch(Map<String, Object> memberInfo) {
		if (memberInfo == null || memberInfo.isEmpty()) {
			return null;
		}
		Object reg = memberInfo.get("region_mobile");
		if (reg != null && StringUtils.hasText(reg.toString().trim())) {
			return reg.toString().trim();
		}
		Object m = memberInfo.get("mobile");
		if (m != null && StringUtils.hasText(m.toString().trim())) {
			return decryptMembersMobileColumn(m.toString().trim());
		}
		return null;
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
}
