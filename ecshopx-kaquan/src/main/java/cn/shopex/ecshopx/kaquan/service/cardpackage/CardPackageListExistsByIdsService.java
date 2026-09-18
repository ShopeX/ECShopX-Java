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

import cn.shopex.ecshopx.kaquan.domain.CardPackage;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class CardPackageListExistsByIdsService {

	private final CardPackageMapper cardPackageMapper;

	public CardPackageListExistsByIdsService(CardPackageMapper cardPackageMapper) {
		this.cardPackageMapper = cardPackageMapper;
	}

	public List<CardPackage> getListByIdList(long companyId, List<Long> packageIds) {
		if (CollectionUtils.isEmpty(packageIds)) {
			return List.of();
		}
		List<Long> ids = new ArrayList<>();
		for (Long id : packageIds) {
			if (id != null && id > 0L) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return List.of();
		}
		return cardPackageMapper.selectList(
				new LambdaQueryWrapper<CardPackage>()
						.eq(CardPackage::getCompanyId, companyId)
						.in(CardPackage::getPackageId, ids));
	}
}
