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

package cn.shopex.ecshopx.promotions.service.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.SeckillActivityCreateService;
import cn.shopex.ecshopx.promotions.service.seckill.SeckillRelGoodsLifecycleStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class WxappSeckillItemTicketService {

	private final SeckillActivityMapper seckillActivityMapper;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final MessageSource messageSource;
	private final WxappSeckillStoreTicketService wxappSeckillStoreTicketService;

	public WxappSeckillItemTicketService(
			SeckillActivityMapper seckillActivityMapper,
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			MessageSource messageSource,
			WxappSeckillStoreTicketService wxappSeckillStoreTicketService) {
		this.seckillActivityMapper = seckillActivityMapper;
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.messageSource = messageSource;
		this.wxappSeckillStoreTicketService = wxappSeckillStoreTicketService;
	}

	public Object getSeckillItemTicket(long userId, long companyId, long seckillId, long itemId, int num, Locale locale) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		SeckillActivity activity =
				seckillActivityMapper.selectOne(
						new LambdaQueryWrapper<SeckillActivity>()
								.eq(SeckillActivity::getCompanyId, companyId)
								.eq(SeckillActivity::getSeckillId, seckillId));
		if (activity == null) {
			throw new ResourceException(messageSource.getMessage("promotions.seckill.activity_removed", null, locale));
		}
		SeckillRelGoods rel =
				seckillRelGoodsMapper.selectOne(
						new LambdaQueryWrapper<SeckillRelGoods>()
								.eq(SeckillRelGoods::getCompanyId, companyId)
								.eq(SeckillRelGoods::getSeckillId, seckillId)
								.eq(SeckillRelGoods::getItemId, itemId));
		if (rel == null) {
			throw new ResourceException(messageSource.getMessage("promotions.seckill.activity_removed", null, locale));
		}

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", activity.getCompanyId());
		merged.put("seckill_id", activity.getSeckillId());
		merged.put("seckill_type", activity.getSeckillType());
		merged.putAll(SeckillActivityCreateService.relGoodsEntityToAdminRow(rel));
		merged.put("status", SeckillRelGoodsLifecycleStatus.compute(rel, now));

		int limitNum =
				merged.get("limit_num") instanceof Number ? ((Number) merged.get("limit_num")).intValue() : 0;
		if (limitNum > 0 && limitNum < num) {
			throw new ResourceException(
					messageSource.getMessage("promotions.seckill.purchase_quantity_exceeds_limit", null, locale));
		}

		String st = merged.get("status") != null ? merged.get("status").toString() : "";
		switch (st) {
			case "close" -> throw new ResourceException(
					messageSource.getMessage("promotions.seckill.activity_closed", null, locale));
			case "waiting", "in_the_notice" -> throw new ResourceException(
					messageSource.getMessage("promotions.seckill.activity_not_started", null, locale));
			case "it_has_ended" -> throw new ResourceException(
					messageSource.getMessage("promotions.seckill.activity_ended", null, locale));
			default -> { /* in_sale or other */ }
		}

		Object ticket = wxappSeckillStoreTicketService.getTicket(userId, merged, num, locale);
		boolean truthy =
				(ticket instanceof String s && !s.isEmpty()) || Boolean.TRUE.equals(ticket);
		if (truthy) {
			return ticket;
		}
		throw new ResourceException(messageSource.getMessage("promotions.seckill.sold_out", null, locale));
	}
}
