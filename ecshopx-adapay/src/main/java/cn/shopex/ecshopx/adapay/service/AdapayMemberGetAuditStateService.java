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
import cn.shopex.ecshopx.companys.service.employee.MemberOperatorContextService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdapayMemberGetAuditStateService {

	private final MemberOperatorContextService memberOperatorContextService;
	private final AdapayMemberMapper adapayMemberMapper;

	public AdapayMemberGetAuditStateService(
			MemberOperatorContextService memberOperatorContextService, AdapayMemberMapper adapayMemberMapper) {
		this.memberOperatorContextService = memberOperatorContextService;
		this.adapayMemberMapper = adapayMemberMapper;
	}

	public Map<String, Object> getAuditState(long companyId, Map<String, Object> jwtMap) {
		long jwtOperatorId = toLong(jwtMap.get("operator_id"));
		String jwtOperatorType = stringVal(jwtMap.get("operator_type"));
		Long jwtDistributorId = toLongObject(jwtMap.get("distributor_id"));
		Map<String, Object> ctx =
				memberOperatorContextService.resolve(jwtOperatorId, jwtOperatorType, jwtDistributorId);

		int effectiveOperatorId = toInt(ctx.get("operator_id"));
		String effectiveOperatorType = stringVal(ctx.get("operator_type"));

		AdapayMember row = adapayMemberMapper.selectOne(
				new LambdaQueryWrapper<AdapayMember>()
						.eq(AdapayMember::getCompanyId, companyId)
						.eq(AdapayMember::getOperatorId, effectiveOperatorId)
						.eq(AdapayMember::getOperatorType, effectiveOperatorType)
						.last("LIMIT 1"));

		if (row == null) {
			Map<String, Object> out = new LinkedHashMap<>(2);
			out.put("audit_state", "D");
			out.put("audit_desc", "待提交");
			return out;
		}

		String dbState = row.getAuditState();
		if (dbState == null) {
			dbState = "";
		}
		String external;
		switch (dbState) {
			case "0":
			case "A":
				external = "A";
				break;
			case "B":
			case "C":
			case "D":
				external = "B";
				break;
			case "E":
				external = "C";
				break;
			default:
				external = "A";
				break;
		}

		Map<String, Object> out = new LinkedHashMap<>(5);
		out.put("audit_state", external);
		out.put("audit_desc", row.getAuditDesc());
		out.put("update_time", row.getUpdateTime());
		out.put("member_type", row.getMemberType());
		out.put("valid", row.getValid());
		return out;
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
