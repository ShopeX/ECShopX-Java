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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.TradeRate;
import cn.shopex.ecshopx.orders.domain.TradeRateReply;
import cn.shopex.ecshopx.orders.mapper.TradeRateMapper;
import cn.shopex.ecshopx.orders.mapper.TradeRateReplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappTradeRateListService {

	private final TradeRateMapper tradeRateMapper;
	private final TradeRateReplyMapper tradeRateReplyMapper;
	private final MemberAccountService memberAccountService;
	private final StringRedisTemplate sharedStringRedisTemplate;

	public WxappTradeRateListService(
			TradeRateMapper tradeRateMapper,
			TradeRateReplyMapper tradeRateReplyMapper,
			MemberAccountService memberAccountService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.tradeRateMapper = tradeRateMapper;
		this.tradeRateReplyMapper = tradeRateReplyMapper;
		this.memberAccountService = memberAccountService;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	public Object getRateList(long companyId, long itemId, int page, int pageSize, String orderType) {
		Map<String, Object> itemRow;
		if (StringUtils.hasText(orderType) && "pointsmall".equals(orderType)) {
			itemRow = tradeRateMapper.selectPointsmallItemsRowMapByItemIdAndCompanyId(itemId, companyId);
		} else {
			itemRow = tradeRateMapper.selectItemsRowMapByItemIdAndCompanyId(itemId, companyId);
		}

		if (itemRow == null || itemRow.get("goods_id") == null) {
			return Collections.emptyList();
		}

		Object g = itemRow.get("goods_id");
		long goodsId;
		try {
			if (g instanceof Number) {
				goodsId = ((Number) g).longValue();
			} else {
				goodsId = Long.parseLong(String.valueOf(g).trim());
			}
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}

		LambdaQueryWrapper<TradeRate> w = new LambdaQueryWrapper<>();
		w.eq(TradeRate::getGoodsId, goodsId)
				.eq(TradeRate::getCompanyId, companyId)
				.eq(TradeRate::getDisabled, false)
				.orderByDesc(TradeRate::getCreated)
				.orderByDesc(TradeRate::getStar)
				.orderByDesc(TradeRate::getRatePicNum)
				.orderByDesc(TradeRate::getContentLen);

		long totalCount = tradeRateMapper.selectCount(w);
		Page<TradeRate> p = new Page<>(page, pageSize, false);
		tradeRateMapper.selectPage(p, w);
		List<TradeRate> records = p.getRecords();

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (TradeRate rate : records) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("rate_id", rate.getRateId());
			row.put("company_id", rate.getCompanyId());
			row.put("item_id", rate.getItemId());
			row.put("goods_id", rate.getGoodsId());
			row.put("order_id", rate.getOrderId());
			row.put("user_id", rate.getUserId());
			String rawPic = rate.getRatePic();
			row.put("rate_pic", rawPic == null ? "" : rawPic);
			row.put("rate_pic_num", rate.getRatePicNum());
			row.put("content", rate.getContent());
			row.put("content_len", rate.getContentLen());
			row.put("is_reply", Boolean.TRUE.equals(rate.getIsReply()));
			row.put("disabled", Boolean.TRUE.equals(rate.getDisabled()));
			row.put("anonymous", Boolean.TRUE.equals(rate.getAnonymous()));
			row.put("star", rate.getStar());
			row.put("created", rate.getCreated());
			row.put("updated", rate.getUpdated());
			row.put("unionid", rate.getUnionid());
			row.put("item_spec_desc", Optional.ofNullable(rate.getItemSpecDesc()).orElse(""));
			row.put("order_type", rate.getOrderType());

			Long uid = rate.getUserId();
			String username;
			String avatar;
			if (uid == null || uid <= 0) {
				username = "匿名用户";
				avatar = "";
			} else {
				Map<String, Object> mem = memberAccountService.getMemberInfo(uid, companyId);
				Object av = mem.get("avatar");
				avatar = av == null ? "" : String.valueOf(av).trim();
				if (!StringUtils.hasText(avatar)) {
					avatar = "";
				}
				Object un = mem.get("username");
				String trimmedUn = un == null ? "" : String.valueOf(un).trim();
				if (!StringUtils.hasText(trimmedUn)) {
					username = "匿名用户";
				} else {
					username = DataMasking.maskTruenameIfBlocked(trimmedUn, 1);
				}
			}
			row.put("avatar", avatar);
			row.put("username", username);

			Object rawPraise =
					sharedStringRedisTemplate
							.opsForHash()
							.get("ratePraise", String.valueOf(rate.getRateId()));
			int praiseNum = 0;
			if (rawPraise != null && StringUtils.hasText(String.valueOf(rawPraise).trim())) {
				try {
					praiseNum = Integer.parseInt(String.valueOf(rawPraise).trim());
				} catch (NumberFormatException ignored) {
					praiseNum = 0;
				}
			}
			row.put("praise_num", praiseNum);

			long c =
					tradeRateReplyMapper.selectCount(
							new LambdaQueryWrapper<TradeRateReply>()
									.eq(TradeRateReply::getRateId, rate.getRateId()));
			LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
			nested.put("total_count", (int) Math.min(c, Integer.MAX_VALUE));
			row.put("reply", nested);

			listMaps.add(row);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listMaps);
		return out;
	}
}
