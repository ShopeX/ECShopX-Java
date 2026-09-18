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

package cn.shopex.ecshopx.point.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberPointUpdateService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MemberAccountService memberAccountService;
	private final PointMemberAddPointService pointMemberAddPointService;

	public OpenapiThirdApiV2MemberPointUpdateService(
			MemberAccountService memberAccountService,
			PointMemberAddPointService pointMemberAddPointService) {
		this.memberAccountService = memberAccountService;
		this.pointMemberAddPointService = pointMemberAddPointService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void executeOpenapiUpdate(long companyId, Map<String, Object> mergedRaw) {
		Map<String, Object> merged = mergedRaw == null ? Map.of() : mergedRaw;
		validateParams(merged);

		String mobilePlain = stringValue(merged.get("mobile")).trim();
		Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobilePlain);
		long userId = member != null && member.getUserId() != null ? member.getUserId() : 0L;
		if (userId <= 0L) {
			throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
		}

		boolean plus;
		int point;
		if (merged.containsKey("increase_point")) {
			point = parseNonNegativeInt(merged.get("increase_point"), "请求参数错误：增加积分参数错误");
			plus = true;
		} else {
			point = parseNonNegativeInt(merged.get("decrease_point"), "请求参数错误：减去积分参数错误");
			plus = false;
		}
		if (point < 0) {
			throw v2Fail(OpenapiErrorCode.MEMBER_POINT_ERROR, "积分异常");
		}

		String externalId = normalizeExternalId(merged);
		String operaterRemark = requireNonBlankRecord(merged);

		try {
			pointMemberAddPointService.addPointForOpenapi(
					userId, companyId, point, plus, externalId, operaterRemark);
		} catch (OpenapiMemberV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw v2Fail(OpenapiErrorCode.SERVICE_ERROR, "未知错误");
		}
	}

	private void validateParams(Map<String, Object> merged) {
		if (!merged.containsKey("mobile")) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "请求参数错误：会员手机号参数错误");
		}
		String mobile = stringValue(merged.get("mobile"));
		if (!StringUtils.hasText(mobile) || !MOBILE_PATTERN.matcher(mobile.trim()).matches()) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "请求参数错误：会员手机号参数错误");
		}

		boolean hasIncrease = merged.containsKey("increase_point");
		boolean hasDecrease = merged.containsKey("decrease_point");
		if (!hasIncrease && !hasDecrease) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "增加积分或减去积分二选一必填");
		}
		if (hasIncrease && hasDecrease) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "增加积分和减去积分只能存在一个");
		}

		if (hasIncrease) {
			parseNonNegativeInt(merged.get("increase_point"), "请求参数错误：增加积分参数错误");
		} else {
			parseNonNegativeInt(merged.get("decrease_point"), "请求参数错误：减去积分参数错误");
		}

		if (!merged.containsKey("record")) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "请求参数错误：积分变动原因（备注）参数错误");
		}
		String record = stringValue(merged.get("record"));
		if (!StringUtils.hasText(record) || !StringUtils.hasText(record.trim())) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "请求参数错误：积分变动原因（备注）参数错误");
		}

		if (merged.containsKey("external_id")) {
			Object externalIdRaw = merged.get("external_id");
			if (externalIdRaw != null && !isScalarValue(externalIdRaw)) {
				throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "请求参数错误：外部唯一标识参数错误");
			}
		}
	}

	private static String requireNonBlankRecord(Map<String, Object> merged) {
		String record = stringValue(merged.get("record"));
		return record.trim();
	}

	private static String normalizeExternalId(Map<String, Object> merged) {
		if (!merged.containsKey("external_id")) {
			return "";
		}
		Object raw = merged.get("external_id");
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw);
	}

	private static int parseNonNegativeInt(Object raw, String errorMessage) {
		Integer parsed = parseNonNegativeIntOptional(raw);
		if (parsed == null) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, errorMessage);
		}
		return parsed;
	}

	private static Integer parseNonNegativeIntOptional(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number number) {
			if (raw instanceof Double || raw instanceof Float || raw instanceof BigDecimal) {
				double d = number.doubleValue();
				if (d != Math.floor(d)) {
					return null;
				}
			}
			int value = number.intValue();
			return value >= 0 ? value : null;
		}
		if (raw instanceof Boolean) {
			return null;
		}
		String text = String.valueOf(raw).trim();
		if (!text.matches("^[0-9]+$")) {
			return null;
		}
		try {
			long parsed = Long.parseLong(text);
			if (parsed > Integer.MAX_VALUE) {
				return null;
			}
			return (int) parsed;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static boolean isScalarValue(Object value) {
		return value instanceof String || value instanceof Number;
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

	private static OpenapiMemberV2FailException v2Fail(String code, String message) {
		return new OpenapiMemberV2FailException(code, message);
	}
}
