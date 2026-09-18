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
import cn.shopex.ecshopx.common.promotions.port.CancelSeckillPlatTicketJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.SeckillActivityCreateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappSeckillStoreTicketService {

	private final StringRedisTemplate stringRedisTemplate;
	private final MessageSource messageSource;
	private final SeckillTicketHashidsSupport hashids;
	private final CancelSeckillPlatTicketJobDispatchPublisher cancelSeckillPlatTicketJobDispatchPublisher;
	private final SeckillActivityMapper seckillActivityMapper;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;

	public WxappSeckillStoreTicketService(
			StringRedisTemplate stringRedisTemplate,
			MessageSource messageSource,
			SeckillTicketHashidsSupport hashids,
			CancelSeckillPlatTicketJobDispatchPublisher cancelSeckillPlatTicketJobDispatchPublisher,
			SeckillActivityMapper seckillActivityMapper,
			SeckillRelGoodsMapper seckillRelGoodsMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.messageSource = messageSource;
		this.hashids = hashids;
		this.cancelSeckillPlatTicketJobDispatchPublisher = cancelSeckillPlatTicketJobDispatchPublisher;
		this.seckillActivityMapper = seckillActivityMapper;
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
	}

	/**
	 * Returns a Redis key segment from {@code v}. {@code null} or a blank value after trimming yields an
	 * empty segment; otherwise the trimmed string form is used (numbers formatted as decimal long strings).
	 */
	private static String segmentForRedisKey(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof Number n) {
			return Long.toString(n.longValue());
		}
		String s = v.toString().trim();
		return s.isEmpty() ? "" : s;
	}

	public boolean cancelSeckillTicket(String seckillTicket, long userId, long companyId) {
		long[] ticketData = hashids.decode(seckillTicket == null ? "" : seckillTicket.trim());
		if (ticketData.length < 1) {
			return true;
		}
		if (ticketData.length < 3) {
			return true;
		}
		long buyNum = ticketData[0];
		long seckillIdDecoded = ticketData[1];
		long itemIdDecoded = ticketData[2];

		Map<String, Object> seckillData = new LinkedHashMap<>();
		SeckillActivity activity =
				seckillActivityMapper.selectOne(
						new LambdaQueryWrapper<SeckillActivity>()
								.eq(SeckillActivity::getCompanyId, companyId)
								.eq(SeckillActivity::getSeckillId, seckillIdDecoded));
		if (activity != null) {
			seckillData.put("company_id", activity.getCompanyId());
			seckillData.put("seckill_id", activity.getSeckillId());
			SeckillRelGoods rel =
					seckillRelGoodsMapper.selectOne(
							new LambdaQueryWrapper<SeckillRelGoods>()
									.eq(SeckillRelGoods::getCompanyId, companyId)
									.eq(SeckillRelGoods::getSeckillId, seckillIdDecoded)
									.eq(SeckillRelGoods::getItemId, itemIdDecoded));
			if (rel != null) {
				seckillData.putAll(SeckillActivityCreateService.relGoodsEntityToAdminRow(rel));
			}
		}

		String companySegment = segmentForRedisKey(seckillData.get("company_id"));
		String seckillSegment = segmentForRedisKey(seckillData.get("seckill_id"));
		String itemSegment = segmentForRedisKey(seckillData.get("item_id"));
		String ticketKey =
				"seckillTicketCompany" + companySegment + ":actid" + seckillSegment + ":itemid" + itemSegment;
		String seckillKey = "seckillActivityItemStore:" + companySegment + ":" + seckillSegment;
		String itemStoreField = "store_" + itemSegment;

		Object rawTmp = stringRedisTemplate.opsForHash().get(ticketKey, String.valueOf(userId));
		String tmpticket = rawTmp == null ? null : rawTmp.toString();
		boolean match = Objects.equals(tmpticket, seckillTicket);
		if (!match) {
			return true;
		}
		stringRedisTemplate.opsForHash().delete(ticketKey, String.valueOf(userId));
		Long newStore = stringRedisTemplate.opsForHash().increment(seckillKey, itemStoreField, buyNum);
		if (newStore != null && newStore < 0L) {
			stringRedisTemplate.opsForHash().put(seckillKey, itemStoreField, "0");
		}
		return true;
	}

	public Object getTicket(long userId, Map<String, Object> seckillInfo, int num, Locale locale) {
		if ("limited_time_sale".equals(seckillInfo.get("seckill_type"))) {
			return Boolean.TRUE;
		}
		long companyId = requireLong(seckillInfo, "company_id", locale);
		long seckillId = requireLong(seckillInfo, "seckill_id", locale);
		long itemId = requireLong(seckillInfo, "item_id", locale);
		String userIdField = String.valueOf(userId);
		String ticketKey = "seckillTicketCompany" + companyId + ":actid" + seckillId + ":itemid" + itemId;
		String seckillKey = "seckillActivityItemStore:" + companyId + ":" + seckillId;
		String itemStoreField = "store_" + itemId;
		String userByItemStoreField = "buystore_" + itemId + "_" + userId;

		Object oldTicket = stringRedisTemplate.opsForHash().get(ticketKey, userIdField);
		boolean hadOldTicket = oldTicket != null && StringUtils.hasText(oldTicket.toString().trim());
		if (hadOldTicket) {
			long[] decoded = hashids.decode(oldTicket.toString());
			if (decoded.length > 0 && decoded[0] == num) {
				return oldTicket.toString();
			}
			long replenish = decoded.length > 0 ? decoded[0] : 0L;
			stringRedisTemplate.opsForHash().increment(seckillKey, itemStoreField, replenish);
		}

		int limitNum =
				seckillInfo.get("limit_num") instanceof Number
						? ((Number) seckillInfo.get("limit_num")).intValue()
						: 0;
		if (limitNum > 0) {
			Object total = stringRedisTemplate.opsForHash().get(seckillKey, userByItemStoreField);
			long totalBuy = total == null ? 0L : Long.parseLong(total.toString());
			if (totalBuy + num > limitNum) {
				throw new ResourceException(
						messageSource.getMessage("promotions.seckill.per_user_limit", new Object[] {limitNum}, locale));
			}
		}

		if (hadOldTicket) {
			stringRedisTemplate.opsForHash().put(ticketKey, userIdField, "0");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		long[] encodeData = {num, seckillId, itemId, now};
		String ticket = hashids.encode(encodeData);
		stringRedisTemplate.opsForHash().put(ticketKey, userIdField, ticket);
		Long newStore = stringRedisTemplate.opsForHash().increment(seckillKey, itemStoreField, -num);
		if (newStore != null && newStore < 0) {
			stringRedisTemplate.opsForHash().put(ticketKey, userIdField, "0");
			stringRedisTemplate.opsForHash().put(seckillKey, itemStoreField, "0");
			throw new ResourceException(messageSource.getMessage("promotions.seckill.out_of_stock", null, locale));
		}
		cancelSeckillPlatTicketJobDispatchPublisher.enqueueCancelSeckillPlatTicket(
				ticketKey, seckillKey, itemStoreField, num, userIdField);
		return ticket;
	}

	private long requireLong(Map<String, Object> seckillInfo, String key, Locale locale) {
		Object v = seckillInfo.get(key);
		if (!(v instanceof Number)) {
			throw new ResourceException(messageSource.getMessage("promotions.seckill.activity_removed", null, locale));
		}
		return ((Number) v).longValue();
	}
}
