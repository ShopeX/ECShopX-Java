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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Repository
public class MemberPriceExportQueryRepository {

	private final MemberPriceMapper memberPriceMapper;

	public MemberPriceExportQueryRepository(MemberPriceMapper memberPriceMapper) {
		this.memberPriceMapper = memberPriceMapper;
	}

	/** Returns {@code item_id ->} JSON string from {@code promotions_member_price.price} column. */
	public Map<Long, String> mapMpriceJsonByItemId(long companyId, Collection<Long> itemIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (CollectionUtils.isEmpty(itemIds)) {
			return out;
		}
		LambdaQueryWrapper<MemberPrice> w = new LambdaQueryWrapper<>();
		w.eq(MemberPrice::getCompanyId, companyId).in(MemberPrice::getItemId, itemIds);
		List<MemberPrice> list = memberPriceMapper.selectList(w);
		for (MemberPrice mp : list) {
			if (mp.getItemId() == null) {
				continue;
			}
			out.put(mp.getItemId(), mp.getMprice() != null ? mp.getMprice() : "");
		}
		return out;
	}
}
