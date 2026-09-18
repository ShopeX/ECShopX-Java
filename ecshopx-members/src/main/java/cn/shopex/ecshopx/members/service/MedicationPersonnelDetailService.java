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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MedicationPersonnel;
import cn.shopex.ecshopx.members.mapper.MedicationPersonnelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicationPersonnelDetailService {

	private final MedicationPersonnelMapper medicationPersonnelMapper;

	public MedicationPersonnelDetailService(MedicationPersonnelMapper medicationPersonnelMapper) {
		this.medicationPersonnelMapper = medicationPersonnelMapper;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getDetail(long companyId, long userId, long id) {
		LambdaQueryWrapper<MedicationPersonnel> wrapper = new LambdaQueryWrapper<MedicationPersonnel>()
				.eq(MedicationPersonnel::getId, id)
				.eq(MedicationPersonnel::getCompanyId, companyId)
				.eq(MedicationPersonnel::getUserId, userId);
		MedicationPersonnel entity = medicationPersonnelMapper.selectOne(wrapper);
		if (entity == null) {
			throw new ResourceException("用药人信息不存在");
		}
		Map<String, Object> out = new LinkedHashMap<>();
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
