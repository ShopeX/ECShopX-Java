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

import cn.shopex.ecshopx.common.kaquan.port.OpenapiMemberListKaquanLookupPort;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiMemberListKaquanLookupPortImpl implements OpenapiMemberListKaquanLookupPort {

	private final MemberCardGradeMapper memberCardGradeMapper;
	private final VipGradeMapper vipGradeMapper;
	private final VipGradeRelUserMapper vipGradeRelUserMapper;

	public OpenapiMemberListKaquanLookupPortImpl(
			MemberCardGradeMapper memberCardGradeMapper,
			VipGradeMapper vipGradeMapper,
			VipGradeRelUserMapper vipGradeRelUserMapper) {
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.vipGradeMapper = vipGradeMapper;
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
	}

	@Override
	public long resolveGradeIdByName(long companyId, String gradeName) {
		if (!StringUtils.hasText(gradeName)) {
			return 0L;
		}
		MemberCardGrade row =
				memberCardGradeMapper.selectOne(
						new LambdaQueryWrapper<MemberCardGrade>()
								.eq(MemberCardGrade::getCompanyId, String.valueOf(companyId))
								.eq(MemberCardGrade::getGradeName, gradeName.trim())
								.last("LIMIT 1"));
		return row == null || row.getGradeId() == null ? 0L : row.getGradeId();
	}

	@Override
	public long resolveVipGradeIdByName(long companyId, String vipGradeName) {
		if (!StringUtils.hasText(vipGradeName)) {
			return 0L;
		}
		VipGrade row =
				vipGradeMapper.selectOne(
						new LambdaQueryWrapper<VipGrade>()
								.eq(VipGrade::getCompanyId, (int) companyId)
								.eq(VipGrade::getGradeName, vipGradeName.trim())
								.eq(VipGrade::getIsDisabled, true)
								.last("LIMIT 1"));
		return row == null || row.getVipGradeId() == null ? 0L : row.getVipGradeId();
	}

	@Override
	public Map<Long, List<Map<String, Object>>> loadActiveVipGradesByUserIds(long companyId, List<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Map.of();
		}
		long nowSec = Instant.now().getEpochSecond();
		List<VipGradeRelUser> relRows =
				vipGradeRelUserMapper.selectList(
						new LambdaQueryWrapper<VipGradeRelUser>()
								.eq(VipGradeRelUser::getCompanyId, (int) companyId)
								.in(VipGradeRelUser::getUserId, userIds));
		List<VipGrade> vipGrades =
				vipGradeMapper.selectList(
						new LambdaQueryWrapper<VipGrade>().eq(VipGrade::getCompanyId, (int) companyId));
		Map<Long, String> gradeNameById = new LinkedHashMap<>();
		for (VipGrade grade : vipGrades) {
			if (grade.getVipGradeId() != null) {
				gradeNameById.put(grade.getVipGradeId(), grade.getGradeName());
			}
		}

		Map<Long, List<Map<String, Object>>> out = new LinkedHashMap<>();
		for (VipGradeRelUser rel : relRows) {
			long endDate = parseEndDateSeconds(rel.getEndDate());
			if (endDate < nowSec) {
				continue;
			}
			Long userId = rel.getUserId();
			if (userId == null) {
				continue;
			}
			Long vipGradeId = rel.getVipGradeId() != null ? rel.getVipGradeId() : 0L;
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", vipGradeId);
			item.put("type", rel.getVipType() != null ? rel.getVipType() : "");
			item.put("end_date", endDate);
			item.put("grade_nme", gradeNameById.getOrDefault(vipGradeId, ""));
			out.computeIfAbsent(userId, k -> new ArrayList<>()).add(item);
		}
		return out;
	}

	private static long parseEndDateSeconds(String endDate) {
		if (endDate == null || endDate.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(endDate.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
