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

package cn.shopex.ecshopx.kaquan.service.vipgrade;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeOrder;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeOrderMapper;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class VipGradeOrderReceiveMembercardPromotionService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private final VipGradeMapper vipGradeMapper;
	private final VipGradeOrderMapper vipGradeOrderMapper;
	private final VipGradeOrderMemberRelationApplyService vipGradeOrderMemberRelationApplyService;
	private final NormalOrderNumericIdService normalOrderNumericIdService;
	private final ObjectMapper objectMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final StringRedisTemplate companysRedisTemplate;

	public VipGradeOrderReceiveMembercardPromotionService(
			VipGradeMapper vipGradeMapper,
			VipGradeOrderMapper vipGradeOrderMapper,
			VipGradeOrderMemberRelationApplyService vipGradeOrderMemberRelationApplyService,
			NormalOrderNumericIdService normalOrderNumericIdService,
			ObjectMapper objectMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.vipGradeMapper = vipGradeMapper;
		this.vipGradeOrderMapper = vipGradeOrderMapper;
		this.vipGradeOrderMemberRelationApplyService = vipGradeOrderMemberRelationApplyService;
		this.normalOrderNumericIdService = normalOrderNumericIdService;
		this.objectMapper = objectMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public Map<String, Object> receiveMemberCardForPromotionReceive(
			long companyId, long userId, String mobilePlain, Map<String, Object> membercardSection) {
		Object vg = membercardSection.get("vip_grade_id");
		long vipGradeId = parsePositiveLong(vg);
		if (vipGradeId <= 0L) {
			throw new BadRequestException("缺少有效的 vip_grade_id");
		}
		if (vipGradeId > Integer.MAX_VALUE || vipGradeId < Integer.MIN_VALUE) {
			throw new BadRequestException("vip_grade_id 超出允许范围");
		}
		int vipGradeIdInt = (int) vipGradeId;

		VipGrade grade = vipGradeMapper.selectOne(new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, (int) companyId)
				.eq(VipGrade::getVipGradeId, (long) vipGradeIdInt));
		if (grade == null) {
			throw new ResourceException("没有该会员卡");
		}
		if (Boolean.TRUE.equals(grade.getIsDisabled())) {
			throw new ResourceException("会员卡已禁用");
		}

		Object cardTypeRaw = membercardSection.get("card_type");
		String cardTypeStr = cardTypeRaw == null ? "" : String.valueOf(cardTypeRaw).trim();

		String cardTypeJson;
		if ("custom".equals(cardTypeStr)) {
			int day = parseDayValue(membercardSection.get("day"));
			if (day <= 0) {
				throw new BadRequestException("自定义会员卡天数无效");
			}
			Map<String, Object> custom = new LinkedHashMap<>(4);
			custom.put("day", day);
			custom.put("desc", "后台手动赠送" + day + "天");
			custom.put("name", "custom");
			custom.put("price", 0);
			try {
				cardTypeJson = objectMapper.writeValueAsString(custom);
			} catch (JsonProcessingException e) {
				throw new ResourceException("没有该会员卡");
			}
		} else {
			Map<String, Object> matched = matchPriceListRow(grade.getPriceList(), cardTypeStr);
			if (matched == null) {
				throw new BadRequestException("Invalid member card type for configured price list.");
			}
			try {
				cardTypeJson = objectMapper.writeValueAsString(matched);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("Invalid member card type for configured price list.");
			}
		}

		int discount = VipGradeGradeDiscountParser.parseDiscountFromPrivilegesJson(grade.getPrivileges(), objectMapper);

		Integer price = 0;

		long orderId = normalOrderNumericIdService.generate(userId);
		int nowSec = (int) Instant.now().getEpochSecond();

		String mobileStored;
		if (mobilePlain != null && !mobilePlain.isBlank()) {
			mobileStored = sensitiveFieldEncryptor.encrypt(mobilePlain.trim());
		} else {
			mobileStored = "";
		}

		long distributorId = parseLongDefault(membercardSection.get("distributor_id"), 0L);
		long sourceId = parseLongDefault(membercardSection.get("source_id"), 0L);
		long monitorId = parseLongDefault(membercardSection.get("monitor_id"), 0L);

		VipGradeOrder order = new VipGradeOrder();
		order.setOrderId(orderId);
		order.setVipGradeId(vipGradeIdInt);
		order.setLvType(grade.getLvType());
		order.setCompanyId((int) companyId);
		order.setUserId(userId);
		order.setMobile(mobileStored);
		order.setTitle(grade.getGradeName());
		order.setPrice(price);
		order.setCardType(cardTypeJson);
		order.setDiscount(discount);
		order.setDistributorId(distributorId);
		order.setSourceId(sourceId == 0L ? null : sourceId);
		order.setMonitorId(monitorId == 0L ? null : monitorId);
		order.setOrderStatus("DONE");
		order.setSourceType("receive");
		order.setCreated(nowSec);
		order.setUpdated(nowSec);

		vipGradeOrderMapper.insert(order);

		Map<String, Object> redisPayload =
				vipGradeOrderMemberRelationApplyService.addMemberVipGrade(companyId, userId, orderId, true);
		String dateYmd = DateTimeFormatter.BASIC_ISO_DATE.format(Instant.now().atZone(SHANGHAI));
		String vipType = String.valueOf(redisPayload.get("vip_type"));
		String redisKey = "MemberCard:" + (int) companyId + ":" + vipType + ":" + dateYmd;
		companysRedisTemplate.opsForSet().add(redisKey, String.valueOf(userId));

		return buildOrderResponseMap(order, mobilePlain, cardTypeJson);
	}

	private Map<String, Object> buildOrderResponseMap(VipGradeOrder order, String mobilePlain, String cardTypeJson) {
		Map<String, Object> cardTypeObj = new LinkedHashMap<>();
		try {
			JsonNode n = objectMapper.readTree(cardTypeJson);
			if (n != null && n.isObject()) {
				cardTypeObj.putAll(objectMapper.convertValue(n, new TypeReference<Map<String, Object>>() {}));
			}
		} catch (JsonProcessingException e) {
			cardTypeObj.put("raw", cardTypeJson);
		}

		Map<String, Object> m = new LinkedHashMap<>(20);
		m.put("order_id", String.valueOf(order.getOrderId()));
		m.put("vip_grade_id", order.getVipGradeId());
		m.put("lv_type", order.getLvType());
		m.put("mobile", mobilePlain == null ? "" : mobilePlain);
		m.put("title", order.getTitle());
		m.put("price", order.getPrice());
		m.put("card_type", cardTypeObj);
		m.put("discount", order.getDiscount());
		m.put("distributor_id", order.getDistributorId() == null ? 0L : order.getDistributorId());
		m.put("source_id", order.getSourceId() == null ? "" : String.valueOf(order.getSourceId()));
		m.put("source_type", order.getSourceType());
		m.put("monitor_id", order.getMonitorId() == null ? "" : String.valueOf(order.getMonitorId()));
		m.put("order_status", order.getOrderStatus());
		m.put("created", order.getCreated());
		m.put("updated", order.getUpdated());
		m.put("fee_type", order.getFeeType());
		double feeRate = order.getFeeRate() == null ? 0d : order.getFeeRate();
		m.put("fee_rate", (int) Math.round(feeRate));
		m.put("fee_symbol", order.getFeeSymbol());
		return m;
	}

	private Map<String, Object> matchPriceListRow(String priceListJson, String cardTypeName) {
		if (priceListJson == null || priceListJson.isBlank() || cardTypeName.isBlank()) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(priceListJson);
			if (root == null || !root.isArray()) {
				return null;
			}
			for (JsonNode row : root) {
				if (!row.isObject()) {
					continue;
				}
				JsonNode nameNode = row.get("name");
				if (nameNode == null || nameNode.isNull()) {
					continue;
				}
				if (cardTypeName.equals(nameNode.asText())) {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = objectMapper.convertValue(row, new TypeReference<Map<String, Object>>() {});
					return map;
				}
			}
		} catch (JsonProcessingException e) {
			return null;
		}
		return null;
	}

	private static long parsePositiveLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parseDayValue(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long parseLongDefault(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

}
