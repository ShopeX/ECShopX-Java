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
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicationPersonnelCreateService {

	private static final Set<Integer> ALLOWED_RELATIONSHIPS = Set.of(1, 2, 3, 4, 5);

	private final MedicationPersonnelMapper medicationPersonnelMapper;

	public MedicationPersonnelCreateService(MedicationPersonnelMapper medicationPersonnelMapper) {
		this.medicationPersonnelMapper = medicationPersonnelMapper;
	}

	@Transactional
	public Map<String, Object> create(
			long companyId,
			long userId,
			String userFamilyName,
			String userFamilyIdCard,
			int userFamilyAge,
			int userFamilyGender,
			String userFamilyPhone,
			int relationship) {
		LambdaQueryWrapper<MedicationPersonnel> dupCard = new LambdaQueryWrapper<MedicationPersonnel>()
				.eq(MedicationPersonnel::getCompanyId, companyId)
				.eq(MedicationPersonnel::getUserId, userId)
				.eq(MedicationPersonnel::getUserFamilyIdCard, userFamilyIdCard);
		if (medicationPersonnelMapper.selectOne(dupCard) != null) {
			throw new ResourceException("不能重复添加同一个用药人");
		}
		if (!ALLOWED_RELATIONSHIPS.contains(relationship)) {
			throw new ResourceException("与本人关系类型错误");
		}
		if (relationship == 1) {
			LambdaQueryWrapper<MedicationPersonnel> selfDup = new LambdaQueryWrapper<MedicationPersonnel>()
					.eq(MedicationPersonnel::getCompanyId, companyId)
					.eq(MedicationPersonnel::getUserId, userId)
					.eq(MedicationPersonnel::getRelationship, 1);
			if (medicationPersonnelMapper.selectOne(selfDup) != null) {
				throw new ResourceException("已存在关系为“本人”的用药人，请修改“与本人关系”或用药人信息");
			}
		}
		if (userFamilyAge < 6) {
			throw new ResourceException("不支持添加6岁以下用药人");
		}
		int isDefault = relationship == 1 ? 1 : 0;
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		MedicationPersonnel entity = new MedicationPersonnel();
		entity.setCompanyId(companyId);
		entity.setUserId(userId);
		entity.setUserFamilyName(userFamilyName);
		entity.setUserFamilyIdCard(userFamilyIdCard);
		entity.setUserFamilyAge(userFamilyAge);
		entity.setUserFamilyGender(userFamilyGender);
		entity.setUserFamilyPhone(userFamilyPhone);
		entity.setRelationship(relationship);
		entity.setIsDefault(isDefault);
		entity.setCreated(nowSec);
		entity.setUpdated(nowSec);
		medicationPersonnelMapper.insert(entity);
		return Map.of("success", Boolean.TRUE);
	}
}
