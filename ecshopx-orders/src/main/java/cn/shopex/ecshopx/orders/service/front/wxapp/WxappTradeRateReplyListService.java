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
import cn.shopex.ecshopx.orders.domain.TradeRateReply;
import cn.shopex.ecshopx.orders.mapper.TradeRateReplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappTradeRateReplyListService {

	private final TradeRateReplyMapper tradeRateReplyMapper;
	private final MemberAccountService memberAccountService;

	public WxappTradeRateReplyListService(
			TradeRateReplyMapper tradeRateReplyMapper,
			MemberAccountService memberAccountService) {
		this.tradeRateReplyMapper = tradeRateReplyMapper;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> getReplyRateList(long companyId, String rateIdRaw, int page, int pageSize) {
		LambdaQueryWrapper<TradeRateReply> w = buildReplyListWrapper(companyId, rateIdRaw);

		if (pageSize == 0) {
			return replyListTotalOnly(w);
		}

		Page<TradeRateReply> p = new Page<>(page, pageSize);
		tradeRateReplyMapper.selectPage(p, w);
		int totalCountInt = (int) Math.min(p.getTotal(), Integer.MAX_VALUE);
		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (TradeRateReply row : p.getRecords()) {
			listMaps.add(buildRowMap(companyId, row));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCountInt);
		out.put("list", listMaps);
		return out;
	}

	private Map<String, Object> replyListTotalOnly(LambdaQueryWrapper<TradeRateReply> w) {
		long total = tradeRateReplyMapper.selectCount(w);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", (int) Math.min(total, Integer.MAX_VALUE));
		out.put("list", List.of());
		return out;
	}

	private LambdaQueryWrapper<TradeRateReply> buildReplyListWrapper(long companyId, String rateIdRaw) {
		LambdaQueryWrapper<TradeRateReply> w = new LambdaQueryWrapper<>();
		w.eq(TradeRateReply::getCompanyId, companyId);
		if (rateIdRaw != null && rateIdRaw.matches("^-?\\d+$")) {
			try {
				w.eq(TradeRateReply::getRateId, Long.parseLong(rateIdRaw));
			} catch (NumberFormatException e) {
				w.apply("rate_id = {0}", rateIdRaw);
			}
		} else {
			w.apply("rate_id = {0}", rateIdRaw);
		}
		w.orderByDesc(TradeRateReply::getOperatorId).orderByDesc(TradeRateReply::getCreated);
		return w;
	}

	private Map<String, Object> buildRowMap(long companyId, TradeRateReply row) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("reply_id", row.getReplyId());
		map.put("rate_id", row.getRateId());
		map.put("company_id", row.getCompanyId());
		map.put("user_id", row.getUserId());
		map.put("operator_id", row.getOperatorId());
		map.put("content", row.getContent());
		map.put("content_len", row.getContentLen());
		map.put("role", row.getRole());
		map.put("created", row.getCreated());
		map.put("unionid", row.getUnionid());

		String username = "";
		String role = row.getRole();
		if ("buyer".equals(role) && row.getUserId() != null && row.getUserId() > 0) {
			Map<String, Object> wxFilter = new LinkedHashMap<>();
			wxFilter.put("company_id", companyId);
			if (!StringUtils.hasText(row.getUnionid())) {
				wxFilter.put("unionid", null);
			} else {
				wxFilter.put("unionid", row.getUnionid());
			}
			Map<String, Object> wechat = memberAccountService.getWechatUserInfo(wxFilter);
			if (!wechat.containsKey("nickname")) {
				username = "";
			} else {
				Object nickObj = wechat.get("nickname");
				if (nickObj == null) {
					username = "";
				} else {
					String nick = String.valueOf(nickObj).trim();
					if (nick.isEmpty()) {
						username = "";
					} else {
						int cp = nick.codePointCount(0, nick.length());
						if (cp <= 4) {
							username = nick;
						} else {
							int end = nick.offsetByCodePoints(0, 4);
							username = nick.substring(0, end) + "***";
						}
					}
				}
			}
		} else if ("seller".equals(role)
				&& row.getOperatorId() != null
				&& row.getOperatorId() > 0) {
			username = "管理员回复";
		}

		map.put("username", username);
		return map;
	}
}
