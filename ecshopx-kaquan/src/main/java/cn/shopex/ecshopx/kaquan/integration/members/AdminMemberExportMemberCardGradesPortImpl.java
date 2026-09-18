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

package cn.shopex.ecshopx.kaquan.integration.members;

import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeQueryService;
import cn.shopex.ecshopx.members.service.export.port.AdminMemberExportMemberCardGradesPort;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminMemberExportMemberCardGradesPortImpl implements AdminMemberExportMemberCardGradesPort {

	private final MemberCardGradeQueryService memberCardGradeQueryService;

	public AdminMemberExportMemberCardGradesPortImpl(MemberCardGradeQueryService memberCardGradeQueryService) {
		this.memberCardGradeQueryService = memberCardGradeQueryService;
	}

	@Override
	public List<Map<String, Object>> getGradeListByCompanyId(long companyId, boolean isMemberCount) {
		return memberCardGradeQueryService.getGradeListByCompanyId(companyId, isMemberCount);
	}
}
