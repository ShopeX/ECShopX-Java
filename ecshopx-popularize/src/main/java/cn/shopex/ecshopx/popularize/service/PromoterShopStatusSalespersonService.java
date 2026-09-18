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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromoterShopStatusSalespersonService {

	private final PromoterMapper promoterMapper;

	public PromoterShopStatusSalespersonService(PromoterMapper promoterMapper) {
		this.promoterMapper = promoterMapper;
	}

	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public void updateShopStatusSalesperson(long companyId, long userId) {
		LambdaQueryWrapper<Promoter> active = new LambdaQueryWrapper<Promoter>()
				.eq(Promoter::getCompanyId, companyId)
				.eq(Promoter::getUserId, userId)
				.and(w -> w.isNull(Promoter::getDisabled).or().eq(Promoter::getDisabled, 0));

		Promoter promoter = promoterMapper.selectOne(active);
		if (promoter == null) {
			Promoter insert = new Promoter();
			insert.setUserId(userId);
			insert.setCompanyId(companyId);
			insert.setIdentityId(0L);
			insert.setIsSubordinates(0);
			insert.setPid(null);
			insert.setPmobile("0");
			insert.setPname("");
			insert.setGradeLevel(1);
			insert.setIsPromoter(0);
			insert.setDisabled(0);
			insert.setShopStatus(0);
			insert.setIsBuy(0);
			insert.setPromoterName("");
			insert.setRegionsId(null);
			insert.setAddress("");
			insert.setCreated((int) Instant.now().getEpochSecond());
			promoterMapper.insert(insert);
		}

		promoter = promoterMapper.selectOne(active);
		if (promoter == null) {
			return;
		}
		promoter.setIsPromoter(1);
		promoter.setShopStatus(1);
		promoterMapper.updateById(promoter);
	}
}
