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

import cn.shopex.ecshopx.common.port.order.OrderExportEmployeePurchaseInfoLookupPort;
import cn.shopex.ecshopx.companys.service.setting.TradeCancelSettingRedisService;
import cn.shopex.ecshopx.espier.domain.Subdistrict;
import cn.shopex.ecshopx.espier.mapper.SubdistrictMapper;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelZiti;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.domain.OrdersDiagnosis;
import cn.shopex.ecshopx.orders.domain.OrdersPrescription;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelZitiMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDiagnosisMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersPrescriptionMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Enriches each admin normal-order list row with the same structural fields as the detail assembler path
 * (items, dada, ziti, cancel, delivery split, {@code app_info}, subdistrict labels, prescription/diagnosis, etc.).
 */
@Component
public class AdminNormalOrderListRowDecorator {

	private static final String NO_APPLY_CANCEL = "NO_APPLY_CANCEL";

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final TradeMapper tradeMapper;
	private final CancelOrdersMapper cancelOrdersMapper;
	private final NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	private final NormalOrdersRelZitiMapper normalOrdersRelZitiMapper;
	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier;
	private final SubdistrictMapper subdistrictMapper;
	private final OrdersPrescriptionMapper ordersPrescriptionMapper;
	private final OrdersDiagnosisMapper ordersDiagnosisMapper;
	private final ObjectMapper objectMapper;
	private final ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort;
	private final TradeCancelSettingRedisService tradeCancelSettingRedisService;

	public AdminNormalOrderListRowDecorator(
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			TradeMapper tradeMapper,
			CancelOrdersMapper cancelOrdersMapper,
			NormalOrdersRelDadaMapper normalOrdersRelDadaMapper,
			NormalOrdersRelZitiMapper normalOrdersRelZitiMapper,
			OrdersDeliveryMapper ordersDeliveryMapper,
			AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier,
			SubdistrictMapper subdistrictMapper,
			OrdersPrescriptionMapper ordersPrescriptionMapper,
			OrdersDiagnosisMapper ordersDiagnosisMapper,
			ObjectMapper objectMapper,
			ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort,
			TradeCancelSettingRedisService tradeCancelSettingRedisService) {
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.tradeMapper = tradeMapper;
		this.cancelOrdersMapper = cancelOrdersMapper;
		this.normalOrdersRelDadaMapper = normalOrdersRelDadaMapper;
		this.normalOrdersRelZitiMapper = normalOrdersRelZitiMapper;
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.adminOrderDetailStatusAppApplier = adminOrderDetailStatusAppApplier;
		this.subdistrictMapper = subdistrictMapper;
		this.ordersPrescriptionMapper = ordersPrescriptionMapper;
		this.ordersDiagnosisMapper = ordersDiagnosisMapper;
		this.objectMapper = objectMapper;
		this.employeePurchaseInfoLookupPort = employeePurchaseInfoLookupPort;
		this.tradeCancelSettingRedisService = tradeCancelSettingRedisService;
	}

