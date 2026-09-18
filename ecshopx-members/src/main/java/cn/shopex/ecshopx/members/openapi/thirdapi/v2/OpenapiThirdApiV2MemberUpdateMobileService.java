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

package cn.shopex.ecshopx.members.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.members.domain.MemberOperateLog;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberOperateLogMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberUpdateMobileService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MembersMapper membersMapper;
	private final MemberOperateLogMapper memberOperateLogMapper;
	private final MemberAccountService memberAccountService;

	public OpenapiThirdApiV2MemberUpdateMobileService(
			MembersMapper membersMapper,
			MemberOperateLogMapper memberOperateLogMapper,
			MemberAccountService memberAccountService) {
		this.membersMapper = membersMapper;
		this.memberOperateLogMapper = memberOperateLogMapper;
		this.memberAccountService = memberAccountService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void executeOpenapiUpdateMobile(long companyId, Map<String, Object> mergedRaw) {
		try {
			String mobilePlain = validateMobile(mergedRaw);
			String newMobilePlain = validateNewMobile(mergedRaw);

			if (companyId <= 0L) {
				throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "缺少必要参数");
			}

			Members oldMember = memberAccountService.findMemberByCompanyAndMobile(companyId, mobilePlain);
			if (oldMember == null || oldMember.getUserId() == null) {
				throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
			}

			Members newMobileOccupant =
					memberAccountService.findMemberByCompanyAndMobile(companyId, newMobilePlain);
			if (newMobileOccupant != null
					&& newMobileOccupant.getUserId() != null
					&& !newMobileOccupant.getUserId().equals(oldMember.getUserId())) {
				throw v2Fail(OpenapiErrorCode.MEMBER_EXIST, "会员新手机号已存在");
			}

			long nowSec = System.currentTimeMillis() / 1000L;
			String newStored = LegacyFixedMobileEncrypt.fixedEncryptMobile(newMobilePlain);

			LambdaUpdateWrapper<Members> uw = new LambdaUpdateWrapper<>();
			uw.eq(Members::getCompanyId, companyId).eq(Members::getUserId, oldMember.getUserId());
			uw.set(Members::getMobile, newStored);
			uw.set(Members::getUpdated, nowSec);
			int affected = membersMapper.update(null, uw);
			if (affected == 0) {
				throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
			}

			saveOperateMobileLog(companyId, oldMember.getUserId(), mobilePlain, newMobilePlain);
		} catch (OpenapiMemberV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw v2Fail(OpenapiErrorCode.SERVICE_ERROR, "系统错误");
		}
	}

	private String validateMobile(Map<String, Object> mergedRaw) {
		Map<String, Object> in = mergedRaw == null ? Map.of() : mergedRaw;
		String mobile = stringValue(in.get("mobile"));
		if (!StringUtils.hasText(mobile) || !MOBILE_PATTERN.matcher(mobile.trim()).matches()) {
			throw v2Fail(OpenapiErrorCode.MEMBER_EXIST, "会员手机号已存在");
		}
		return mobile.trim();
	}

	private String validateNewMobile(Map<String, Object> mergedRaw) {
		Map<String, Object> in = mergedRaw == null ? Map.of() : mergedRaw;
		Object raw = in.get("new_mobile");
		if (raw == null) {
			throw missingParams("会员新手机号参数错误");
		}
		if (raw instanceof String s && !StringUtils.hasText(s.trim())) {
			throw missingParams("会员新手机号参数错误");
		}

		String newMobilePlain = normalizeNewMobilePlain(raw);
		if (newMobilePlain == null) {
			throw missingParams("会员新手机号参数错误");
		}
		if (!MOBILE_PATTERN.matcher(newMobilePlain).matches()) {
			throw missingParams("新手机号填写错误");
		}
		return newMobilePlain;
	}

	private void saveOperateMobileLog(
			long companyId, long userId, String oldMobilePlain, String newMobilePlain) {
		if (Objects.equals(oldMobilePlain, newMobilePlain)) {
			return;
		}
		long nowSec = System.currentTimeMillis() / 1000L;
		MemberOperateLog log = new MemberOperateLog();
		log.setCompanyId(companyId);
		log.setUserId(userId);
		log.setOperateType("mobile");
		log.setOldData(oldMobilePlain);
		log.setNewData(newMobilePlain);
		log.setOperater("外部开发者");
		log.setRemarks("");
		log.setCreated(nowSec);
		log.setUpdated(nowSec);
		memberOperateLogMapper.insert(log);
	}

	private static String normalizeNewMobilePlain(Object rawNew) {
		if (rawNew == null) {
			return null;
		}
		if (rawNew instanceof Boolean b) {
			if (!b) {
				return null;
			}
			String t = String.valueOf(rawNew).trim();
			if (t.isEmpty() || "0".equals(t)) {
				return null;
			}
			return t;
		}
		if (rawNew instanceof Number num) {
			if (num.doubleValue() == 0.0) {
				return null;
			}
			boolean useLongString =
					rawNew instanceof Long
							|| rawNew instanceof Integer
							|| rawNew instanceof Short
							|| rawNew instanceof Byte;
			if (!useLongString) {
				double d = num.doubleValue();
				if (d == Math.rint(d) && Math.abs(d) <= (double) Long.MAX_VALUE) {
					useLongString = true;
				}
			}
			if (useLongString) {
				String plain = String.valueOf(num.longValue());
				if ("0".equals(plain)) {
					return null;
				}
				return plain;
			}
			String t = String.valueOf(rawNew).trim();
			if (t.isEmpty() || "0".equals(t)) {
				return null;
			}
			return t;
		}
		if (rawNew instanceof String str) {
			String t = str.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return null;
			}
			return t;
		}
		String t = String.valueOf(rawNew).trim();
		if (t.isEmpty() || "0".equals(t)) {
			return null;
		}
		return t;
	}

	private static String stringValue(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		return String.valueOf(raw);
	}

	private static OpenapiMemberV2FailException missingParams(String message) {
		return new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberV2FailException v2Fail(String code, String message) {
		return new OpenapiMemberV2FailException(code, message);
	}
}
