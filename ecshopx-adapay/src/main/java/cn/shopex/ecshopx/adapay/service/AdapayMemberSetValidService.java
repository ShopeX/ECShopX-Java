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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.employee.MemberOperatorContextService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayMemberSetValidService {

	private final MemberOperatorContextService memberOperatorContextService;
	private final AdapayMemberMapper adapayMemberMapper;

	public AdapayMemberSetValidService(
			MemberOperatorContextService memberOperatorContextService, AdapayMemberMapper adapayMemberMapper) {
		this.memberOperatorContextService = memberOperatorContextService;
		this.adapayMemberMapper = adapayMemberMapper;
	}

	public void setValid(long companyId, Map<String, Object> jwtMap) {
		long jwtOperatorId = toLong(jwtMap.get("operator_id"));
		String jwtOperatorType = stringVal(jwtMap.get("operator_type"));
		Long jwtDistributorId = toLongObject(jwtMap.get("distributor_id"));
		Map<String, Object> operatorContext =
				memberOperatorContextService.resolve(jwtOperatorId, jwtOperatorType, jwtDistributorId);

		int effectiveOperatorId = toInt(operatorContext.get("operator_id"));
		String effectiveOperatorType = stringVal(operatorContext.get("operator_type"));
		if (effectiveOperatorId <= 0 || !StringUtils.hasText(effectiveOperatorType)) {
			return;
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<AdapayMember> uw = new LambdaUpdateWrapper<>();
		uw.eq(AdapayMember::getOperatorId, effectiveOperatorId)
				.eq(AdapayMember::getOperatorType, effectiveOperatorType)
				.set(AdapayMember::getValid, Boolean.TRUE)
				.set(AdapayMember::getUpdateTime, now);
		int rows = adapayMemberMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到个人经销商更新数据");
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long toLongObject(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int toInt(Object o) {
		long v = toLong(o);
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
	}
}
