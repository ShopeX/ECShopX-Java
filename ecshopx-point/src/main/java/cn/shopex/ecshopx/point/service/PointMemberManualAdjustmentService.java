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

package cn.shopex.ecshopx.point.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointMemberManualAdjustmentService {

	private final MemberAccountService memberAccountService;
	private final PointMemberAddPointService pointMemberAddPointService;

	public PointMemberManualAdjustmentService(
			MemberAccountService memberAccountService, PointMemberAddPointService pointMemberAddPointService) {
		this.memberAccountService = memberAccountService;
		this.pointMemberAddPointService = pointMemberAddPointService;
	}

	public Map<String, Object> adjust(HttpServletRequest request, Map<String, Object> mergedInput) {
		Object rawUserId = mergedInput.get("user_id");
		if (rawUserId == null || (rawUserId instanceof String s && !StringUtils.hasText(s.trim()))) {
			throw new BadRequestException("会员ID必填");
		}
		long userId = parseRequiredUserId(rawUserId);

		Object rawAdj = mergedInput.get("adjustment_type");
		if (rawAdj == null || (rawAdj instanceof String s && !StringUtils.hasText(s.trim()))) {
			throw new BadRequestException("调整类型必填");
		}
		String adjustmentType = String.valueOf(rawAdj).trim();
		if (!"plus".equals(adjustmentType) && !"reduce".equals(adjustmentType)) {
			throw new BadRequestException("调整类型必填");
		}

		Object rawPoint = mergedInput.get("point");
		if (rawPoint == null || (rawPoint instanceof String ps && !StringUtils.hasText(ps.trim()))) {
			throw new BadRequestException("积分必填");
		}
		int pointInt = parseIntVal(rawPoint);
		if (pointInt < 0) {
			throw new BadRequestException("积分必填");
		}
		if (pointInt > 9_999_999) {
			throw new ResourceException("可调整积分最大为9999999");
		}

		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object operatorType = ud.get("operator_type");
		if (!"admin".equals(operatorType == null ? "" : String.valueOf(operatorType))) {
			throw new ForbiddenException("请使用超级管理员调整积分");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);
		String username = ud.get("username") == null ? "" : String.valueOf(ud.get("username"));

		Map<String, Object> memberRow = memberAccountService.getMemberInfo(userId, companyId);
		Object uidCheck = memberRow.get("user_id");
		if (uidCheck == null || !StringUtils.hasText(String.valueOf(uidCheck))) {
			throw new ResourceException("未查询到相关会员信息");
		}
		Object mobileVal = memberRow.get("mobile");
		if (!StringUtils.hasText(mobileVal == null ? "" : String.valueOf(mobileVal))) {
			throw new ResourceException("未查询到相关会员信息");
		}

		if (pointInt <= 0) {
			throw new ResourceException("积分必填");
		}

		boolean plus = "plus".equals(adjustmentType);
		String record = "管理员：" + username + "，手动调整积分";
		pointMemberAddPointService.addPointForManualAdjustment(userId, companyId, pointInt, plus, record);
		return Map.of("status", true);
	}

	private static long parseRequiredUserId(Object raw) {
		try {
			long v = parseLongLoose(raw);
			if (v <= 0) {
				throw new BadRequestException("会员ID必填");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("会员ID必填");
		}
	}

	private static long parseLongLoose(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static long parseCompanyId(Object v) {
		try {
			return parseLongLoose(v);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private static int parseIntVal(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(o).trim();
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
