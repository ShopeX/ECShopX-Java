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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.TradeRate;
import cn.shopex.ecshopx.orders.domain.TradeRateReply;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeRateMapper;
import cn.shopex.ecshopx.orders.mapper.TradeRateReplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TradeRateAdminDetailService {

	private final TradeRateMapper tradeRateMapper;
	private final TradeRateReplyMapper tradeRateReplyMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final MemberAccountService memberAccountService;
	private final OperatorsMapper operatorsMapper;

	public TradeRateAdminDetailService(
			TradeRateMapper tradeRateMapper,
			TradeRateReplyMapper tradeRateReplyMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			MemberAccountService memberAccountService,
			OperatorsMapper operatorsMapper) {
		this.tradeRateMapper = tradeRateMapper;
		this.tradeRateReplyMapper = tradeRateReplyMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.memberAccountService = memberAccountService;
		this.operatorsMapper = operatorsMapper;
	}

	public Map<String, Object> getTradeRateInfo(long companyId, long rateId) {
		LambdaQueryWrapper<TradeRate> w = new LambdaQueryWrapper<>();
		w.eq(TradeRate::getRateId, rateId).eq(TradeRate::getCompanyId, companyId);
		TradeRate rate = tradeRateMapper.selectOne(w);
		if (rate == null) {
			throw new ResourceException("评价不存在或无权限");
		}

		Map<String, Object> rateInfo = new LinkedHashMap<>();
		rateInfo.put("rate_id", rate.getRateId());
		rateInfo.put("company_id", rate.getCompanyId());
		rateInfo.put("item_id", rate.getItemId());
		rateInfo.put("goods_id", rate.getGoodsId());
		rateInfo.put("order_id", rate.getOrderId());
		rateInfo.put("user_id", rate.getUserId());
		rateInfo.put("rate_pic_num", rate.getRatePicNum());
		rateInfo.put("content", rate.getContent());
		rateInfo.put("content_len", rate.getContentLen());
		rateInfo.put("created", rate.getCreated());
		rateInfo.put("updated", rate.getUpdated());
		rateInfo.put("unionid", rate.getUnionid());
		rateInfo.put("item_spec_desc", rate.getItemSpecDesc());
		rateInfo.put("order_type", rate.getOrderType());
		String rawPic = rate.getRatePic();
		if (rawPic == null || rawPic.isBlank()) {
			rateInfo.put("rate_pic", new ArrayList<String>());
		} else {
			rateInfo.put(
					"rate_pic",
					Arrays.stream(rawPic.split(","))
							.map(String::trim)
							.filter(s -> !s.isEmpty())
							.toList());
		}
		rateInfo.put("is_reply", Boolean.TRUE.equals(rate.getIsReply()));
		rateInfo.put("disabled", Boolean.TRUE.equals(rate.getDisabled()));
		rateInfo.put("anonymous", Boolean.TRUE.equals(rate.getAnonymous()));
		rateInfo.put("star", rate.getStar());
		rateInfo.put("username", resolveMemberUsername(rate.getUserId(), companyId));

		Long orderIdLong = EspierAdminJwtControllerSupport.parseLongOrNull(rate.getOrderId());
		NormalOrdersItems orderLine = null;
		if (orderIdLong != null && rate.getItemId() != null) {
			LambdaQueryWrapper<NormalOrdersItems> ow = new LambdaQueryWrapper<>();
			ow.eq(NormalOrdersItems::getCompanyId, companyId)
					.eq(NormalOrdersItems::getOrderId, orderIdLong)
					.eq(NormalOrdersItems::getItemId, rate.getItemId());
			orderLine = normalOrdersItemsMapper.selectOne(ow);
		}

		Map<String, Object> itemRow;
		Long itemId = rate.getItemId();
		if (itemId == null) {
			itemRow = null;
		} else if ("pointsmall".equals(rate.getOrderType())) {
			itemRow = tradeRateMapper.selectPointsmallItemsRowMapByItemIdAndCompanyId(itemId, companyId);
		} else {
			itemRow = tradeRateMapper.selectItemsRowMapByItemIdAndCompanyId(itemId, companyId);
		}
		if (itemRow == null) {
			itemRow = new LinkedHashMap<>();
		}
		Map<String, Object> itemCopy = new LinkedHashMap<>(itemRow);
		itemCopy.put("total_fee", orderLine != null ? orderLine.getTotalFee() : null);
		if ("pointsmall".equals(rate.getOrderType())) {
			itemCopy.put("total_point", orderLine != null ? orderLine.getPoint() : null);
		}
		List<Map<String, Object>> itemInfo = Collections.singletonList(itemCopy);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("rateInfo", rateInfo);
		result.put("itemInfo", itemInfo);

		if (Boolean.TRUE.equals(rate.getIsReply())) {
			LambdaQueryWrapper<TradeRateReply> rw = new LambdaQueryWrapper<>();
			rw.eq(TradeRateReply::getCompanyId, companyId)
					.eq(TradeRateReply::getRateId, rateId)
					.eq(TradeRateReply::getRole, "seller");
			TradeRateReply sellerReply = tradeRateReplyMapper.selectOne(rw);
			if (sellerReply != null) {
				Map<String, Object> replyInfo = replyBaseMap(sellerReply);
				replyInfo.put(
						"operator_name", resolveOperatorName(sellerReply.getOperatorId(), companyId));
				result.put("replyInfo", replyInfo);
			}
		}

		LambdaQueryWrapper<TradeRateReply> bw = new LambdaQueryWrapper<>();
		bw.eq(TradeRateReply::getCompanyId, companyId)
				.eq(TradeRateReply::getRateId, rateId)
				.eq(TradeRateReply::getRole, "buyer")
				.orderByAsc(TradeRateReply::getCreated)
				.last("LIMIT 100");
		List<TradeRateReply> buyerRows = tradeRateReplyMapper.selectList(bw);
		List<Map<String, Object>> userReplyList;
		if (buyerRows.isEmpty()) {
			userReplyList = Collections.emptyList();
		} else {
			userReplyList = new ArrayList<>(buyerRows.size());
			for (TradeRateReply row : buyerRows) {
				Map<String, Object> m = replyBaseMap(row);
				m.put("username", resolveMemberUsername(row.getUserId(), companyId));
				userReplyList.add(m);
			}
		}
		result.put("userReply", userReplyList);

		return result;
	}

	private static Map<String, Object> replyBaseMap(TradeRateReply row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("reply_id", row.getReplyId());
		m.put("rate_id", row.getRateId());
		m.put("company_id", row.getCompanyId());
		m.put("user_id", row.getUserId());
		m.put("operator_id", row.getOperatorId());
		m.put("content", row.getContent());
		m.put("content_len", row.getContentLen());
		m.put("role", row.getRole());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		m.put("unionid", row.getUnionid());
		return m;
	}

	private String resolveMemberUsername(Long userId, long companyId) {
		if (userId == null || userId <= 0) {
			return "匿名";
		}
		Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
		String u = EspierAdminJwtControllerSupport.optionalTrimmedString(member.get("username"));
		return u != null ? u : "匿名";
	}

	private String resolveOperatorName(Long operatorId, long companyId) {
		if (operatorId == null || operatorId <= 0) {
			return "";
		}
		LambdaQueryWrapper<Operators> ow = new LambdaQueryWrapper<>();
		ow.eq(Operators::getOperatorId, operatorId).eq(Operators::getCompanyId, companyId);
		Operators op = operatorsMapper.selectOne(ow);
		if (op == null) {
			return "";
		}
		String username = op.getUsername();
		return username != null ? username : "";
	}
}
