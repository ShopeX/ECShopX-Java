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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.CompanyRelLogistics;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.mapper.CompanyRelLogisticsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminOrderUpdateDeliveryService {

	private static final String KUAIDI_REDIS_PREFIX = "kuaidiTypeOpenConfig:";
	private static final Set<String> MERCHANT_SELF_STATUSES =
			Set.of("PACKAGED", "DELIVERING", "DONE", "CONFIRMING");

	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final CompanyRelLogisticsMapper companyRelLogisticsMapper;
	private final StringRedisTemplate stringRedisTemplate;
	private final AdminSelfDeliveryStaffFeeService adminSelfDeliveryStaffFeeService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	public AdminOrderUpdateDeliveryService(
			OrdersDeliveryMapper ordersDeliveryMapper,
			NormalOrdersMapper normalOrdersMapper,
			CompanyRelLogisticsMapper companyRelLogisticsMapper,
			StringRedisTemplate stringRedisTemplate,
			AdminSelfDeliveryStaffFeeService adminSelfDeliveryStaffFeeService,
			OrderProcessLogPublishPort orderProcessLogPublishPort) {
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.companyRelLogisticsMapper = companyRelLogisticsMapper;
		this.stringRedisTemplate = stringRedisTemplate;
		this.adminSelfDeliveryStaffFeeService = adminSelfDeliveryStaffFeeService;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateDelivery(
			long companyId, long operatorId, String ordersDeliveryId, Map<String, Object> params) {
		String pathId = trimNullToEmpty(ordersDeliveryId);
		OrdersDelivery deliveryBeforeUpdate =
				ordersDeliveryMapper.selectOne(
						new LambdaQueryWrapper<OrdersDelivery>()
								.eq(OrdersDelivery::getOrdersDeliveryId, pathId)
								.eq(OrdersDelivery::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (deliveryBeforeUpdate == null) {
			throw new ResourceException("发货单不存在");
		}

		long orderIdFromDelivery = deliveryBeforeUpdate.getOrderId();
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderIdFromDelivery)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("订单号为" + orderIdFromDelivery + "的订单不存在");
		}

		if ("merchant".equalsIgnoreCase(trimNullToEmpty(order.getReceiptType()))) {
			return updateMerchantBranch(
					companyId, operatorId, pathId, params, deliveryBeforeUpdate, order, orderIdFromDelivery);
		}
		return updateCourierBranch(companyId, operatorId, pathId, deliveryBeforeUpdate, params);
	}

	private Map<String, Object> updateMerchantBranch(
			long companyId,
			long operatorId,
			String pathOrdersDeliveryId,
			Map<String, Object> params,
			OrdersDelivery deliveryBeforeUpdate,
			NormalOrders order,
			long orderIdFromDelivery) {
		Object rawStatus = params.get("self_delivery_status");
		String status = rawStatus == null ? "" : String.valueOf(rawStatus).trim();
		if (!StringUtils.hasText(status)) {
			throw new BadRequestException("自配送状态必填");
		}
		if (!MERCHANT_SELF_STATUSES.contains(status)) {
			throw new BadRequestException("自配送状态无效");
		}

		String remarks;
		switch (status) {
			case "PACKAGED" -> remarks = "已打包";
			case "DELIVERING" -> remarks = "配送中";
			case "DONE" -> remarks = "商品已送达";
			case "CONFIRMING" -> remarks = "取消配送";
			default -> throw new BadRequestException("自配送状态无效");
		}

		boolean confirming = "CONFIRMING".equals(status);
		long resolvedId = resolveSelfDeliveryOperatorId(params);
		int orderFee = order.getSelfDeliveryFee() == null ? 0 : order.getSelfDeliveryFee();

		Integer feeToSet = null;
		Long opToSet = null;
		if (confirming) {
			opToSet = 0L;
			feeToSet = 0;
		}
		if (orderFee == 0 && resolvedId > 0L) {
			int feeFen = adminSelfDeliveryStaffFeeService.computeFeeFen(companyId, order, resolvedId);
			feeToSet = feeFen;
			opToSet = resolvedId;
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<NormalOrders> uw = new LambdaUpdateWrapper<>();
		uw.eq(NormalOrders::getCompanyId, companyId).eq(NormalOrders::getOrderId, order.getOrderId());
		uw.set(NormalOrders::getSelfDeliveryStatus, status);
		if (feeToSet != null) {
			uw.set(NormalOrders::getSelfDeliveryFee, feeToSet);
		}
		if (opToSet != null) {
			uw.set(NormalOrders::getSelfDeliveryOperatorId, opToSet);
		}
		if ("DONE".equals(status)) {
			long epoch = System.currentTimeMillis() / 1000L;
			uw.set(NormalOrders::getSelfDeliveryEndTime, Long.valueOf(epoch));
		}
		uw.set(NormalOrders::getUpdateTime, now);
		int n = normalOrdersMapper.update(null, uw);
		if (n != 1) {
			throw new ResourceException("未查询到更新数据");
		}

		LinkedHashMap<String, Object> logParams = new LinkedHashMap<>(params);
		logParams.put("orders_delivery_id", pathOrdersDeliveryId);
		logParams.put("company_id", companyId);
		logParams.put("operator_type", "admin");
		logParams.put("operator_id", operatorId);

		LinkedHashMap<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", deliveryBeforeUpdate.getOrderId());
		log.put("company_id", companyId);
		log.put("operator_type", "admin");
		log.put("operator_id", operatorId);
		log.put("is_show", Boolean.TRUE);
		log.put("remarks", remarks);
		log.put("detail", "订单号：" + orderIdFromDelivery + "，订单发货信息修改");
		log.put("params", logParams);
		Object dr = params.get("delivery_remark");
		log.put("delivery_remark", dr == null ? "" : String.valueOf(dr));
		log.put("pics", params.get("delivery_pics"));
		orderProcessLogPublishPort.publish(log);

		return toOrdersDeliveryDataMap(deliveryBeforeUpdate);
	}

	private Map<String, Object> updateCourierBranch(
			long companyId,
			long operatorId,
			String pathOrdersDeliveryId,
			OrdersDelivery deliveryBeforeUpdate,
			Map<String, Object> params) {
		Object dc = params.get("delivery_corp");
		Object code = params.get("delivery_code");
		String deliveryCorp = dc == null ? "" : String.valueOf(dc).trim();
		String deliveryCode = code == null ? "" : String.valueOf(code).trim();
		String corpName = resolveDeliveryCorpName(companyId, deliveryCorp);

		Long deliveryPk = deliveryBeforeUpdate.getOrdersDeliveryId();
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<OrdersDelivery> uw = new LambdaUpdateWrapper<>();
		uw.eq(OrdersDelivery::getOrdersDeliveryId, deliveryPk)
				.eq(OrdersDelivery::getCompanyId, companyId)
				.set(OrdersDelivery::getDeliveryCorp, deliveryCorp)
				.set(OrdersDelivery::getDeliveryCode, deliveryCode)
				.set(OrdersDelivery::getDeliveryCorpName, corpName)
				.set(OrdersDelivery::getUpdated, now);
		int rows = ordersDeliveryMapper.update(null, uw);
		if (rows != 1) {
			throw new ResourceException("未查询到更新数据");
		}

		OrdersDelivery refreshed =
				ordersDeliveryMapper.selectOne(
						new LambdaQueryWrapper<OrdersDelivery>()
								.eq(OrdersDelivery::getOrdersDeliveryId, deliveryPk)
								.eq(OrdersDelivery::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (refreshed == null) {
			throw new ResourceException("未查询到更新数据");
		}

		LinkedHashMap<String, Object> logParams = new LinkedHashMap<>(params);
		logParams.put("orders_delivery_id", pathOrdersDeliveryId);
		logParams.put("company_id", companyId);
		logParams.put("operator_type", "admin");
		logParams.put("operator_id", operatorId);

		LinkedHashMap<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", refreshed.getOrderId());
		log.put("company_id", companyId);
		log.put("operator_type", "admin");
		log.put("operator_id", operatorId);
		log.put("remarks", "订单发货");
		log.put("detail", "订单号：" + refreshed.getOrderId() + "，订单发货信息修改");
		log.put("params", logParams);
		orderProcessLogPublishPort.publish(log);

		return toOrdersDeliveryDataMap(refreshed);
	}

	private long resolveSelfDeliveryOperatorId(Map<String, Object> params) {
		Object raw = params.get("self_delivery_operator_id");
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof CharSequence cs) {
			String t = cs.toString().trim();
			if (!StringUtils.hasText(t)) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("配送员标识无效");
			}
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		throw new BadRequestException("配送员标识无效");
	}

	private String resolveDeliveryCorpName(long companyId, String deliveryCorp) {
		String openType = readKuaidiOpenType(companyId);
		LambdaQueryWrapper<CompanyRelLogistics> qw = new LambdaQueryWrapper<>();
		qw.eq(CompanyRelLogistics::getCompanyId, (int) companyId);
		qw.eq(CompanyRelLogistics::getSupplierId, 0L);
		if ("kuaidi100".equalsIgnoreCase(openType) && StringUtils.hasText(deliveryCorp)) {
			qw.apply("LOWER(kuaidi_code) = LOWER({0})", deliveryCorp.trim());
		} else {
			qw.eq(CompanyRelLogistics::getCorpCode, deliveryCorp);
		}
		qw.last("LIMIT 1");
		CompanyRelLogistics row = companyRelLogisticsMapper.selectOne(qw);
		if (row != null && StringUtils.hasText(row.getCorpName())) {
			return row.getCorpName().trim();
		}
		return "其他";
	}

	private String readKuaidiOpenType(long companyId) {
		String key = KUAIDI_REDIS_PREFIX + sha1Hex(String.valueOf(companyId));
		try {
			String v = stringRedisTemplate.opsForValue().get(key);
			return StringUtils.hasText(v) ? v.trim() : "";
		} catch (DataAccessException e) {
			return "";
		}
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String trimNullToEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	private static Map<String, Object> toOrdersDeliveryDataMap(OrdersDelivery d) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("orders_delivery_id", d.getOrdersDeliveryId());
		m.put("company_id", d.getCompanyId());
		m.put("supplier_id", d.getSupplierId());
		m.put("order_id", d.getOrderId());
		m.put("user_id", d.getUserId());
		m.put("delivery_corp", d.getDeliveryCorp());
		m.put("delivery_corp_name", d.getDeliveryCorpName());
		m.put("delivery_code", d.getDeliveryCode());
		m.put("delivery_time", d.getDeliveryTime());
		m.put("delivery_corp_source", d.getDeliveryCorpSource());
		m.put("receiver_mobile", d.getReceiverMobile());
		m.put("package_type", d.getPackageType());
		m.put("created", d.getCreated());
		m.put("updated", d.getUpdated());
		m.put("self_delivery_operator_id", d.getSelfDeliveryOperatorId());
		m.put("delivery_remark", d.getDeliveryRemark());
		m.put("delivery_pics", d.getDeliveryPics());
		return m;
	}
}
