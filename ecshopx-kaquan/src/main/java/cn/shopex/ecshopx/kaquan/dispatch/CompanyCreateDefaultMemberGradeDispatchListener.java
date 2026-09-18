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

package cn.shopex.ecshopx.kaquan.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CompanyCreateDefaultMemberGradeDispatchListener implements DispatchListener {

	private final MemberCardGradeMapper memberCardGradeMapper;

	public CompanyCreateDefaultMemberGradeDispatchListener(MemberCardGradeMapper memberCardGradeMapper) {
		this.memberCardGradeMapper = memberCardGradeMapper;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		Object raw = payload.get("company_id");
		if (raw == null) {
			return;
		}
		long companyId = ((Number) raw).longValue();
		String companyIdStr = String.valueOf(companyId);
		MemberCardGrade existing =
				memberCardGradeMapper.selectOne(
						new LambdaQueryWrapper<MemberCardGrade>()
								.eq(MemberCardGrade::getCompanyId, companyIdStr)
								.eq(MemberCardGrade::getDefaultGrade, Boolean.TRUE)
								.last("LIMIT 1"));
		if (existing != null) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		MemberCardGrade row = new MemberCardGrade();
		row.setCompanyId(companyIdStr);
		row.setGradeName("普通会员");
		row.setDefaultGrade(Boolean.TRUE);
		row.setPromotionCondition("{\"total_consumption\":0}");
		row.setPrivileges("{\"discount\":0,\"discount_desc\":0}");
		row.setCreated(now);
		row.setUpdated(now);
		memberCardGradeMapper.insert(row);
	}
}
