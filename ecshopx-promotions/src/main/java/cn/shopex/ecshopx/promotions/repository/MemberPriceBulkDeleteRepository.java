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

package cn.shopex.ecshopx.promotions.repository;

import cn.shopex.ecshopx.promotions.domain.MemberPrice;
import cn.shopex.ecshopx.promotions.mapper.MemberPriceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import org.springframework.stereotype.Repository;

@Repository
public class MemberPriceBulkDeleteRepository {

	private final MemberPriceMapper memberPriceMapper;

	public MemberPriceBulkDeleteRepository(MemberPriceMapper memberPriceMapper) {
		this.memberPriceMapper = memberPriceMapper;
	}

	public int deleteByCompanyIdAndItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<MemberPrice> w = new LambdaQueryWrapper<>();
		w.eq(MemberPrice::getCompanyId, companyId).in(MemberPrice::getItemId, itemIds);
		return memberPriceMapper.delete(w);
	}
}
