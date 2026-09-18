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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.mapper.MerchantMapper;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BspayTradeInfoService {

	private final TradeMapper tradeMapper;
	private final BspayTradeListForInfoQueryService bspayTradeListForInfoQueryService;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final BspayTradeRefundSnippetQueryService bspayTradeRefundSnippetQueryService;
	private final BspayDivFeeInfoAssembler bspayDivFeeInfoAssembler;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final MerchantMapper merchantMapper;

	public BspayTradeInfoService(
			TradeMapper tradeMapper,
			BspayTradeListForInfoQueryService bspayTradeListForInfoQueryService,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			BspayTradeRefundSnippetQueryService bspayTradeRefundSnippetQueryService,
			BspayDivFeeInfoAssembler bspayDivFeeInfoAssembler,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			MerchantMapper merchantMapper) {
		this.tradeMapper = tradeMapper;
		this.bspayTradeListForInfoQueryService = bspayTradeListForInfoQueryService;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.bspayTradeRefundSnippetQueryService = bspayTradeRefundSnippetQueryService;
		this.bspayDivFeeInfoAssembler = bspayDivFeeInfoAssembler;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.merchantMapper = merchantMapper;
	}

	public static long parseJwtCompanyIdOrThrow(Map<String, Object> jwtMap) {
		Object v = jwtMap.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("未登录");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	public Object getTradeInfo(String tradeId, Map<String, Object> jwtMap) {
		Trade trade = tradeMapper.selectById(tradeId);
		if (trade == null) {
			return Collections.emptyList();
		}
		long jwtCompanyId = parseJwtCompanyIdOrThrow(jwtMap);
		long tradeCompanyId = parseCompanyIdLong(trade.getCompanyId());
		if (jwtCompanyId != tradeCompanyId) {
			throw new ForbiddenException("无权访问该交易单");
		}
		List<Map<String, Object>> aggRows =
				bspayTradeListForInfoQueryService.listAggregatedForOrder(tradeCompanyId, trade.getOrderId());
		String tradeStateForPayload;
		if (aggRows == null || aggRows.isEmpty()) {
			tradeStateForPayload = trade.getTradeState() != null ? trade.getTradeState() : "";
		} else {
			Object ts = aggRows.get(0).get("tradeState");
			tradeStateForPayload = ts != null ? ts.toString() : "";
		}
		Map<String, Object> out = tradeToSnakeMap(trade);
		out.put("trade_state", tradeStateForPayload);
		out.put("refund_list", bspayTradeRefundSnippetQueryService.listRefundSnippets(tradeCompanyId, tradeId));
		out.remove("inital_request");
		out.remove("inital_response");
		putUsername(out, trade, tradeCompanyId);
		out.put("div_fee_info", bspayDivFeeInfoAssembler.build(trade, jwtMap));
		out.put("distributor_name", resolveDistributorName(tradeCompanyId, trade.getDistributorId()));
		String merName;
		Long mid = trade.getMerchantId();
		if (mid != null && mid > 0) {
			Merchant merchant = merchantMapper.selectById(mid);
			merName = (merchant != null && merchant.getMerchantName() != null && !merchant.getMerchantName().isEmpty())
					? merchant.getMerchantName()
					: "-";
		} else {
			merName = "-";
		}
		out.put("mer_name", merName);
		return out;
	}

	private void putUsername(Map<String, Object> out, Trade trade, long tradeCompanyId) {
		Long userId = parseLongOrNull(trade.getUserId());
		if (userId == null) {
			out.put("username", "");
			return;
		}
		Members m = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getUserId, userId)
				.eq(Members::getCompanyId, Long.valueOf(tradeCompanyId))
				.last("LIMIT 1"));
		if (m == null) {
			out.put("username", "");
			return;
		}
		MembersInfo mi = membersInfoMapper.selectOne(new LambdaQueryWrapper<MembersInfo>()
				.eq(MembersInfo::getUserId, userId)
				.eq(MembersInfo::getCompanyId, Long.valueOf(tradeCompanyId))
				.last("LIMIT 1"));
		out.put("username", mi != null && mi.getUsername() != null ? mi.getUsername() : "");
	}

	private String resolveDistributorName(long tradeCompanyId, String distributorIdRaw) {
		Long did = parseLongOrNull(distributorIdRaw);
		if (did == null || did <= 0L) {
			return "自营";
		}
		Object info = distributorRepositoryGetInfoSimpleService.getInfoSimple(tradeCompanyId, String.valueOf(did));
		if (info instanceof Map<?, ?> map) {
			Object name = map.get("name");
			if (name != null && !name.toString().isEmpty()) {
				return name.toString();
			}
		}
		return "自营";
	}

	private static Map<String, Object> tradeToSnakeMap(Trade t) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("trade_id", t.getTradeId());
		m.put("order_id", t.getOrderId());
		m.put("company_id", t.getCompanyId());
		m.put("shop_id", t.getShopId());
		m.put("distributor_id", t.getDistributorId());
		m.put("dealer_id", t.getDealerId());
		m.put("trade_source_type", t.getTradeSourceType());
		m.put("user_id", t.getUserId());
		m.put("mobile", t.getMobile());
		m.put("open_id", t.getOpenId());
		m.put("discount_info", t.getDiscountInfo());
		m.put("mch_id", t.getMchId());
		m.put("total_fee", t.getTotalFee());
		m.put("discount_fee", t.getDiscountFee());
		m.put("fee_type", t.getFeeType());
		m.put("pay_fee", t.getPayFee());
		m.put("trade_no", t.getTradeNo());
		m.put("trade_state", t.getTradeState());
		m.put("pay_type", t.getPayType());
		m.put("pay_channel", t.getPayChannel());
		m.put("transaction_id", t.getTransactionId());
		m.put("authorizer_appid", t.getAuthorizerAppid());
		m.put("wxa_appid", t.getWxaAppid());
		m.put("bank_type", t.getBankType());
		m.put("body", t.getBody());
		m.put("detail", t.getDetail());
		m.put("time_start", t.getTimeStart());
		m.put("time_expire", t.getTimeExpire());
		m.put("div_members", t.getDivMembers());
		m.put("refunded_fee", t.getRefundedFee());
		m.put("adapay_fee_mode", t.getAdapayFeeMode());
		m.put("adapay_fee", t.getAdapayFee());
		m.put("adapay_div_status", t.getAdapayDivStatus());
		m.put("cur_fee_type", t.getCurFeeType());
		m.put("cur_fee_rate", t.getCurFeeRate());
		m.put("cur_fee_symbol", t.getCurFeeSymbol());
		m.put("cur_pay_fee", t.getCurPayFee());
		m.put("coupon_fee", t.getCouponFee());
		m.put("coupon_info", t.getCouponInfo());
		m.put("inital_request", t.getInitalRequest());
		m.put("inital_response", t.getInitalResponse());
		m.put("merchant_id", t.getMerchantId());
		m.put("is_settled", t.getIsSettled());
		m.put("payment_params", t.getPaymentParams());
		m.put("supplier_id", t.getSupplierId());
		m.put("bspay_req_date", t.getBspayReqDate());
		m.put("bspay_div_members", t.getBspayDivMembers());
		m.put("bspay_div_status", t.getBspayDivStatus());
		m.put("bspay_fee_mode", t.getBspayFeeMode());
		m.put("bspay_fee", t.getBspayFee());
		return m;
	}

	private static long parseCompanyIdLong(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long parseLongOrNull(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
