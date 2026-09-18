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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 拼团 / 秒杀等活动中需跳过旺店通库存回写的货号，与 {@code CheckPromotionsValid::getActivityItems} 的 BN 落点一致（不经 goods
 * 强依赖，直接查表）。
 */
@Service
public class WdtErpSyncInventoryPromotionSupport {

	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final JdbcTemplate jdbcTemplate;

	public WdtErpSyncInventoryPromotionSupport(
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			JdbcTemplate jdbcTemplate) {
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * 当前公司在途活动下需跳过库存覆盖的 item_bn 集合；无则空集（不阻断同步）。
	 */
	public Set<String> listActiveActivityItemBns(long companyId) {
		long now = System.currentTimeMillis() / 1000L;
		List<Long> groupGoodsIds = new ArrayList<>();
		LambdaQueryWrapper<PromotionGroupsActivity> g = new LambdaQueryWrapper<>();
		g.eq(PromotionGroupsActivity::getCompanyId, companyId)
				.eq(PromotionGroupsActivity::getDisabled, Boolean.FALSE)
				.le(PromotionGroupsActivity::getBeginTime, now)
				.ge(PromotionGroupsActivity::getEndTime, now);
		for (PromotionGroupsActivity row : promotionGroupsActivityMapper.selectList(g)) {
			if (row.getGoodsId() != null) {
				groupGoodsIds.add(row.getGoodsId());
			}
		}
		LambdaQueryWrapper<SeckillRelGoods> s = new LambdaQueryWrapper<>();
		s.eq(SeckillRelGoods::getCompanyId, companyId)
				.eq(SeckillRelGoods::getItemType, "normal")
				.le(SeckillRelGoods::getActivityStartTime, (int) now)
				.ge(SeckillRelGoods::getActivityEndTime, (int) now)
				.eq(SeckillRelGoods::getDisabled, Boolean.FALSE);
		List<Long> seckillItemIds = new ArrayList<>();
		for (SeckillRelGoods row : seckillRelGoodsMapper.selectList(s)) {
			if (row.getItemId() != null) {
				seckillItemIds.add(row.getItemId());
			}
		}
		Set<String> bns = new HashSet<>();
		if (!groupGoodsIds.isEmpty()) {
			for (List<Long> chunk : chunk(groupGoodsIds, 300)) {
				String in = String.join(",", chunk.stream().map(String::valueOf).toList());
				String sql = "SELECT DISTINCT item_bn FROM items WHERE company_id = ? AND goods_id IN (" + in + ")";
				for (String bn : jdbcTemplate.queryForList(sql, String.class, companyId)) {
					if (StringUtils.hasText(bn)) {
						bns.add(bn.trim());
					}
				}
			}
		}
		if (!seckillItemIds.isEmpty()) {
			for (List<Long> chunk : chunk(seckillItemIds, 300)) {
				String in = String.join(",", chunk.stream().map(String::valueOf).toList());
				String sql = "SELECT DISTINCT item_bn FROM items WHERE company_id = ? AND item_id IN (" + in + ")";
				for (String bn : jdbcTemplate.queryForList(sql, String.class, companyId)) {
					if (StringUtils.hasText(bn)) {
						bns.add(bn.trim());
					}
				}
			}
		}
		return bns;
	}

	private static <T> List<List<T>> chunk(List<T> in, int size) {
		List<List<T>> out = new ArrayList<>();
		for (int i = 0; i < in.size(); i += size) {
			out.add(in.subList(i, Math.min(i + size, in.size())));
		}
		return out;
	}
}
