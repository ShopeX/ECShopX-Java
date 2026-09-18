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

import cn.shopex.ecshopx.popularize.domain.PromoterGoods;
import cn.shopex.ecshopx.popularize.mapper.PromoterGoodsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PromoterRelGoodsService {

	private final PromoterGoodsMapper promoterGoodsMapper;

	public PromoterRelGoodsService(PromoterGoodsMapper promoterGoodsMapper) {
		this.promoterGoodsMapper = promoterGoodsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void relPromoterGoods(long companyId, long userId, Object goodsIdRaw) {
		Long queryGoodsId = resolveQueryGoodsId(goodsIdRaw);

		LambdaQueryWrapper<PromoterGoods> w = new LambdaQueryWrapper<PromoterGoods>()
				.eq(PromoterGoods::getCompanyId, companyId)
				.eq(PromoterGoods::getUserId, userId);
		if (queryGoodsId == null) {
			w.isNull(PromoterGoods::getGoodsId);
		} else {
			w.eq(PromoterGoods::getGoodsId, queryGoodsId);
		}
		w.last("LIMIT 1");
		PromoterGoods existing = promoterGoodsMapper.selectOne(w);

		Long insertGoodsId = resolveInsertGoodsId(goodsIdRaw);
		if (existing == null && insertGoodsId != null) {
			PromoterGoods row = new PromoterGoods();
			row.setCompanyId(companyId);
			row.setUserId(userId);
			row.setGoodsId(insertGoodsId);
			row.setCreated((int) (System.currentTimeMillis() / 1000L));
			promoterGoodsMapper.insert(row);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteRelPromoterGoods(long companyId, long userId, Object goodsIdRaw) {
		Long queryGoodsId = resolveQueryGoodsId(goodsIdRaw);

		LambdaQueryWrapper<PromoterGoods> w = new LambdaQueryWrapper<PromoterGoods>()
				.eq(PromoterGoods::getCompanyId, companyId)
				.eq(PromoterGoods::getUserId, userId);
		if (queryGoodsId == null) {
			w.isNull(PromoterGoods::getGoodsId);
		} else {
			w.eq(PromoterGoods::getGoodsId, queryGoodsId);
		}
		promoterGoodsMapper.delete(w);
	}

	private static Long resolveQueryGoodsId(Object goodsIdRaw) {
		if (goodsIdRaw == null) {
			return null;
		}
		String s = String.valueOf(goodsIdRaw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long resolveInsertGoodsId(Object goodsIdRaw) {
		String s = String.valueOf(goodsIdRaw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
