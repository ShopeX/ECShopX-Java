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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import cn.shopex.ecshopx.kaquan.domain.CardPackageTrigger;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageTriggerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CardPackageTriggerPackageIdsService {

	private final CardPackageTriggerMapper cardPackageTriggerMapper;

	public CardPackageTriggerPackageIdsService(CardPackageTriggerMapper cardPackageTriggerMapper) {
		this.cardPackageTriggerMapper = cardPackageTriggerMapper;
	}

	public List<Long> listPackageIds(long companyId, long gradeId, String triggerType) {
		List<CardPackageTrigger> rows = cardPackageTriggerMapper.selectList(new LambdaQueryWrapper<CardPackageTrigger>()
				.eq(CardPackageTrigger::getCompanyId, companyId)
				.eq(CardPackageTrigger::getAssociationId, gradeId)
				.eq(CardPackageTrigger::getTriggerType, triggerType));
		List<Long> out = new ArrayList<>(rows.size());
		for (CardPackageTrigger r : rows) {
			if (r.getPackageId() != null) {
				out.add(r.getPackageId());
			}
		}
		return out;
	}
}