	public void decorateBatch(long companyId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> orderIds =
				rows.stream().map(r -> parseOrderIdLong(r.get("order_id"))).filter(id -> id > 0).distinct().toList();
		if (orderIds.isEmpty()) {
			return;
		}

		Map<Long, List<NormalOrdersItems>> itemsByOrder = loadItemsGrouped(companyId, orderIds);
		Map<Long, List<Trade>> tradesByOrder = loadTradesGrouped(companyId, orderIds);
		Map<Long, CancelOrders> cancelByOrder = loadCancelByOrder(companyId, orderIds);
		boolean repeatCancel =
				Boolean.TRUE.equals(tradeCancelSettingRedisService.getCancelSetting(companyId).get("repeat_cancel"));
		Map<Long, NormalOrdersRelDada> dadaByOrder = loadDadaByOrder(companyId, orderIds);
		Map<Long, NormalOrdersRelZiti> zitiByOrder = loadZitiByOrder(companyId, orderIds);
		Map<Long, OrdersDelivery> deliveryByOrder = loadDeliveryByOrder(orderIds);
		Map<Long, String> subdistrictLabelById = loadSubdistrictLabels(rows);
		PrescriptionDiagnosisBatch pdBatch = loadPrescriptionAndDiagnosis(companyId, rows);
		Map<Long, OrderExportEmployeePurchaseInfoLookupPort.Info> purchaseByOrder =
				loadEmployeePurchaseInfo(companyId, orderIds);

		for (Map<String, Object> orderInfo : rows) {
			long orderIdNum = parseOrderIdLong(orderInfo.get("order_id"));
			applySyntheticNotpayCancel(orderInfo);
			applyEmployeePurchaseFields(orderInfo, purchaseByOrder.get(orderIdNum));

			applySubdistrictLabels(orderInfo, subdistrictLabelById);
			String oidStr = String.valueOf(orderIdNum);
			Map<String, Object> prescRow = pdBatch.prescriptionByOrderId().get(oidStr);
			orderInfo.put("prescription_data", prescRow != null ? prescRow : new ArrayList<>());
			Map<String, Object> diagRow = pdBatch.diagnosisByOrderId().get(oidStr);
			orderInfo.put("diagnosis_data", diagRow != null ? diagRow : new ArrayList<>());

			List<Map<String, Object>> itemMaps = buildItemMaps(itemsByOrder.get(orderIdNum));
			orderInfo.put("items", itemMaps);

			CancelOrders cancelRow = cancelByOrder.get(orderIdNum);
			AdminOrderCanApplyCancelResolver.apply(orderInfo, cancelRow, repeatCancel);

			Map<String, Object> cancelData = new LinkedHashMap<>();
			String cancelStatus = str(orderInfo.get("cancel_status"));
			if (!NO_APPLY_CANCEL.equals(cancelStatus) && cancelRow != null) {
				cancelData = cancelToMap(cancelRow);
			}

			orderInfo.put("can_apply_aftersales", 0);
			long now = System.currentTimeMillis() / 1000L;
			int leftAftersalesNum = intVal(orderInfo.get("left_aftersales_num"));
			int autoCloseAftersalesTime = intVal(orderInfo.get("order_auto_close_aftersales_time"));
			if ((autoCloseAftersalesTime <= 0 || autoCloseAftersalesTime > now) && leftAftersalesNum > 0) {
				orderInfo.put("can_apply_aftersales", 1);
			}
			String oStatus = str(orderInfo.get("order_status"));
			if ("CANCEL".equals(oStatus)) {
				orderInfo.put("can_apply_aftersales", 0);
			}
			orderInfo.put("refund_freight", 0);
			int freight = intVal(orderInfo.get("freight_fee"));
			orderInfo.put("refund_freight_amount", freight);
			orderInfo.put("cancelData", cancelData);

			List<Trade> trades = tradesByOrder.getOrDefault(orderIdNum, List.of());
			List<Map<String, Object>> tradeListMaps = new ArrayList<>();
			for (Trade t : trades) {
				tradeListMaps.add(AdminOrderDetailPayloadMaps.tradeToMap(t));
			}
			String cancelFrom = cancelData.isEmpty() ? "" : str(cancelData.get("cancel_from"));

			Map<String, Object> dadaMap = new LinkedHashMap<>();
			if ("dada".equals(str(orderInfo.get("receipt_type")))) {
				NormalOrdersRelDada dadaRow = dadaByOrder.get(orderIdNum);
				dadaMap = AdminOrderDetailPayloadMaps.dadaToMap(dadaRow);
				orderInfo.put("dada", dadaMap);
			} else {
				orderInfo.put("dada", dadaMap);
			}

			adminOrderDetailStatusAppApplier.apply(orderInfo, dadaMap, cancelFrom);

			if (StringUtils.hasText(str(orderInfo.get("delivery_corp")))) {
				String corp = str(orderInfo.get("delivery_corp"));
				orderInfo.put("delivery_corp_name", corp);
			}

			int getPts = intVal(orderInfo.get("get_points"));
			int bonus = intVal(orderInfo.get("bonus_points"));
			orderInfo.put("estimate_get_points", getPts + bonus);

			orderInfo.put("delivery_type", "new");
			if (StringUtils.hasText(str(orderInfo.get("delivery_code")))) {
				orderInfo.put("delivery_type", "old");
			} else {
				for (Map<String, Object> im : itemMaps) {
					if (StringUtils.hasText(str(im.get("delivery_code")))) {
						orderInfo.put("delivery_type", "old");
						break;
					}
				}
			}

			if ("new".equals(str(orderInfo.get("delivery_type")))) {
				OrdersDelivery od = deliveryByOrder.get(orderIdNum);
				if (od != null) {
					orderInfo.put("orders_delivery_id", od.getOrdersDeliveryId() != null ? od.getOrdersDeliveryId() : "");
					putStr(orderInfo, "delivery_corp", od.getDeliveryCorp());
					putStr(orderInfo, "delivery_corp_name", od.getDeliveryCorpName());
					putStr(orderInfo, "delivery_code", od.getDeliveryCode());
					orderInfo.put(
							"is_all_delivery",
							od.getPackageType() != null && "batch".equals(od.getPackageType()));
				} else {
					orderInfo.put("orders_delivery_id", "");
					orderInfo.put("is_all_delivery", "");
					putStr(orderInfo, "delivery_corp", "");
					putStr(orderInfo, "delivery_corp_name", "");
					putStr(orderInfo, "delivery_code", "");
				}
			}

			if ("ziti".equals(str(orderInfo.get("receipt_type")))) {
				NormalOrdersRelZiti ziti = zitiByOrder.get(orderIdNum);
				if (ziti != null) {
					orderInfo.put("ziti_info", zitiToMap(ziti));
				}
			}

			orderInfo.putIfAbsent("self_delivery_operator_mobile", "");
			orderInfo.putIfAbsent("self_delivery_operator_name", "");

			int offlinePs = intVal(orderInfo.get("offline_payment_status"));
			orderInfo.put("offline_pay_check_status", offlinePs == -1 ? null : offlinePs);

			orderInfo.put("latest_aftersale_time", 0);
			if (!tradeListMaps.isEmpty()) {
				orderInfo.put("trade", new LinkedHashMap<>(tradeListMaps.get(0)));
			} else {
				orderInfo.put("trade", new LinkedHashMap<String, Object>());
			}
		}
	}

