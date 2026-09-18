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

package cn.shopex.ecshopx.promotions.service.bargain;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.BargainOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.BargainOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.promotions.domain.BargainLog;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.UserBargains;
import cn.shopex.ecshopx.promotions.mapper.BargainLogMapper;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import cn.shopex.ecshopx.promotions.service.BargainPromotionCreateService;
import cn.shopex.ecshopx.promotions.service.multilang.BargainPromotionMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserBargainGetUserBargainService {

	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;
	private final BargainPromotionsMapper bargainPromotionsMapper;
	private final BargainPromotionCreateService bargainPromotionCreateService;
	private final BargainPromotionMultiLangReadService bargainPromotionMultiLangReadService;
	private final JdbcTemplate jdbcTemplate;
	private final UserBargainsMapper userBargainsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final BargainOrdersMapper bargainOrdersMapper;
	private final BargainLogMapper bargainLogMapper;
	private final MemberAccountService memberAccountService;

	public UserBargainGetUserBargainService(
			MessageSource messageSource,
			ObjectMapper objectMapper,
			BargainPromotionsMapper bargainPromotionsMapper,
			BargainPromotionCreateService bargainPromotionCreateService,
			BargainPromotionMultiLangReadService bargainPromotionMultiLangReadService,
			JdbcTemplate jdbcTemplate,
			UserBargainsMapper userBargainsMapper,
			NormalOrdersMapper normalOrdersMapper,
			BargainOrdersMapper bargainOrdersMapper,
			BargainLogMapper bargainLogMapper,
			MemberAccountService memberAccountService) {
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
		this.bargainPromotionsMapper = bargainPromotionsMapper;
		this.bargainPromotionCreateService = bargainPromotionCreateService;
		this.bargainPromotionMultiLangReadService = bargainPromotionMultiLangReadService;
		this.jdbcTemplate = jdbcTemplate;
		this.userBargainsMapper = userBargainsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.bargainOrdersMapper = bargainOrdersMapper;
		this.bargainLogMapper = bargainLogMapper;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> getUserBargain(
			long companyId,
			long authUserIdFromClaims,
			long resolvedUserId,
			long bargainId,
			boolean hasOrder,
			String acceptLanguage,
			Locale locale) {
		BargainPromotions entity = bargainPromotionsMapper.selectById(bargainId);
		if (entity == null
				|| entity.getCompanyId() == null
				|| !Objects.equals(entity.getCompanyId(), companyId)) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.activity_not_exist", null, locale));
		}

		long nowEpochSeconds = Instant.now().getEpochSecond();
		Map<String, Object> bargainInfo =
				bargainPromotionCreateService.toBargainListRow(entity, nowEpochSeconds);
		String itemIdRaw = entity.getItemId();
		if (StringUtils.hasText(itemIdRaw)) {
			try {
				long itemIdLong = Long.parseLong(itemIdRaw.trim());
				if (itemIdLong > 0L) {
					String intro = selectItemIntro(companyId, itemIdLong);
					if (intro != null) {
						bargainInfo.put("item_intro", intro);
					}
				}
			} catch (NumberFormatException ignored) {
				// omit item_intro when item id is not a valid number
			}
		}
		bargainPromotionMultiLangReadService.applyTitleAndAdPic(
				Collections.singletonList(bargainInfo), acceptLanguage);
		long endSec = entity.getEndTime() == null ? 0L : entity.getEndTime();
		bargainInfo.put("left_micro_second", (endSec - nowEpochSeconds) * 1000L);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("bargain_info", bargainInfo);

		LambdaQueryWrapper<UserBargains> ubW =
				new LambdaQueryWrapper<UserBargains>()
						.eq(UserBargains::getCompanyId, companyId)
						.eq(UserBargains::getBargainId, bargainId)
						.eq(UserBargains::getUserId, resolvedUserId)
						.last("LIMIT 1");
		UserBargains ubRow = userBargainsMapper.selectOne(ubW);
		if (ubRow == null) {
			result.put("user_bargain_info", new ArrayList<>());
			return result;
		}

		result.put("user_bargain_info", userBargainRowToMap(ubRow));

		if (Boolean.TRUE.equals(ubRow.getIsOrdered())) {
			LambdaQueryWrapper<NormalOrders> noW =
					new LambdaQueryWrapper<NormalOrders>()
							.eq(NormalOrders::getUserId, ubRow.getUserId())
							.eq(NormalOrders::getActId, ubRow.getBargainId())
							.eq(NormalOrders::getCompanyId, ubRow.getCompanyId())
							.eq(NormalOrders::getOrderClass, "bargain")
							.orderByDesc(NormalOrders::getCreateTime)
							.last("LIMIT 1");
			NormalOrders normal = normalOrdersMapper.selectOne(noW);
			if (normal == null) {
				result.put("bargain_order", Boolean.FALSE);
			} else {
				Map<String, Object> snap = new LinkedHashMap<>();
				snap.put("order_id", normal.getOrderId());
				snap.put("order_status", normal.getOrderStatus());
				result.put("bargain_order", snap);
			}
		}

		Map<String, Object> userInfo =
				memberAccountService.getWechatUserInfo(
						Map.of("company_id", companyId, "user_id", resolvedUserId));
		if (userInfo == null || userInfo.isEmpty()) {
			result.put("user_info", new ArrayList<>());
		} else {
			result.put("user_info", userInfo);
		}

		LambdaQueryWrapper<BargainLog> logCountW =
				new LambdaQueryWrapper<BargainLog>()
						.eq(BargainLog::getBargainId, bargainId)
						.eq(BargainLog::getUserId, resolvedUserId);
		long logTotal = bargainLogMapper.selectCount(logCountW);
		Map<String, Object> bargainLogOut = new LinkedHashMap<>();
		bargainLogOut.put("total_count", (int) logTotal);
		if (logTotal > 0L) {
			LambdaQueryWrapper<BargainLog> logListW =
					new LambdaQueryWrapper<BargainLog>()
							.eq(BargainLog::getBargainId, bargainId)
							.eq(BargainLog::getUserId, resolvedUserId)
							.orderByDesc(BargainLog::getCreated);
			List<BargainLog> logs = bargainLogMapper.selectList(logListW);
			List<Map<String, Object>> list = new ArrayList<>();
			for (BargainLog log : logs) {
				list.add(bargainLogRowToMap(log));
			}
			bargainLogOut.put("list", list);
		}
		result.put("bargain_log", bargainLogOut);

		if (hasOrder) {
			LambdaQueryWrapper<BargainOrders> boCountW =
					new LambdaQueryWrapper<BargainOrders>().eq(BargainOrders::getBargainId, bargainId);
			if (resolvedUserId != 0L) {
				boCountW = boCountW.eq(BargainOrders::getUserId, resolvedUserId);
			}
			long boTotal = bargainOrdersMapper.selectCount(boCountW);
			if (boTotal > 0L) {
				LambdaQueryWrapper<BargainOrders> boOneW =
						new LambdaQueryWrapper<BargainOrders>().eq(BargainOrders::getBargainId, bargainId);
				if (resolvedUserId != 0L) {
					boOneW = boOneW.eq(BargainOrders::getUserId, resolvedUserId);
				}
				boOneW.orderByDesc(BargainOrders::getCreateTime).last("LIMIT 1");
				BargainOrders first = bargainOrdersMapper.selectOne(boOneW);
				if (first != null) {
					result.put("bargain_order", bargainOrderEntityToSnakeMap(first));
				}
			}
		}

		return result;
	}

	private String selectItemIntro(long companyId, long itemId) {
		try {
			return jdbcTemplate.queryForObject(
					"SELECT intro FROM items WHERE item_id = ? AND company_id = ? LIMIT 1",
					String.class,
					itemId,
					companyId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	private Map<String, Object> bargainLogRowToMap(BargainLog log) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("bargain_log_id", log.getBargainLogId());
		m.put("company_id", log.getCompanyId());
		m.put("authorizer_appid", log.getAuthorizerAppid());
		m.put("wxa_appid", log.getWxaAppid());
		m.put("bargain_id", log.getBargainId());
		m.put("user_id", log.getUserId());
		m.put("open_id", log.getOpenId());
		m.put("nickname", log.getNickname());
		m.put("headimgurl", log.getHeadimgurl());
		m.put("cutdown_num", log.getCutdownNum());
		m.put("created", log.getCreated());
		m.put("updated", log.getUpdated());
		return m;
	}

	private Map<String, Object> userBargainRowToMap(UserBargains ub) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", ub.getCompanyId());
		m.put("authorizer_appid", ub.getAuthorizerAppid());
		m.put("wxa_appid", ub.getWxaAppid());
		m.put("bargain_id", ub.getBargainId());
		m.put("user_id", ub.getUserId());
		m.put("item_name", ub.getItemName());
		m.put("mkt_price", ub.getMktPrice());
		m.put("price", ub.getPrice());
		m.put("cutprice_num", ub.getCutpriceNum());
		m.put("cutprice_range", parseCutpriceRange(ub.getCutpriceRange()));
		m.put("cutdown_amount", ub.getCutdownAmount());
		m.put("is_ordered", ub.getIsOrdered());
		m.put("created", ub.getCreated());
		m.put("updated", ub.getUpdated());
		return m;
	}

	private Object parseCutpriceRange(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return null;
		}
		String t = raw.trim();
		try {
			JsonNode node = objectMapper.readTree(t);
			if (node == null || node.isNull()) {
				return null;
			}
			if (node.isArray()) {
				return objectMapper.convertValue(node, new TypeReference<List<Object>>() {});
			}
			if (node.isObject()) {
				return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
			}
		} catch (JsonProcessingException e) {
			throw new ResourceException("数据解析失败");
		}
		throw new ResourceException("数据解析失败");
	}

	private Object parseDiscountDescJson(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return new ArrayList<>();
		}
		String t = raw.trim();
		try {
			JsonNode node = objectMapper.readTree(t);
			if (node == null || node.isNull()) {
				return new ArrayList<>();
			}
			if (node.isArray()) {
				return objectMapper.convertValue(node, new TypeReference<List<Object>>() {});
			}
			if (node.isObject()) {
				return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
			}
		} catch (JsonProcessingException e) {
			throw new ResourceException("数据解析失败");
		}
		throw new ResourceException("数据解析失败");
	}

	private Map<String, Object> bargainOrderEntityToSnakeMap(BargainOrders o) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", o.getOrderId());
		m.put("title", o.getTitle());
		m.put("company_id", o.getCompanyId());
		m.put("user_id", o.getUserId());
		m.put("bargain_id", o.getBargainId());
		m.put("item_name", o.getItemName());
		m.put("item_price", o.getItemPrice());
		m.put("item_pics", o.getItemPics());
		m.put("item_num", o.getItemNum());
		m.put("templates_id", o.getTemplatesId());
		m.put("mobile", o.getMobile());
		m.put("item_fee", o.getItemFee());
		m.put("total_fee", o.getTotalFee());
		m.put("freight_fee", o.getFreightFee());
		m.put("order_status", o.getOrderStatus());
		m.put("order_type", o.getOrderType());
		m.put("order_source", o.getOrderSource());
		m.put("receiver_name", o.getReceiverName());
		m.put("receiver_mobile", o.getReceiverMobile());
		m.put("receiver_zip", o.getReceiverZip());
		m.put("receiver_state", o.getReceiverState());
		m.put("receiver_city", o.getReceiverCity());
		m.put("receiver_district", o.getReceiverDistrict());
		m.put("receiver_address", o.getReceiverAddress());
		m.put("create_time", o.getCreateTime());
		m.put("update_time", o.getUpdateTime());
		m.put("auto_cancel_time", o.getAutoCancelTime());
		m.put("source_id", o.getSourceId());
		m.put("monitor_id", o.getMonitorId());
		m.put("remark", o.getRemark());
		m.put("member_discount", o.getMemberDiscount());
		m.put("coupon_discount", o.getCouponDiscount());
		m.put("coupon_discount_desc", parseDiscountDescJson(o.getCouponDiscountDesc()));
		m.put("member_discount_desc", parseDiscountDescJson(o.getMemberDiscountDesc()));
		m.put("fee_type", o.getFeeType());
		m.put("fee_rate", o.getFeeRate());
		m.put("fee_symbol", o.getFeeSymbol());
		m.put("third_params", o.getThirdParams());
		return m;
	}
}
