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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderDetailItemGoodsLookupPort;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.OrderTradeInfoPhpParityMaps;
import cn.shopex.ecshopx.orders.service.normal.OrderZitiQrCodeRedisService;
import cn.shopex.ecshopx.orders.service.orderexport.support.NormalOrderExportDistributorLookupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2OrderDetailService {

	private final OrderZitiQrCodeRedisService orderZitiQrCodeRedisService;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final NormalOrderExportDistributorLookupService distributorLookupService;
	private final TradeMapper tradeMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final MembersMapper membersMapper;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final WechatUsersMapper wechatUsersMapper;
	private final OpenapiOrderDetailItemGoodsLookupPort itemGoodsLookupPort;
	private final OpenapiThirdApiV2OrderDetailFormatSupport formatSupport;
	private final OpenapiThirdApiV2OrderListFormatSupport listFormatSupport;

	public OpenapiThirdApiV2OrderDetailService(
			OrderZitiQrCodeRedisService orderZitiQrCodeRedisService,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			NormalOrderExportDistributorLookupService distributorLookupService,
			TradeMapper tradeMapper,
			MembersInfoMapper membersInfoMapper,
			MembersMapper membersMapper,
			MembersAssociationsMapper membersAssociationsMapper,
			WechatUsersMapper wechatUsersMapper,
			OpenapiOrderDetailItemGoodsLookupPort itemGoodsLookupPort,
			OpenapiThirdApiV2OrderDetailFormatSupport formatSupport,
			OpenapiThirdApiV2OrderListFormatSupport listFormatSupport) {
		this.orderZitiQrCodeRedisService = orderZitiQrCodeRedisService;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.distributorLookupService = distributorLookupService;
		this.tradeMapper = tradeMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.membersMapper = membersMapper;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.wechatUsersMapper = wechatUsersMapper;
		this.itemGoodsLookupPort = itemGoodsLookupPort;
		this.formatSupport = formatSupport;
		this.listFormatSupport = listFormatSupport;
	}

	public Map<String, Object> execute(long companyId, String orderIdRaw, String zitiCodeRaw) {
		if (isPhpFalsy(orderIdRaw) && isPhpFalsy(zitiCodeRaw)) {
			throw new ResourceException("请填写订单编号");
		}

		String orderIdStr;
		if (isPhpFalsy(orderIdRaw)) {
			String zitiCode = zitiCodeRaw;
			if (zitiCode != null && zitiCode.indexOf("ZT_") > 0) {
				zitiCode = zitiCode.substring(3);
			}
			Long mapped = orderZitiQrCodeRedisService.resolveOrderIdNullable(zitiCode);
			if (mapped == null) {
				throw new ResourceException("订单相应的明细不存在");
			}
			orderIdStr = String.valueOf(mapped);
		} else {
			orderIdStr = orderIdRaw.trim();
		}

		Map<String, Object> bundle;
		try {
			bundle = adminNormalOrderDetailService.buildOrderBundle(companyId, orderIdStr, false);
		} catch (BadRequestException e) {
			String msg = e.getMessage();
			if (msg != null && msg.startsWith("订单号为")) {
				throw new OpenapiLegacyZeroCodeFailException(msg);
			}
			throw e;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfo = (Map<String, Object>) bundle.get("orderInfo");

		long distributorId = longVal(orderInfo.get("distributor_id"));
		String shopCode = resolveShopCode(companyId, distributorId);

		long orderIdNum = longVal(orderInfo.get("order_id"));
		String tradeNo = resolveTradeNo(companyId, orderIdNum);
		String payTime = resolvePayTime(companyId, orderIdStr);

		long userId = longVal(orderInfo.get("user_id"));
		MemberWechatInfo memberWechatInfo = loadMemberWechatInfo(companyId, userId);

		List<Long> itemIds = extractItemIds(orderInfo);
		Map<Long, Long> goodsIdByItemId =
				itemIds.isEmpty() ? Map.of() : itemGoodsLookupPort.lookupGoodsIds(companyId, itemIds);

		return formatSupport.formatOrderInfoStruct(
				companyId,
				orderInfo,
				shopCode,
				tradeNo,
				payTime,
				memberWechatInfo.username(),
				memberWechatInfo.unionid(),
				memberWechatInfo.openid(),
				goodsIdByItemId,
				listFormatSupport);
	}

	private String resolveShopCode(long companyId, long distributorId) {
		if (distributorId <= 0L) {
			return null;
		}
		Map<Long, NormalOrderExportDistributorLookupService.StoreInfo> storeMap =
				distributorLookupService.loadStores(companyId, List.of(distributorId));
		NormalOrderExportDistributorLookupService.StoreInfo store = storeMap.get(distributorId);
		return store != null ? store.shopCode() : null;
	}

	private String resolveTradeNo(long companyId, long orderIdNum) {
		if (orderIdNum <= 0L) {
			return "-";
		}
		List<Map<String, Object>> rows =
				tradeMapper.selectSuccessTradeIndexRows(companyId, List.of(orderIdNum));
		if (rows == null || rows.isEmpty()) {
			return "-";
		}
		String tn = str(rows.get(0).get("trade_no"));
		return (StringUtils.hasText(tn) && !"0".equals(tn)) ? tn : "-";
	}

	private String resolvePayTime(long companyId, String orderIdStr) {
		Trade trade =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getOrderId, orderIdStr)
								.eq(Trade::getTradeState, "SUCCESS")
								.last("LIMIT 1"));
		if (trade == null) {
			return "";
		}
		return OrderTradeInfoPhpParityMaps.formatTradePayDate(trade.getTimeExpire());
	}

	private MemberWechatInfo loadMemberWechatInfo(long companyId, long userId) {
		if (userId <= 0L) {
			return MemberWechatInfo.EMPTY;
		}
		MembersInfo info =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.eq(MembersInfo::getUserId, userId)
								.last("LIMIT 1"));
		if (info == null) {
			return MemberWechatInfo.EMPTY;
		}
		String username = nz(info.getUsername());
		Members member =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		MembersAssociations asso =
				membersAssociationsMapper.selectOne(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.eq(MembersAssociations::getUserId, userId)
								.last("LIMIT 1"));
		if (asso == null || !StringUtils.hasText(asso.getUnionid()) || member == null) {
			return new MemberWechatInfo(username, "", "");
		}
		WechatUsers wu =
				wechatUsersMapper.selectOne(
						new LambdaQueryWrapper<WechatUsers>()
								.eq(WechatUsers::getCompanyId, companyId)
								.eq(WechatUsers::getUnionid, asso.getUnionid())
								.eq(WechatUsers::getAuthorizerAppid, member.getWxaAppid())
								.last("LIMIT 1"));
		if (wu == null) {
			return new MemberWechatInfo(username, "", "");
		}
		return new MemberWechatInfo(username, nz(wu.getUnionid()), nz(wu.getOpenId()));
	}

	private static List<Long> extractItemIds(Map<String, Object> orderInfo) {
		Object itemsObj = orderInfo.get("items");
		if (!(itemsObj instanceof List<?> items)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object itemObj : items) {
			if (!(itemObj instanceof Map<?, ?> rawItem)) {
				continue;
			}
			long itemId = longVal(rawItem.get("item_id"));
			if (itemId > 0L) {
				out.add(itemId);
			}
		}
		return out;
	}

	private static boolean isPhpFalsy(String raw) {
		if (raw == null) {
			return true;
		}
		String t = raw.trim();
		return t.isEmpty() || "0".equals(t);
	}

	private static long longVal(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}

	private static String nz(String value) {
		return value != null ? value : "";
	}

	private record MemberWechatInfo(String username, String unionid, String openid) {
		private static final MemberWechatInfo EMPTY = new MemberWechatInfo("", "", "");
	}
}