	private Map<Long, OrderExportEmployeePurchaseInfoLookupPort.Info> loadEmployeePurchaseInfo(
			long companyId, List<Long> orderIds) {
		OrderExportEmployeePurchaseInfoLookupPort port = employeePurchaseInfoLookupPort.getIfAvailable();
		if (port == null) {
			return Map.of();
		}
		return port.lookupByOrderIds(companyId, orderIds);
	}

	private static void applyEmployeePurchaseFields(
			Map<String, Object> orderInfo, OrderExportEmployeePurchaseInfoLookupPort.Info info) {
		if (info == null) {
			return;
		}
		String mode = info.purchaseMode() == null ? "" : info.purchaseMode();
		orderInfo.put("purchase_mode", mode.isEmpty() ? null : mode);
		orderInfo.put(
				"purchase_mode_desc",
				"prepaid_point".equals(mode) ? "预充点数" : ("cash".equals(mode) ? "现金" : ""));
		orderInfo.put(
				"employee_purchase_activity_id",
				info.activityId() == null ? 0L : info.activityId());
		orderInfo.put(
				"employee_purchase_activity_name",
				info.activityName() == null ? "" : info.activityName());
	}

	private Map<Long, List<NormalOrdersItems>> loadItemsGrouped(long companyId, List<Long> orderIds) {
		List<NormalOrdersItems> all =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.in(NormalOrdersItems::getOrderId, orderIds)
								.orderByAsc(NormalOrdersItems::getId));
		return all.stream().collect(Collectors.groupingBy(NormalOrdersItems::getOrderId));
	}

	private Map<Long, List<Trade>> loadTradesGrouped(long companyId, List<Long> orderIds) {
		List<String> orderIdStrs = orderIds.stream().map(String::valueOf).toList();
		List<Trade> all =
				tradeMapper.selectList(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.in(Trade::getOrderId, orderIdStrs)
								.orderByAsc(Trade::getTimeStart));
		return all.stream()
				.collect(
						Collectors.groupingBy(
								t -> {
									try {
										return Long.parseLong(String.valueOf(t.getOrderId()).trim());
									} catch (NumberFormatException e) {
										return 0L;
									}
								}));
	}

	private Map<Long, CancelOrders> loadCancelByOrder(long companyId, List<Long> orderIds) {
		List<CancelOrders> all =
				cancelOrdersMapper.selectList(
						new LambdaQueryWrapper<CancelOrders>()
								.eq(CancelOrders::getCompanyId, companyId)
								.in(CancelOrders::getOrderId, orderIds));
		Map<Long, CancelOrders> m = new LinkedHashMap<>();
		for (CancelOrders c : all) {
			m.putIfAbsent(c.getOrderId(), c);
		}
		return m;
	}

	private Map<Long, NormalOrdersRelDada> loadDadaByOrder(long companyId, List<Long> orderIds) {
		List<NormalOrdersRelDada> all =
				normalOrdersRelDadaMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersRelDada>()
								.eq(NormalOrdersRelDada::getCompanyId, companyId)
								.in(NormalOrdersRelDada::getOrderId, orderIds));
		Map<Long, NormalOrdersRelDada> m = new LinkedHashMap<>();
		for (NormalOrdersRelDada d : all) {
			m.putIfAbsent(d.getOrderId(), d);
		}
		return m;
	}

	private Map<Long, NormalOrdersRelZiti> loadZitiByOrder(long companyId, List<Long> orderIds) {
		List<NormalOrdersRelZiti> all =
				normalOrdersRelZitiMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersRelZiti>()
								.eq(NormalOrdersRelZiti::getCompanyId, companyId)
								.in(NormalOrdersRelZiti::getOrderId, orderIds));
		Map<Long, NormalOrdersRelZiti> m = new LinkedHashMap<>();
		for (NormalOrdersRelZiti z : all) {
			m.putIfAbsent(z.getOrderId(), z);
		}
		return m;
	}

	private Map<Long, OrdersDelivery> loadDeliveryByOrder(List<Long> orderIds) {
		List<OrdersDelivery> all =
				ordersDeliveryMapper.selectList(
						new LambdaQueryWrapper<OrdersDelivery>().in(OrdersDelivery::getOrderId, orderIds));
		Map<Long, OrdersDelivery> m = new LinkedHashMap<>();
		for (OrdersDelivery od : all) {
			m.putIfAbsent(od.getOrderId(), od);
		}
		return m;
	}

	private static List<Map<String, Object>> buildItemMaps(List<NormalOrdersItems> rows) {
		List<Map<String, Object>> itemMaps = new ArrayList<>();
		if (rows == null) {
			return itemMaps;
		}
		for (NormalOrdersItems it : rows) {
			Map<String, Object> im = AdminOrderDetailPayloadMaps.itemToMap(it);
			im.put("item_holder", "self");
			im.put("supplier_name", "");
			im.put("sale_price", im.get("price"));
			int totalFee = intVal(im.get("total_fee"));
			int pointFee = intVal(im.get("point_fee"));
			int refunded = intVal(im.get("refunded_fee"));
			im.put("after_sales_fee", 0);
			im.put("remain_fee", totalFee);
			im.put("remain_point", pointFee);
			im.put("refundable_amount", totalFee - refunded);
			String ds = str(im.get("delivery_status"));
			int num = intVal(im.get("num"));
			Integer din = it.getDeliveryItemNum();
			if ("DONE".equals(ds) && (din == null || din == 0)) {
				im.put("delivery_item_num", num);
			}
			int effectiveDin = intVal(im.get("delivery_item_num"));
			im.put("delivery_item_num", Math.min(effectiveDin > 0 ? effectiveDin : 0, num));
			if (StringUtils.hasText(str(im.get("delivery_corp")))) {
				String corp = str(im.get("delivery_corp"));
				im.put("delivery_corp_name", corp);
			}
			itemMaps.add(im);
		}
		return itemMaps;
	}

	private static void applySyntheticNotpayCancel(Map<String, Object> orderInfo) {
		if (!"NOTPAY".equals(str(orderInfo.get("order_status")))) {
			return;
		}
		if ("drug".equals(str(orderInfo.get("order_class")))) {
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		int autoCancel = intVal(orderInfo.get("auto_cancel_time"));
		if (autoCancel <= 0 || autoCancel - now > 0) {
			return;
		}
		String payType = str(orderInfo.get("pay_type"));
		String offlineSt = str(orderInfo.get("offline_payment_status"));
		boolean branch1 = !"offline_pay".equals(payType) || "-1".equals(offlineSt);
		if (branch1) {
			orderInfo.put("order_status", "CANCEL");
			return;
		}
		if ("offline_pay".equals(payType) && !"0".equals(offlineSt)) {
			orderInfo.put("order_status", "CANCEL");
		}
	}

	private static Map<String, Object> cancelToMap(CancelOrders c) {
		Map<String, Object> m = new LinkedHashMap<>();
		putStr(m, "cancel_id", c.getCancelId());
		putStr(m, "order_id", c.getOrderId());
		putStr(m, "company_id", c.getCompanyId());
		putStr(m, "cancel_from", c.getCancelFrom());
		m.put("cancel_reason", c.getCancelReason());
		m.put("refund_status", c.getRefundStatus());
		m.put("progress", c.getProgress());
		m.put("create_time", c.getCreateTime());
		m.put("update_time", c.getUpdateTime());
		return m;
	}

	private static Map<String, Object> zitiToMap(NormalOrdersRelZiti z) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("name", z.getName());
		m.put("lng", z.getLng());
		m.put("lat", z.getLat());
		m.put("province", z.getProvince());
		m.put("city", z.getCity());
		m.put("area", z.getArea());
		m.put("address", z.getAddress());
		m.put("contract_phone", z.getContractPhone());
		m.put("pickup_date", z.getPickupDate());
		m.put("pickup_time", decodeZitiPickupTime(z.getPickupTime()));
		m.put("create_time", z.getCreateTime());
		m.put("update_time", z.getUpdateTime());
		return m;
	}

	private static Object decodeZitiPickupTime(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return new ObjectMapper().readValue(raw, new TypeReference<List<Object>>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private static void putStr(Map<String, Object> m, String k, Object v) {
		m.put(k, v == null ? "" : String.valueOf(v));
	}

	private static long parseOrderIdLong(Object raw) {
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

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private Map<Long, String> loadSubdistrictLabels(List<Map<String, Object>> rows) {
		Set<Long> ids = new LinkedHashSet<>();
		for (Map<String, Object> r : rows) {
			Long a = longOrNull(r.get("subdistrict_parent_id"));
			Long b = longOrNull(r.get("subdistrict_id"));
			if (a != null) {
				ids.add(a);
			}
			if (b != null) {
				ids.add(b);
			}
		}
		if (ids.isEmpty()) {
			return Map.of();
		}
		List<Subdistrict> list =
				subdistrictMapper.selectList(new LambdaQueryWrapper<Subdistrict>().in(Subdistrict::getId, ids));
		Map<Long, String> m = new LinkedHashMap<>();
		for (Subdistrict s : list) {
			m.put(s.getId(), s.getLabel() != null ? s.getLabel() : "");
		}
		return m;
	}

	private static void applySubdistrictLabels(Map<String, Object> orderInfo, Map<Long, String> labelById) {
		Long parentId = longOrNull(orderInfo.get("subdistrict_parent_id"));
		orderInfo.put("subdistrict_parent", parentId != null ? labelById.getOrDefault(parentId, "") : "");
		Long sid = longOrNull(orderInfo.get("subdistrict_id"));
		orderInfo.put("subdistrict", sid != null ? labelById.getOrDefault(sid, "") : "");
	}

	private PrescriptionDiagnosisBatch loadPrescriptionAndDiagnosis(long companyId, List<Map<String, Object>> rows) {
		List<String> orderIdStrs = new ArrayList<>();
		for (Map<String, Object> r : rows) {
			if (intVal(r.get("prescription_status")) > 0) {
				long oid = parseOrderIdLong(r.get("order_id"));
				if (oid > 0) {
					orderIdStrs.add(String.valueOf(oid));
				}
			}
		}
		if (orderIdStrs.isEmpty()) {
			return new PrescriptionDiagnosisBatch(Map.of(), Map.of());
		}
		List<OrdersPrescription> prescList =
				ordersPrescriptionMapper.selectList(
						new LambdaQueryWrapper<OrdersPrescription>()
								.eq(OrdersPrescription::getCompanyId, companyId)
								.in(OrdersPrescription::getOrderId, orderIdStrs)
								.eq(OrdersPrescription::getIsDeleted, 0));
		Map<String, Map<String, Object>> byP = new LinkedHashMap<>();
		for (OrdersPrescription p : prescList) {
			if (p.getOrderId() != null) {
				byP.putIfAbsent(p.getOrderId(), prescriptionToMap(p));
			}
		}
		List<OrdersDiagnosis> diagList =
				ordersDiagnosisMapper.selectList(
						new LambdaQueryWrapper<OrdersDiagnosis>()
								.eq(OrdersDiagnosis::getCompanyId, companyId)
								.in(OrdersDiagnosis::getOrderId, orderIdStrs));
		Map<String, Map<String, Object>> byD = new LinkedHashMap<>();
		for (OrdersDiagnosis d : diagList) {
			if (d.getOrderId() != null) {
				byD.putIfAbsent(d.getOrderId(), diagnosisToMap(d));
			}
		}
		return new PrescriptionDiagnosisBatch(byP, byD);
	}

	private Map<String, Object> prescriptionToMap(OrdersPrescription p) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", p.getId());
		m.put("order_id", p.getOrderId());
		m.put("diagnosis_id", p.getDiagnosisId());
		m.put("user_id", p.getUserId());
		m.put("company_id", p.getCompanyId());
		m.put("distributor_id", p.getDistributorId());
		m.put("prescription_id", p.getPrescriptionId());
		m.put("hospital_name", p.getHospitalName());
		m.put("kuaizhen_store_id", p.getKuaizhenStoreId());
		m.put("kuaizhen_store_name", p.getKuaizhenStoreName());
		m.put("kuaizhen_diagnosis_id", p.getKuaizhenDiagnosisId());
		m.put("doctor_sign_time", p.getDoctorSignTime());
		m.put("doctor_office", p.getDoctorOffice());
		m.put("doctor_id", p.getDoctorId());
		m.put("doctor_name", p.getDoctorName());
		m.put("user_family_name", p.getUserFamilyName());
		m.put("user_family_phone", p.getUserFamilyPhone());
		m.put("user_family_age", p.getUserFamilyAge());
		m.put("user_family_gender", p.getUserFamilyGender());
		m.put("user_family_id_card", p.getUserFamilyIdCard());
		m.put("tags", p.getTags());
		m.put("status", p.getStatus());
		m.put("memo", p.getMemo());
		m.put("remarks", p.getRemarks());
		m.put("reason", p.getReason());
		m.put("dst_file_path", p.getDstFilePath());
		m.put("serial_no", p.getSerialNo());
		putDrugRspList(m, p.getDrugRspList());
		m.put("audit_status", p.getAuditStatus());
		m.put("audit_time", p.getAuditTime());
		m.put("audit_reason", p.getAuditReason());
		m.put("audit_apothecary_name", p.getAuditApothecaryName());
		m.put("is_deleted", p.getIsDeleted());
		m.put("delete_time", p.getDeleteTime());
		m.put("created", p.getCreated());
		m.put("updated", p.getUpdated());
		return m;
	}

	private void putDrugRspList(Map<String, Object> m, String raw) {
		if (!StringUtils.hasText(raw)) {
			m.put("drug_rsp_list", raw);
			return;
		}
		try {
			m.put("drug_rsp_list", objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {}));
		} catch (Exception e) {
			m.put("drug_rsp_list", raw);
		}
	}

	private static Map<String, Object> diagnosisToMap(OrdersDiagnosis d) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", d.getId());
		m.put("order_id", d.getOrderId());
		m.put("user_id", d.getUserId());
		m.put("company_id", d.getCompanyId());
		m.put("kuaizhen_store_id", d.getKuaizhenStoreId());
		m.put("distributor_id", d.getDistributorId());
		m.put("service_type", d.getServiceType());
		m.put("is_examine", d.getIsExamine());
		m.put("is_pregnant_woman", d.getIsPregnantWoman());
		m.put("is_lactation", d.getIsLactation());
		m.put("souce_from", d.getSouceFrom());
		m.put("user_family_name", d.getUserFamilyName());
		m.put("user_family_id_card", d.getUserFamilyIdCard());
		m.put("user_family_age", d.getUserFamilyAge());
		m.put("user_family_gender", d.getUserFamilyGender());
		m.put("user_family_phone", d.getUserFamilyPhone());
		m.put("relationship", d.getRelationship());
		m.put("before_ai_data_list", d.getBeforeAiDataList());
		m.put("prescription_status", d.getPrescriptionStatus());
		m.put("prescription_refuse_reason", d.getPrescriptionRefuseReason());
		m.put("location_url", d.getLocationUrl());
		m.put("status", d.getStatus());
		m.put("end_time", d.getEndTime());
		m.put("cancel_time", d.getCancelTime());
		m.put("doctor_office", d.getDoctorOffice());
		m.put("doctor_name", d.getDoctorName());
		m.put("hospital_name", d.getHospitalName());
		m.put("first_visit_list", d.getFirstVisitList());
		m.put("created", d.getCreated());
		m.put("updated", d.getUpdated());
		return m;
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v == 0L ? null : v;
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private record PrescriptionDiagnosisBatch(
			Map<String, Map<String, Object>> prescriptionByOrderId,
			Map<String, Map<String, Object>> diagnosisByOrderId) {}
}
