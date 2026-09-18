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

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.TradeRate;
import cn.shopex.ecshopx.orders.domain.TradeRateReply;
import cn.shopex.ecshopx.orders.mapper.TradeRateMapper;
import cn.shopex.ecshopx.orders.mapper.TradeRateReplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappTradeRateDetailService {

	private final TradeRateMapper tradeRateMapper;
	private final TradeRateReplyMapper tradeRateReplyMapper;
	private final MemberAccountService memberAccountService;
	private final StringRedisTemplate sharedStringRedisTemplate;

	public WxappTradeRateDetailService(
			TradeRateMapper tradeRateMapper,
			TradeRateReplyMapper tradeRateReplyMapper,
			MemberAccountService memberAccountService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.tradeRateMapper = tradeRateMapper;
		this.tradeRateReplyMapper = tradeRateReplyMapper;
		this.memberAccountService = memberAccountService;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	public Map<String, Object> getRateDetail(long companyId, long rateId) {
		TradeRate rate = tradeRateMapper.selectById(rateId);
		if (rate == null) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("avatar", "");
			empty.put("username", "");
			empty.put("praise_num", 0);
			empty.put("reply_count", 0);
			empty.put("item_spec_desc", "");
			return empty;
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("rate_id", rate.getRateId());
		result.put("company_id", rate.getCompanyId());
		result.put("item_id", rate.getItemId());
		result.put("goods_id", rate.getGoodsId());
		result.put("order_id", rate.getOrderId());
		result.put("user_id", rate.getUserId());
		String rawPic = rate.getRatePic();
		result.put("rate_pic", rawPic == null ? "" : rawPic);
		result.put("rate_pic_num", rate.getRatePicNum());
		result.put("content", rate.getContent());
		result.put("content_len", rate.getContentLen());
		result.put("created", rate.getCreated());
		result.put("updated", rate.getUpdated());
		result.put("unionid", rate.getUnionid());
		result.put("item_spec_desc", rate.getItemSpecDesc());
		result.put("order_type", rate.getOrderType());
		result.put("is_reply", Boolean.TRUE.equals(rate.getIsReply()));
		result.put("disabled", Boolean.TRUE.equals(rate.getDisabled()));
		result.put("anonymous", Boolean.TRUE.equals(rate.getAnonymous()));
		result.put("star", rate.getStar());

		Map<String, Object> wxFilter = new LinkedHashMap<>();
		wxFilter.put("company_id", companyId);
		if (!StringUtils.hasText(rate.getUnionid())) {
			wxFilter.put("unionid", null);
		} else {
			wxFilter.put("unionid", rate.getUnionid());
		}
		Map<String, Object> wechat = memberAccountService.getWechatUserInfo(wxFilter);

		String head =
				wechat.get("headimgurl") == null ? "" : String.valueOf(wechat.get("headimgurl"));
		result.put("avatar", head);

		if (!wechat.containsKey("nickname")) {
			result.put("username", "");
		} else {
			Object nickObj = wechat.get("nickname");
			if (nickObj == null) {
				result.put("username", "");
			} else {
				String nick = String.valueOf(nickObj).trim();
				if (nick.isEmpty()) {
					result.put("username", "");
				} else {
					int cp = nick.codePointCount(0, nick.length());
					if (cp <= 4) {
						result.put("username", nick);
					} else {
						int end = nick.offsetByCodePoints(0, 4);
						result.put("username", nick.substring(0, end) + "***");
					}
				}
			}
		}

		Object rawPraise =
				sharedStringRedisTemplate.opsForHash().get("ratePraise", String.valueOf(rateId));
		int praiseNum = 0;
		if (rawPraise != null && StringUtils.hasText(String.valueOf(rawPraise).trim())) {
			try {
				praiseNum = Integer.parseInt(String.valueOf(rawPraise).trim());
			} catch (NumberFormatException ignored) {
				praiseNum = 0;
			}
		}
		result.put("praise_num", praiseNum);

		long c =
				tradeRateReplyMapper.selectCount(
						new LambdaQueryWrapper<TradeRateReply>().eq(TradeRateReply::getRateId, rateId));
		result.put("reply_count", (int) Math.min(c, Integer.MAX_VALUE));

		result.put("item_spec_desc", Optional.ofNullable(rate.getItemSpecDesc()).orElse(""));

		return result;
	}
}
