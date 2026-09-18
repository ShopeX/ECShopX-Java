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

package cn.shopex.ecshopx.kaquan.service.membercard;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberCardGradeNextForWxappService {

	private final MemberCardGradeQueryService memberCardGradeQueryService;

	public MemberCardGradeNextForWxappService(MemberCardGradeQueryService memberCardGradeQueryService) {
		this.memberCardGradeQueryService = memberCardGradeQueryService;
	}

	public void putNextGradeInfo(long companyId, long currentGradeId, Map<String, Object> memberInfo) {
		List<Map<String, Object>> grades = memberCardGradeQueryService.getGradeListByCompanyId(companyId, true);
		if (grades == null || grades.isEmpty()) {
			memberInfo.put("nextGradeInfo", Collections.emptyList());
			return;
		}
		int idx = -1;
		for (int i = 0; i < grades.size(); i++) {
			long gid = normalizeGradeId(grades.get(i).get("grade_id"));
			if (gid == currentGradeId) {
				idx = i;
				break;
			}
		}
		if (idx < 0 || idx + 1 >= grades.size()) {
			memberInfo.put("nextGradeInfo", Collections.emptyList());
			return;
		}
		memberInfo.put("nextGradeInfo", new LinkedHashMap<>(grades.get(idx + 1)));
	}

	private static long normalizeGradeId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String t = String.valueOf(raw).trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
