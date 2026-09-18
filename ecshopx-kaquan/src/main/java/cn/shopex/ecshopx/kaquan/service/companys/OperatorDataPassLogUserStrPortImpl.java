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

package cn.shopex.ecshopx.kaquan.service.companys;

import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassLogUserStrPort;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorDataPassLogUserStrPortImpl implements OperatorDataPassLogUserStrPort {

	private final MemberAccountService memberAccountService;
	private final MemberCardGradeMapper memberCardGradeMapper;

	public OperatorDataPassLogUserStrPortImpl(
			MemberAccountService memberAccountService, MemberCardGradeMapper memberCardGradeMapper) {
		this.memberAccountService = memberAccountService;
		this.memberCardGradeMapper = memberCardGradeMapper;
	}

	@Override
	public String buildUserStrMiddle(long userId, long companyId) {
		Map<String, Object> m = memberAccountService.getMemberInfo(userId, companyId);
		if (m == null || m.isEmpty()) {
			return "";
		}
		Long gradeId = parseGradeId(m.get("grade_id"));
		String gradeName = "";
		if (gradeId != null) {
			MemberCardGrade entity = memberCardGradeMapper.selectOne(
					new LambdaQueryWrapper<MemberCardGrade>()
							.eq(MemberCardGrade::getCompanyId, String.valueOf(companyId))
							.eq(MemberCardGrade::getGradeId, gradeId)
							.last("LIMIT 1"));
			if (entity != null && entity.getGradeName() != null) {
				gradeName = entity.getGradeName();
			}
		}
		String username = "";
		Object u = m.get("username");
		if (u != null && StringUtils.hasText(u.toString())) {
			username = u.toString();
		} else {
			Object n = m.get("name");
			if (n != null && StringUtils.hasText(n.toString())) {
				username = n.toString();
			}
		}
		return gradeName + username + "的";
	}

	private static Long parseGradeId(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
