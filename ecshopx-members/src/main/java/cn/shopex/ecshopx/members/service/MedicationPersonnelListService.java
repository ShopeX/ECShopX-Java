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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.members.domain.MedicationPersonnel;
import cn.shopex.ecshopx.members.mapper.MedicationPersonnelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicationPersonnelListService {

	private final MedicationPersonnelMapper medicationPersonnelMapper;

	public MedicationPersonnelListService(MedicationPersonnelMapper medicationPersonnelMapper) {
		this.medicationPersonnelMapper = medicationPersonnelMapper;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getList(long companyId, long userId, int page, Integer pageSize) {
		LambdaQueryWrapper<MedicationPersonnel> base = new LambdaQueryWrapper<MedicationPersonnel>()
				.eq(MedicationPersonnel::getUserId, userId)
				.eq(MedicationPersonnel::getCompanyId, companyId);

		long total = medicationPersonnelMapper.selectCount(base);

		List<Map<String, Object>> rowMaps;
		if (total == 0L) {
			rowMaps = new ArrayList<>();
		} else {
			LambdaQueryWrapper<MedicationPersonnel> wrapper = new LambdaQueryWrapper<MedicationPersonnel>()
					.eq(MedicationPersonnel::getUserId, userId)
					.eq(MedicationPersonnel::getCompanyId, companyId)
					.orderByDesc(MedicationPersonnel::getIsDefault)
					.orderByDesc(MedicationPersonnel::getCreated);
			if (pageSize != null && pageSize > 0) {
				long offset = ((long) page - 1L) * (long) pageSize.intValue();
				wrapper.last("LIMIT " + pageSize + " OFFSET " + offset);
			}
			List<MedicationPersonnel> rows = medicationPersonnelMapper.selectList(wrapper);
			rowMaps = new ArrayList<>(rows.size());
			for (MedicationPersonnel entity : rows) {
				rowMaps.add(toRowMap(entity));
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", rowMaps);
		return result;
	}

	private static LinkedHashMap<String, Object> toRowMap(MedicationPersonnel entity) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", entity.getId());
		out.put("company_id", entity.getCompanyId());
		out.put("user_id", entity.getUserId());
		out.put("user_family_name", entity.getUserFamilyName());
		out.put("user_family_id_card", entity.getUserFamilyIdCard());
		out.put("user_family_age", entity.getUserFamilyAge());
		out.put("user_family_gender", entity.getUserFamilyGender());
		out.put("user_family_phone", entity.getUserFamilyPhone());
		out.put("relationship", entity.getRelationship());
		out.put("is_default", entity.getIsDefault());
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		return out;
	}
}
