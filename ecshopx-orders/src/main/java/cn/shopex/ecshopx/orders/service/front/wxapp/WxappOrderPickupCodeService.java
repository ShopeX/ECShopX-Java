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
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.operator.sms.CompanySceneSmsSendPort;
import cn.shopex.ecshopx.companys.service.setting.PickupcodeSettingRedisService;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminEntityOrderDetailTypePolicy;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationEffectiveTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderPickupCodeService {

	private static final Logger log = LoggerFactory.getLogger(WxappOrderPickupCodeService.class);

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final AdminEntityOrderDetailTypePolicy adminEntityOrderDetailTypePolicy;
	private final PickupcodeSettingRedisService pickupcodeSettingRedisService;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final CompanySceneSmsSendPort companySceneSmsSendPort;
	private final StringRedisTemplate companysRedisTemplate;

	public WxappOrderPickupCodeService(
			OrderAssociationsMapper orderAssociationsMapper,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			AdminEntityOrderDetailTypePolicy adminEntityOrderDetailTypePolicy,
			PickupcodeSettingRedisService pickupcodeSettingRedisService,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			CompanySceneSmsSendPort companySceneSmsSendPort,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.adminEntityOrderDetailTypePolicy = adminEntityOrderDetailTypePolicy;
		this.pickupcodeSettingRedisService = pickupcodeSettingRedisService;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.companySceneSmsSendPort = companySceneSmsSendPort;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public boolean getOrderPickupCode(long companyId, long jwtUserId, String orderIdRaw) {
		String raw = orderIdRaw == null ? "" : orderIdRaw;
		String t = raw.trim();
		if (t.isEmpty()) {
			if (orderIdRaw == null || raw.isEmpty()) {
				throw new BadRequestException("订单号必填");
			}
			throw new BadRequestException("此订单不存在！");
		}
		if ("0".equals(t)) {
			throw new BadRequestException("订单号必填");
		}

		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("此订单不存在！");
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdNum)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new BadRequestException("此订单不存在！");
		}

		Long assocUserId = assoc.getUserId();
		long orderOwner = assocUserId == null ? 0L : assocUserId.longValue();
		if (orderOwner != jwtUserId) {
			throw new ForbiddenException("操作失败");
		}

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		if (!adminEntityOrderDetailTypePolicy.supportsNormalPipeline(effective)) {
			throw new ResourceException("无此类型订单！");
		}

		Map<String, Object> pickupSetting = pickupcodeSettingRedisService.handle(companyId, null);
		boolean pickupOn = toPickupStatus(pickupSetting.get("pickupcode_status"));
		if (assoc.getOrderClass() != null && "community".equalsIgnoreCase(assoc.getOrderClass().trim())) {
			pickupOn = false;
		}
		if (!pickupOn) {
			throw new ResourceException("商家未开启订单提货码");
		}

		Map<String, Object> bundle;
		try {
			bundle = adminNormalOrderDetailService.buildOrderBundle(companyId, t, false, "api");
		} catch (BadRequestException e) {
			if (e.getMessage() != null && e.getMessage().contains("的订单不存在")) {
				throw new ResourceException("订单号为" + t + "的订单不存在");
			}
			throw e;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfo = (Map<String, Object>) bundle.get("orderInfo");
		if (orderInfo == null || orderInfo.isEmpty()) {
			throw new ResourceException("订单号为" + t + "的订单不存在");
		}

		String orderStatus = orderInfo.get("order_status") == null ? "" : String.valueOf(orderInfo.get("order_status")).trim();
		String zitiStatus = orderInfo.get("ziti_status") == null ? "" : String.valueOf(orderInfo.get("ziti_status")).trim();
		if (!("PAYED".equals(orderStatus) && "PENDING".equals(zitiStatus))) {
			throw new ResourceException("订单不在待核销状态，无法获取提货码！");
		}

		String phone = orderInfo.get("mobile") == null ? "" : String.valueOf(orderInfo.get("mobile")).trim();
		if (phone.isEmpty()) {
			throw new ResourceException("未查询到提货人联系手机！");
		}

		String key = "admin-pickupcode:" + orderIdNum + "|" + phone;
		long ttl = companysRedisTemplate.getExpire(key, TimeUnit.SECONDS);
		if (ttl > 240L) {
			long wait = ttl - 240L;
			throw new ResourceException("发送过于频繁，请 " + wait + " 秒后再试");
		}

		int n = 100000 + ThreadLocalRandom.current().nextInt(900000);
		String vcode = String.valueOf(n);
		companysRedisTemplate.opsForValue().set(key, vcode, Duration.ofSeconds(1800));

		Map<String, String> vars = new LinkedHashMap<>();
		vars.put("订单号", t);
		vars.put("提货码", vcode);
		companySceneSmsSendPort.sendSceneTemplatedSms(companyId, phone, "order_pickup", vars);

		log.info("pickup sms sent companyId={} orderId={} mobileTail={}", companyId, orderIdNum, maskTail(phone));
		return true;
	}

	private static boolean toPickupStatus(Object raw) {
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		return "true".equalsIgnoreCase(String.valueOf(raw).trim());
	}

	private static String maskTail(String phone) {
		if (phone == null || phone.isEmpty()) {
			return "****";
		}
		String p = phone.trim();
		if (p.length() >= 4) {
			return "****" + p.substring(p.length() - 4);
		}
		return "****";
	}
}
