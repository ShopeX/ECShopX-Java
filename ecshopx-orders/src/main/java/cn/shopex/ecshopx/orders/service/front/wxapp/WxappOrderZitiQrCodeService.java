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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.setting.PickupcodeSettingRedisService;
import cn.shopex.ecshopx.members.service.wxapp.MemberBarcodeGenerateService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.normal.OrderZitiQrCodeRedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappOrderZitiQrCodeService {

	private static final Pattern NUMERIC_STRING_PATTERN =
			Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");

	private static final Pattern INTEGER_STRING = Pattern.compile("^-?\\d+$");

	private final NormalOrdersMapper normalOrdersMapper;

	private final OrderZitiQrCodeRedisService orderZitiQrCodeRedisService;

	private final MemberBarcodeGenerateService memberBarcodeGenerateService;

	private final PickupcodeSettingRedisService pickupcodeSettingRedisService;

	private final AdminNormalOrderDetailService adminNormalOrderDetailService;

	private final StringRedisTemplate companysRedisTemplate;

	public WxappOrderZitiQrCodeService(
			NormalOrdersMapper normalOrdersMapper,
			OrderZitiQrCodeRedisService orderZitiQrCodeRedisService,
			MemberBarcodeGenerateService memberBarcodeGenerateService,
			PickupcodeSettingRedisService pickupcodeSettingRedisService,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.orderZitiQrCodeRedisService = orderZitiQrCodeRedisService;
		this.memberBarcodeGenerateService = memberBarcodeGenerateService;
		this.pickupcodeSettingRedisService = pickupcodeSettingRedisService;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public WxappOrderZitiQrCodeResult getZitiQRCode(
			long companyId, Object jwtUserId, String orderTypeRaw, String orderIdRaw) {
		String dispatchKey = normalizeOrderTypeKey(orderTypeRaw);
		if (!isZiticodeDispatchAllowed(dispatchKey)) {
			throw new ResourceException("无此类型订单！");
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("user_id", "");
		result.put("barcode_url", "");
		result.put("qrcode_url", "");
		result.put("code", "");

		if (isNumericOrderIdString(orderIdRaw)) {
			String trimmed = orderIdRaw.trim();
			long orderIdNum = new BigDecimal(trimmed).longValue();
			NormalOrders order =
					normalOrdersMapper.selectOne(
							new LambdaQueryWrapper<NormalOrders>()
									.eq(NormalOrders::getCompanyId, companyId)
									.eq(NormalOrders::getOrderId, orderIdNum)
									.last("LIMIT 1"));
			if (order != null) {
				String sixDigits = randomSixDigitSuffix();
				long zitiBase = order.getZitiCode() == null ? 0L : order.getZitiCode().longValue();
				String fullCode = String.valueOf(zitiBase) + sixDigits;
				orderZitiQrCodeRedisService.saveZitiCodeOrderMapping(
						fullCode, orderIdNum, Duration.ofSeconds(300));
				String content = "ZT_" + fullCode;
				Map<String, String> images =
						memberBarcodeGenerateService.generateBarcodeAndQrcodeDataUrlsForPlainContent(content);
				result.put("barcode_url", images.get("barcode_url"));
				result.put("qrcode_url", images.get("qrcode_url"));
				result.put("code", fullCode);
				result.put("user_id", order.getUserId());
				result.put("ziti_status", order.getZitiStatus() == null ? "" : order.getZitiStatus());
			}
		}

		applyShowSmsPickupCode(companyId, orderIdRaw, result);

		Object resultUserId = result.get("user_id");
		if (looseTypedNotEqual(resultUserId, jwtUserId)) {
			return new WxappOrderZitiQrCodeResult(true, null);
		}
		return new WxappOrderZitiQrCodeResult(false, result);
	}

	private static String normalizeOrderTypeKey(String orderTypeRaw) {
		String t = orderTypeRaw == null ? "" : orderTypeRaw.trim();
		if (t.isEmpty()) {
			t = "normal";
		}
		return t.toLowerCase(Locale.ROOT);
	}

	private static boolean isZiticodeDispatchAllowed(String dispatchKey) {
		return switch (dispatchKey) {
			case "normal",
					"bargain",
					"normal_bargain",
					"normal_groups",
					"normal_seckill",
					"service_seckill",
					"normal_drug",
					"normal_shopguide",
					"normal_pointsmall",
					"normal_excard",
					"normal_community",
					"normal_shopadmin",
					"normal_employee_purchase" -> true;
			case "service", "groups", "service_groups", "supplier_order", "membercard" -> false;
			default -> false;
		};
	}

	private static boolean isNumericOrderIdString(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		return NUMERIC_STRING_PATTERN.matcher(t).matches();
	}

	private static String randomSixDigitSuffix() {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		int first = 1 + r.nextInt(9);
		int rest = r.nextInt(100_000);
		return first + String.format("%05d", rest);
	}

	private void applyShowSmsPickupCode(long companyId, String orderIdRaw, LinkedHashMap<String, Object> result) {
		Map<String, Object> setting = pickupcodeSettingRedisService.handle(companyId, null);
		boolean pickupOn = toPickupStatus(setting.get("pickupcode_status"));
		if (!pickupOn) {
			result.put("pickup_code", null);
			return;
		}

		Map<String, Object> bundle;
		try {
			bundle =
					adminNormalOrderDetailService.buildOrderBundle(
							companyId, orderIdRaw == null ? "" : orderIdRaw, false, "api");
		} catch (BadRequestException e) {
			String msg = e.getMessage();
			if (msg != null && (msg.contains("的订单不存在") || msg.contains("参数错误"))) {
				throw new ResourceException("订单不存在");
			}
			throw e;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfo = (Map<String, Object>) bundle.get("orderInfo");
		if (orderInfo == null || orderInfo.isEmpty()) {
			result.put("pickup_code", null);
			return;
		}
		String phone = orderInfo.get("mobile") == null ? "" : String.valueOf(orderInfo.get("mobile")).trim();
		if (phone.isEmpty()) {
			result.put("pickup_code", null);
			return;
		}

		long orderIdNumForPickup = parseOrderIdLong(orderInfo.get("order_id"));
		String key = "admin-pickupcode:" + orderIdNumForPickup + "|" + phone;

		String existing = companysRedisTemplate.opsForValue().get(key);
		if (StringUtils.hasText(existing)) {
			result.put("pickup_code", existing);
			return;
		}

		long ttl = companysRedisTemplate.getExpire(key, TimeUnit.SECONDS);
		if (ttl - 240L > 0L) {
			long wait = ttl - 240L;
			throw new ResourceException("发送过于频繁，请 " + wait + " 秒后再试");
		}

		int n = 100_000 + ThreadLocalRandom.current().nextInt(900_000);
		String vcode = String.valueOf(n);
		companysRedisTemplate.opsForValue().set(key, vcode, Duration.ofSeconds(1800));
		result.put("pickup_code", vcode);
	}

	private static long parseOrderIdLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		return Long.parseLong(s);
	}

	private static boolean toPickupStatus(Object raw) {
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		return "true".equalsIgnoreCase(String.valueOf(raw).trim());
	}

	private static boolean looseTypedNotEqual(Object a, Object b) {
		String sa = a == null ? "" : String.valueOf(a).trim();
		String sb = b == null ? "" : String.valueOf(b).trim();
		if (INTEGER_STRING.matcher(sa).matches() && INTEGER_STRING.matcher(sb).matches()) {
			return !new BigInteger(sa).equals(new BigInteger(sb));
		}
		return !sa.equals(sb);
	}
}
