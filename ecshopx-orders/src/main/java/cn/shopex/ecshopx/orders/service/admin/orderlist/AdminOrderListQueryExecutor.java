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

package cn.shopex.ecshopx.orders.service.admin.orderlist;

import cn.shopex.ecshopx.members.domain.MembersDeleteRecord;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderListRowDecorator;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import cn.shopex.ecshopx.orders.service.supplier.SupplierOrderListRowMaps;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminOrderListQueryExecutor {

	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final ServiceOrdersMapper serviceOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;
	private final AdminNormalOrderListRowDecorator adminNormalOrderListRowDecorator;
	private final MembersDeleteRecordMapper membersDeleteRecordMapper;

	@Value("${common.oem-shuyun:false}")
	private boolean oemShuyun;

	public AdminOrderListQueryExecutor(
			NormalOrdersMapper normalOrdersMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			ServiceOrdersMapper serviceOrdersMapper,
			SupplierOrderMapper supplierOrderMapper,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler,
			AdminNormalOrderListRowDecorator adminNormalOrderListRowDecorator,
			MembersDeleteRecordMapper membersDeleteRecordMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.serviceOrdersMapper = serviceOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
		this.adminNormalOrderListRowDecorator = adminNormalOrderListRowDecorator;
		this.membersDeleteRecordMapper = membersDeleteRecordMapper;
	}

	public record PageResult(long total, List<Map<String, Object>> list) {}

	public PageResult queryPage(
			AdminOrderListExecutorKind kind,
			Map<String, Object> filter,
			int pageNo,
			int pageSize,
			boolean createTimeAsc) {
		if (kind == AdminOrderListExecutorKind.ASSOCIATION) {
			return queryAssociationsPage(filter, pageNo, pageSize, createTimeAsc);
		}
		if (kind == AdminOrderListExecutorKind.SUPPLIER_ORDER) {
			return querySupplierOrdersPage(filter, pageNo, pageSize, createTimeAsc);
		}
		if (kind == AdminOrderListExecutorKind.SERVICE
				|| kind == AdminOrderListExecutorKind.GROUPS_SERVICE
				|| kind == AdminOrderListExecutorKind.SECKILL_SERVICE) {
			return queryServiceOrdersPage(kind, filter, pageNo, pageSize, createTimeAsc);
		}
		return queryNormalOrdersPage(kind, filter, pageNo, pageSize, createTimeAsc);
	}

	public long countNormalOrders(AdminOrderListExecutorKind kind, Map<String, Object> filter) {
		long companyId = longVal(filter.get("company_id"));
		LambdaQueryWrapper<NormalOrders> cw = buildNormalWrapper(companyId, kind, filter);
		if (oemShuyun) {
			applyOemShuyunPromoterConstraint(cw, companyId);
		}
		Long t = normalOrdersMapper.selectCount(cw);
		return t == null ? 0L : t.longValue();
	}

	private PageResult queryNormalOrdersPage(
			AdminOrderListExecutorKind kind,
			Map<String, Object> filter,
			int pageNo,
			int pageSize,
			boolean createTimeAsc) {
		long companyId = longVal(filter.get("company_id"));
		LambdaQueryWrapper<NormalOrders> wrapper = buildNormalWrapper(companyId, kind, filter);
		LambdaQueryWrapper<NormalOrders> countWrapper = buildNormalWrapper(companyId, kind, filter);
		if (oemShuyun) {
			applyOemShuyunPromoterConstraint(wrapper, companyId);
			applyOemShuyunPromoterConstraint(countWrapper, companyId);
		}
		applyOrdering(wrapper, createTimeAsc);
		Page<NormalOrders> page = new Page<>(pageNo, pageSize, false);
		Page<NormalOrders> result = normalOrdersMapper.selectPage(page, wrapper);
		Long totalCt = normalOrdersMapper.selectCount(countWrapper);
		long total = totalCt == null ? 0L : totalCt;
		List<Map<String, Object>> rows = new ArrayList<>();
		for (NormalOrders order : result.getRecords()) {
			Map<String, Object> row =
					new LinkedHashMap<>(normalOrdersServiceOrderDataAssembler.toServiceOrderData(order));
			row.put("order_id", String.valueOf(order.getOrderId()));
			row.put("company_id", companyId);
			rows.add(row);
		}
		adminNormalOrderListRowDecorator.decorateBatch(companyId, rows);
		applyUserDeleteFlags(companyId, rows);
		return new PageResult(total, rows);
	}

	private void applyOemShuyunPromoterConstraint(LambdaQueryWrapper<NormalOrders> w, long companyId) {
		if (!oemShuyun) {
			return;
		}
		w.apply(
				"EXISTS (SELECT 1 FROM popularize_promoter pp WHERE pp.company_id = {0} AND pp.user_id = orders_normal_orders.user_id AND pp.is_promoter = 1)",
				companyId);
	}

	private PageResult queryServiceOrdersPage(
			AdminOrderListExecutorKind kind,
			Map<String, Object> filter,
			int pageNo,
			int pageSize,
			boolean createTimeAsc) {
		long companyId = longVal(filter.get("company_id"));
		LambdaQueryWrapper<ServiceOrders> w = buildServiceWrapper(companyId, kind, filter);
		LambdaQueryWrapper<ServiceOrders> cw = buildServiceWrapper(companyId, kind, filter);
		if (createTimeAsc) {
			w.orderByAsc(ServiceOrders::getCreateTime);
		} else {
			w.orderByDesc(ServiceOrders::getCreateTime);
		}
		Page<ServiceOrders> page = new Page<>(pageNo, pageSize, false);
		Page<ServiceOrders> result = serviceOrdersMapper.selectPage(page, w);
		Long totalCt = serviceOrdersMapper.selectCount(cw);
		long total = totalCt == null ? 0L : totalCt;
		List<Map<String, Object>> rows = new ArrayList<>();
		for (ServiceOrders o : result.getRecords()) {
			rows.add(serviceOrderToRow(o, companyId));
		}
		return new PageResult(total, rows);
	}

	private PageResult querySupplierOrdersPage(
			Map<String, Object> filter, int pageNo, int pageSize, boolean createTimeAsc) {
		long companyId = longVal(filter.get("company_id"));
		LambdaQueryWrapper<SupplierOrder> w = buildSupplierOrderWrapper(companyId, filter);
		LambdaQueryWrapper<SupplierOrder> cw = buildSupplierOrderWrapper(companyId, filter);
		if (createTimeAsc) {
			w.orderByAsc(SupplierOrder::getCreateTime);
		} else {
			w.orderByDesc(SupplierOrder::getCreateTime);
		}
		Page<SupplierOrder> page = new Page<>(pageNo, pageSize, false);
		Page<SupplierOrder> result = supplierOrderMapper.selectPage(page, w);
		Long totalCt = supplierOrderMapper.selectCount(cw);
		long total = totalCt == null ? 0L : totalCt;
		List<Map<String, Object>> rows = new ArrayList<>();
		for (SupplierOrder o : result.getRecords()) {
			Map<String, Object> row = new LinkedHashMap<>(SupplierOrderListRowMaps.toRow(o));
			row.put("order_id", String.valueOf(o.getOrderId()));
			rows.add(row);
		}
		return new PageResult(total, rows);
	}

	private PageResult queryAssociationsPage(
			Map<String, Object> filter, int pageNo, int pageSize, boolean createTimeAsc) {
		long companyId = longVal(filter.get("company_id"));
		LambdaQueryWrapper<OrderAssociations> w = buildAssociationWrapper(companyId, filter);
		LambdaQueryWrapper<OrderAssociations> cw = buildAssociationWrapper(companyId, filter);
		if (createTimeAsc) {
			w.orderByAsc(OrderAssociations::getCreateTime);
		} else {
			w.orderByDesc(OrderAssociations::getCreateTime);
		}
		Page<OrderAssociations> page = new Page<>(pageNo, pageSize, false);
		Page<OrderAssociations> result = orderAssociationsMapper.selectPage(page, w);
		Long totalCt = orderAssociationsMapper.selectCount(cw);
		long total = totalCt == null ? 0L : totalCt;
		List<Map<String, Object>> rows = new ArrayList<>();
		for (OrderAssociations o : result.getRecords()) {
			rows.add(associationToRow(o, companyId));
		}
		return new PageResult(total, rows);
	}

	private LambdaQueryWrapper<NormalOrders> buildNormalWrapper(
			long companyId, AdminOrderListExecutorKind kind, Map<String, Object> filter) {
		LambdaQueryWrapper<NormalOrders> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrders::getCompanyId, companyId);
		applyKindBaseNormal(w, kind, filter);
		applySharedNormalFilters(w, companyId, filter);
		return w;
	}

	private static void applyKindBaseNormal(
			LambdaQueryWrapper<NormalOrders> w, AdminOrderListExecutorKind kind, Map<String, Object> filter) {
		switch (kind) {
			case NORMAL -> {
				w.eq(NormalOrders::getOrderType, "normal");
				String oc = str(filter.get("order_class"));
				if (StringUtils.hasText(oc)) {
					w.eq(NormalOrders::getOrderClass, oc);
				}
			}
			case BARGAIN_NORMAL -> {
				w.eq(NormalOrders::getOrderType, "normal");
				w.eq(NormalOrders::getOrderClass, "bargain");
			}
			case GROUPS_NORMAL -> {
				w.eq(NormalOrders::getOrderType, "normal");
				w.eq(NormalOrders::getOrderClass, "groups");
			}
			case SECKILL_NORMAL -> {
				w.eq(NormalOrders::getOrderType, "normal");
				w.eq(NormalOrders::getOrderClass, "seckill");
			}
			case DRUG_NORMAL -> {
				w.eq(NormalOrders::getOrderType, "normal");
				w.eq(NormalOrders::getOrderClass, "drug");
			}
			case SHOPGUIDE_NORMAL -> {
				w.eq(NormalOrders::getOrderType, "normal");
				w.eq(NormalOrders::getOrderClass, "shopguide");
			}
			case POINTSMALL_NORMAL -> {
				w.eq(NormalOrders::getOrderType, "normal");
				w.eq(NormalOrders::getOrderClass, "pointsmall");
			}
			case EXCARD_NORMAL -> {
				w.eq(NormalOrders::getOrderType, "normal");
				w.eq(NormalOrders::getOrderClass, "excard");
			}
			case COMMUNITY_NORMAL -> {
				w.eq(NormalOrders::getOrderType, "normal");
				w.eq(NormalOrders::getOrderClass, "community");
			}
			case SHOPADMIN_NORMAL -> {
				w.eq(NormalOrders::getOrderType, "normal");
				w.eq(NormalOrders::getOrderClass, "shopadmin");
			}
			case EMPLOYEE_PURCHASE_NORMAL -> {
				w.eq(NormalOrders::getOrderType, "normal");
				w.eq(NormalOrders::getOrderClass, "employee_purchase");
			}
			case MEMBERCARD -> w.eq(NormalOrders::getOrderType, "membercard");
			default -> throw new IllegalStateException("Unsupported normal kind: " + kind);
		}
		Object typeObj = filter.get("type");
		if (typeObj instanceof Number n && n.intValue() != 0) {
			w.eq(NormalOrders::getType, n.intValue());
		}
	}

	private void applySharedNormalFilters(
			LambdaQueryWrapper<NormalOrders> w, long companyId, Map<String, Object> filter) {
		Object merchantId = filter.get("merchant_id");
		if (merchantId != null && StringUtils.hasText(String.valueOf(merchantId))) {
			w.eq(NormalOrders::getMerchantId, longVal(merchantId));
		}
		Object supOpIn = filter.get("supplier_operator_id|in");
		if (supOpIn instanceof Collection<?> c && !c.isEmpty()) {
			List<Long> ids = c.stream().map(AdminOrderListQueryExecutor::longVal).distinct().toList();
			if (!ids.isEmpty()) {
				String inList = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
				w.apply(
						"EXISTS (SELECT 1 FROM orders_rel_supplier rs INNER JOIN supplier su ON su.id = rs.supplier_id AND su.company_id = rs.company_id "
								+ "WHERE rs.order_id = orders_normal_orders.order_id AND rs.company_id = {0} AND su.operator_id IN ("
								+ inList
								+ "))",
						companyId);
			}
		}
		applyIntTimeRange(w, filter, "create_time", NormalOrders::getCreateTime);
		applyIntTimeRange(w, filter, "delivery_time", NormalOrders::getDeliveryTime);
		Object acGt = filter.get("auto_cancel_time|gt");
		if (acGt != null) {
			w.apply("(CAST(auto_cancel_time AS SIGNED) > {0})", longVal(acGt));
		}
		Object orderStatusNeq = filter.get("order_status|neq");
		if (orderStatusNeq != null && StringUtils.hasText(String.valueOf(orderStatusNeq))) {
			w.ne(NormalOrders::getOrderStatus, String.valueOf(orderStatusNeq).trim());
		} else {
			Object orderStatusIn = filter.get("order_status|in");
			if (orderStatusIn instanceof Collection<?> osc && !osc.isEmpty()) {
				List<String> vals = osc.stream().map(String::valueOf).toList();
				w.in(NormalOrders::getOrderStatus, vals);
			} else {
				putEqIfPresent(w, filter, "order_status", NormalOrders::getOrderStatus);
			}
		}
		putEqIfPresent(w, filter, "cancel_status", NormalOrders::getCancelStatus);
		putEqIfPresent(w, filter, "receipt_type", NormalOrders::getReceiptType);
		putEqIfPresent(w, filter, "ziti_status", NormalOrders::getZitiStatus);
		putEqIfPresent(w, filter, "delivery_status", NormalOrders::getDeliveryStatus);
		putEqIfPresent(w, filter, "receiver_name", NormalOrders::getReceiverName);
		putEqIfPresent(w, filter, "receiver_mobile", NormalOrders::getReceiverMobile);
		putEqIfPresent(w, filter, "mobile", NormalOrders::getMobile);
		putEqIfPresent(w, filter, "pay_type", NormalOrders::getPayType);
		Object purchaseMode = filter.get("purchase_mode");
		if (purchaseMode != null && StringUtils.hasText(String.valueOf(purchaseMode))) {
			w.apply(
					"EXISTS (SELECT 1 FROM employee_purchase_orders_rel_activity op "
							+ "WHERE op.order_id = orders_normal_orders.order_id "
							+ "AND op.company_id = {0} AND op.purchase_mode = {1})",
					companyId,
					String.valueOf(purchaseMode).trim());
		}
		putEqIfPresent(w, filter, "user_id", NormalOrders::getUserId);
		putEqIfPresent(w, filter, "order_source", NormalOrders::getOrderSource);
		putEqIfPresent(w, filter, "source_id", NormalOrders::getSourceId);
		putEqIfPresent(w, filter, "monitor_id", NormalOrders::getMonitorId);
		putEqIfPresent(w, filter, "distributor_id", NormalOrders::getDistributorId);
		putEqIfPresent(w, filter, "shop_id", NormalOrders::getShopId);
		Object smIn = filter.get("salesman_id|in");
		if (smIn instanceof Collection<?> smc && !smc.isEmpty()) {
			List<Long> ids = smc.stream().map(AdminOrderListQueryExecutor::longVal).toList();
			w.in(NormalOrders::getSalesmanId, ids);
		} else {
			putEqIfPresent(w, filter, "salesman_id", NormalOrders::getSalesmanId);
		}
		Object promoterUid = filter.get("promoter_user_id");
		if (promoterUid instanceof Collection<?> pc && !pc.isEmpty()) {
			List<Long> ids = pc.stream().map(AdminOrderListQueryExecutor::longVal).toList();
			w.in(NormalOrders::getUserId, ids);
		}
		Object shopIn = filter.get("shop_id|in");
		if (shopIn instanceof Collection<?> sc && !sc.isEmpty()) {
			List<Long> ids = sc.stream().map(AdminOrderListQueryExecutor::longVal).toList();
			w.in(NormalOrders::getShopId, ids);
		}
		Object distIn = filter.get("distributor_id|in");
		if (distIn instanceof Collection<?> dc && !dc.isEmpty()) {
			List<Long> ids = dc.stream().map(AdminOrderListQueryExecutor::longVal).toList();
			w.in(NormalOrders::getDistributorId, ids);
		}
		Object ocIn = filter.get("order_class|in");
		if (ocIn instanceof Collection<?> occ && !occ.isEmpty()) {
			w.in(NormalOrders::getOrderClass, occ.stream().map(String::valueOf).toList());
		}
		Object ocNotIn = filter.get("order_class|notin");
		if (ocNotIn instanceof Collection<?> onc && !onc.isEmpty()) {
			List<String> ex =
					onc.stream()
							.map(String::valueOf)
							.map(String::trim)
							.filter(StringUtils::hasText)
							.distinct()
							.toList();
			if (!ex.isEmpty()) {
				w.notIn(NormalOrders::getOrderClass, ex);
			}
		}
		Object holderIn = filter.get("order_holder|in");
		if (holderIn instanceof Collection<?> hc && !hc.isEmpty()) {
			w.in(NormalOrders::getOrderHolder, hc.stream().map(String::valueOf).toList());
		}
		Object isInvoiced = filter.get("is_invoiced");
		if (isInvoiced != null && StringUtils.hasText(String.valueOf(isInvoiced).trim())) {
			w.eq(NormalOrders::getIsInvoiced, intVal(isInvoiced) != 0);
		}
		Object cancelIn = filter.get("cancel_status|in");
		if (cancelIn instanceof Collection<?> c && !c.isEmpty()) {
			w.in(NormalOrders::getCancelStatus, c.stream().map(String::valueOf).toList());
		}
		Object deliveryIn = filter.get("delivery_status|in");
		if (deliveryIn instanceof Collection<?> c && !c.isEmpty()) {
			w.in(NormalOrders::getDeliveryStatus, c.stream().map(String::valueOf).toList());
		}
		if (truthy(filter.get("invoice_not_empty"))) {
			w.isNotNull(NormalOrders::getInvoice);
			w.ne(NormalOrders::getInvoice, "");
		}
		Object oidLike = filter.get("order_id|like");
		if (oidLike != null && StringUtils.hasText(String.valueOf(oidLike))) {
			String pat = "%" + String.valueOf(oidLike).trim() + "%";
			w.apply("CAST(orders_normal_orders.order_id AS CHAR) LIKE {0}", pat);
		} else {
			Object oidIn = filter.get("order_id|in");
			if (oidIn instanceof Collection<?> oc && !oc.isEmpty()) {
				List<Long> ids =
						oc.stream().map(AdminOrderListQueryExecutor::longVal).filter(id -> id > 0L).distinct().toList();
				if (!ids.isEmpty()) {
					w.in(NormalOrders::getOrderId, ids);
				}
			} else {
				Object oidEq = filter.get("order_id");
				if (oidEq != null && StringUtils.hasText(String.valueOf(oidEq))) {
					long oid = longVal(oidEq);
					if (oid == 0L) {
						w.eq(NormalOrders::getOrderId, 0L);
					} else {
						w.eq(NormalOrders::getOrderId, oid);
					}
				}
			}
		}
		Object isRate = filter.get("is_rate");
		if (isRate != null && StringUtils.hasText(String.valueOf(isRate).trim())) {
			w.eq(NormalOrders::getIsRate, intVal(isRate) != 0);
		}
		if (Boolean.TRUE.equals(filter.get("wxapp_order_is_distribution_eq"))) {
			w.eq(NormalOrders::getIsDistribution, Boolean.TRUE);
		}
		Object titleLike = filter.get("title|like");
		if (titleLike != null && StringUtils.hasText(String.valueOf(titleLike))) {
			w.like(NormalOrders::getTitle, String.valueOf(titleLike).trim());
		}
		Object itemName = filter.get("item_name");
		if (itemName != null && StringUtils.hasText(String.valueOf(itemName))) {
			String like = "%" + String.valueOf(itemName).trim() + "%";
			w.apply(
					"EXISTS (SELECT 1 FROM orders_normal_orders_items i WHERE i.order_id = orders_normal_orders.order_id AND i.company_id = {0} AND i.item_name LIKE {1})",
					companyId,
					like);
		}
		Object actIds = filter.get("act_id");
		if (actIds instanceof Collection<?> c && !c.isEmpty()) {
			w.in(NormalOrders::getActId, c.stream().map(AdminOrderListQueryExecutor::longVal).distinct().toList());
		} else if (actIds != null && StringUtils.hasText(String.valueOf(actIds))) {
			w.eq(NormalOrders::getActId, longVal(actIds));
		}
		Object selfDelOp = filter.get("self_delivery_operator_id");
		if (selfDelOp != null && StringUtils.hasText(String.valueOf(selfDelOp))) {
			w.eq(NormalOrders::getSelfDeliveryOperatorId, longVal(selfDelOp));
		}
		Object sds = filter.get("self_delivery_status");
		if (sds != null && StringUtils.hasText(String.valueOf(sds))) {
			w.eq(NormalOrders::getSelfDeliveryStatus, String.valueOf(sds).trim());
		}
		putEqIfPresent(w, filter, "invoice_status", NormalOrders::getInvoiceStatus);
		Object invNeq = filter.get("invoice_status|neq");
		if (invNeq != null && StringUtils.hasText(String.valueOf(invNeq))) {
			w.ne(NormalOrders::getInvoiceStatus, String.valueOf(invNeq).trim());
		}
		if (filter.containsKey("total_fee|gt")) {
			w.apply("CAST(orders_normal_orders.total_fee AS SIGNED) > {0}", longVal(filter.get("total_fee|gt")));
		}
		applyNotpayAutoCancelConstraint(w, filter);
		applyPrescriptionExists(w, companyId, filter);
	}

	private static void applyNotpayAutoCancelConstraint(LambdaQueryWrapper<NormalOrders> w, Map<String, Object> filter) {
		Object osNeq = filter.get("order_status|neq");
		if (osNeq != null && StringUtils.hasText(String.valueOf(osNeq).trim())) {
			return;
		}
		Object osIn = filter.get("order_status|in");
		if (osIn instanceof Collection<?> c && !c.isEmpty()) {
			return;
		}
		if (!"NOTPAY".equals(str(filter.get("order_status")))) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		w.apply(
				"( (orders_normal_orders.pay_type = {0} AND orders_normal_orders.offline_payment_status = {1}) OR ( (orders_normal_orders.pay_type <> {0} OR orders_normal_orders.offline_payment_status = {2}) AND CAST(orders_normal_orders.auto_cancel_time AS SIGNED) > {3} ) )",
				"offline_pay",
				0,
				-1,
				now);
	}

	private static void applyPrescriptionExists(
			LambdaQueryWrapper<NormalOrders> w, long companyId, Map<String, Object> filter) {
		if (!filter.containsKey("is_prescription_order")) {
			return;
		}
		String uf = str(filter.get("user_family_name"));
		if (StringUtils.hasText(uf)) {
			String like = "%" + uf.trim() + "%";
			w.apply(
					"EXISTS (SELECT 1 FROM orders_prescription pr WHERE pr.company_id = {0} AND CAST(pr.order_id AS UNSIGNED) = orders_normal_orders.order_id AND pr.user_family_name LIKE {1})",
					companyId,
					like);
		} else {
			w.apply(
					"EXISTS (SELECT 1 FROM orders_prescription pr WHERE pr.company_id = {0} AND CAST(pr.order_id AS UNSIGNED) = orders_normal_orders.order_id)",
					companyId);
		}
	}

	private LambdaQueryWrapper<ServiceOrders> buildServiceWrapper(
			long companyId, AdminOrderListExecutorKind kind, Map<String, Object> filter) {
		LambdaQueryWrapper<ServiceOrders> w = new LambdaQueryWrapper<>();
		w.eq(ServiceOrders::getCompanyId, companyId);
		w.eq(ServiceOrders::getOrderType, "service");
		switch (kind) {
			case GROUPS_SERVICE -> w.eq(ServiceOrders::getOrderClass, "groups");
			case SECKILL_SERVICE -> w.eq(ServiceOrders::getOrderClass, "seckill");
			default -> {
			}
		}
		applyIntTimeRangeSvc(w, filter, "create_time", ServiceOrders::getCreateTime);
		Object svcOsNeq = filter.get("order_status|neq");
		if (svcOsNeq != null && StringUtils.hasText(String.valueOf(svcOsNeq))) {
			w.ne(ServiceOrders::getOrderStatus, String.valueOf(svcOsNeq).trim());
		} else {
			Object svcOsIn = filter.get("order_status|in");
			if (svcOsIn instanceof Collection<?> osc && !osc.isEmpty()) {
				w.in(ServiceOrders::getOrderStatus, osc.stream().map(String::valueOf).toList());
			} else {
				putEqIfPresentSvc(w, filter, "order_status", ServiceOrders::getOrderStatus);
			}
		}
		putEqIfPresentSvc(w, filter, "user_id", ServiceOrders::getUserId);
		putEqIfPresentSvc(w, filter, "order_source", ServiceOrders::getOrderSource);
		putEqIfPresentSvc(w, filter, "shop_id", ServiceOrders::getShopId);
		putEqIfPresentSvc(w, filter, "mobile", ServiceOrders::getMobile);
		Object svcSmIn = filter.get("salesman_id|in");
		if (svcSmIn instanceof Collection<?> smc && !smc.isEmpty()) {
			List<Long> smIds = smc.stream().map(AdminOrderListQueryExecutor::longVal).toList();
			w.in(ServiceOrders::getSalesmanId, smIds);
		} else {
			putEqIfPresentSvc(w, filter, "salesman_id", ServiceOrders::getSalesmanId);
		}
		Object titleLike = filter.get("title|like");
		if (titleLike != null && StringUtils.hasText(String.valueOf(titleLike))) {
			w.like(ServiceOrders::getTitle, String.valueOf(titleLike).trim());
		}
		Object oidLike = filter.get("order_id|like");
		if (oidLike != null && StringUtils.hasText(String.valueOf(oidLike))) {
			String pat = "%" + String.valueOf(oidLike).trim() + "%";
			w.apply("CAST(service_orders.order_id AS CHAR) LIKE {0}", pat);
		} else {
			Object oidIn = filter.get("order_id|in");
			if (oidIn instanceof Collection<?> oc && !oc.isEmpty()) {
				List<Long> ids =
						oc.stream().map(AdminOrderListQueryExecutor::longVal).filter(id -> id > 0L).distinct().toList();
				if (!ids.isEmpty()) {
					w.in(ServiceOrders::getOrderId, ids);
				}
			} else {
				Object oidEq = filter.get("order_id");
				if (oidEq != null && StringUtils.hasText(String.valueOf(oidEq))) {
					w.eq(ServiceOrders::getOrderId, longVal(oidEq));
				}
			}
		}
		return w;
	}

	private LambdaQueryWrapper<SupplierOrder> buildSupplierOrderWrapper(long companyId, Map<String, Object> filter) {
		LambdaQueryWrapper<SupplierOrder> w = new LambdaQueryWrapper<>();
		w.eq(SupplierOrder::getCompanyId, companyId);
		Object sup = filter.get("supplier_id");
		if (sup instanceof Collection<?> c && !c.isEmpty()) {
			w.in(SupplierOrder::getSupplierId, c.stream().map(AdminOrderListQueryExecutor::intVal).toList());
		} else if (sup != null && StringUtils.hasText(String.valueOf(sup))) {
			w.eq(SupplierOrder::getSupplierId, intVal(sup));
		}
		applyIntTimeRangeSup(w, filter);
		putEqIfPresentSup(w, filter, "order_status", SupplierOrder::getOrderStatus);
		putEqIfPresentSup(w, filter, "user_id", SupplierOrder::getUserId);
		putEqIfPresentSup(w, filter, "mobile", SupplierOrder::getMobile);
		Object oidLike = filter.get("order_id|like");
		if (oidLike != null && StringUtils.hasText(String.valueOf(oidLike))) {
			w.like(SupplierOrder::getOrderId, String.valueOf(oidLike).trim());
		} else {
			Object oidEq = filter.get("order_id");
			if (oidEq != null && StringUtils.hasText(String.valueOf(oidEq))) {
				w.eq(SupplierOrder::getOrderId, longVal(oidEq));
			}
		}
		return w;
	}

	private LambdaQueryWrapper<OrderAssociations> buildAssociationWrapper(long companyId, Map<String, Object> filter) {
		LambdaQueryWrapper<OrderAssociations> w = new LambdaQueryWrapper<>();
		w.eq(OrderAssociations::getCompanyId, companyId);
		applyIntTimeRangeAsc(w, filter, "create_time", OrderAssociations::getCreateTime);
		putEqIfPresentAsc(w, filter, "order_status", OrderAssociations::getOrderStatus);
		putEqIfPresentAsc(w, filter, "user_id", OrderAssociations::getUserId);
		putEqIfPresentAsc(w, filter, "mobile", OrderAssociations::getMobile);
		putEqIfPresentAsc(w, filter, "shop_id", OrderAssociations::getShopId);
		Object distEq = filter.get("distributor_id");
		if (distEq != null && StringUtils.hasText(String.valueOf(distEq))) {
			w.eq(OrderAssociations::getShopId, longVal(distEq));
		}
		Object distIn = filter.get("distributor_id|in");
		if (distIn instanceof Collection<?> dc && !dc.isEmpty()) {
			List<Long> ids = dc.stream().map(AdminOrderListQueryExecutor::longVal).toList();
			w.in(OrderAssociations::getShopId, ids);
		}
		putEqIfPresentAsc(w, filter, "source_id", OrderAssociations::getSourceId);
		putEqIfPresentAsc(w, filter, "salesman_id", OrderAssociations::getSalesmanId);
		Object promoterUid = filter.get("promoter_user_id");
		if (promoterUid instanceof Collection<?> pc && !pc.isEmpty()) {
			w.in(OrderAssociations::getPromoterUserId, pc.stream().map(AdminOrderListQueryExecutor::longVal).toList());
		}
		Object oidLike = filter.get("order_id|like");
		if (oidLike != null && StringUtils.hasText(String.valueOf(oidLike))) {
			String pat = "%" + String.valueOf(oidLike).trim() + "%";
			w.apply("CAST(orders_associations.order_id AS CHAR) LIKE {0}", pat);
		} else {
			Object oidEq = filter.get("order_id");
			if (oidEq != null && StringUtils.hasText(String.valueOf(oidEq))) {
				w.eq(OrderAssociations::getOrderId, longVal(oidEq));
			}
		}
		return w;
	}

	private static void applyOrdering(LambdaQueryWrapper<NormalOrders> w, boolean createTimeAsc) {
		if (createTimeAsc) {
			w.orderByAsc(NormalOrders::getCreateTime);
		} else {
			w.orderByDesc(NormalOrders::getCreateTime);
		}
	}

	private static Map<String, Object> serviceOrderToRow(ServiceOrders o, long companyId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", o.getOrderId());
		m.put("company_id", companyId);
		m.put("title", o.getTitle());
		m.put("shop_id", o.getShopId());
		m.put("store_name", o.getStoreName());
		m.put("user_id", o.getUserId());
		m.put("item_id", o.getItemId());
		m.put("mobile", o.getMobile());
		m.put("order_class", o.getOrderClass());
		m.put("order_status", o.getOrderStatus());
		m.put("order_type", o.getOrderType());
		m.put("total_fee", o.getTotalFee());
		m.put("salesman_id", o.getSalesmanId());
		m.put("source_id", o.getSourceId());
		m.put("monitor_id", o.getMonitorId());
		m.put("create_time", o.getCreateTime());
		m.put("items", List.of());
		return m;
	}

	private static Map<String, Object> associationToRow(OrderAssociations o, long companyId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", String.valueOf(o.getOrderId()));
		m.put("company_id", companyId);
		m.put("title", o.getTitle());
		m.put("user_id", o.getUserId());
		m.put("mobile", o.getMobile());
		m.put("order_type", o.getOrderType());
		m.put("order_class", o.getOrderClass());
		m.put("order_status", o.getOrderStatus());
		m.put("shop_id", o.getShopId());
		m.put("distributor_id", o.getShopId());
		m.put("source_id", o.getSourceId());
		m.put("monitor_id", o.getMonitorId());
		m.put("promoter_user_id", o.getPromoterUserId());
		m.put("total_fee", o.getTotalFee());
		m.put("create_time", o.getCreateTime());
		m.put("salesman_id", o.getSalesmanId());
		m.put("items", List.of());
		return m;
	}

	private void applyUserDeleteFlags(long companyId, List<Map<String, Object>> rows) {
		List<Long> userIds =
				rows.stream()
						.map(r -> longVal(r.get("user_id")))
						.filter(id -> id > 0)
						.distinct()
						.toList();
		if (userIds.isEmpty()) {
			for (Map<String, Object> r : rows) {
				r.put("user_delete", false);
			}
			return;
		}
		List<MembersDeleteRecord> recs =
				membersDeleteRecordMapper.selectList(
						new LambdaQueryWrapper<MembersDeleteRecord>()
								.eq(MembersDeleteRecord::getCompanyId, companyId)
								.in(MembersDeleteRecord::getUserId, userIds));
		Set<Long> deleted = recs.stream().map(MembersDeleteRecord::getUserId).collect(Collectors.toSet());
		for (Map<String, Object> r : rows) {
			long uid = longVal(r.get("user_id"));
			r.put("user_delete", uid > 0 && deleted.contains(uid));
		}
	}

	private static void applyIntTimeRange(
			LambdaQueryWrapper<NormalOrders> w,
			Map<String, Object> filter,
			String col,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<NormalOrders, Integer> getter) {
		Object gte = filter.get(col + "|gte");
		Object lte = filter.get(col + "|lte");
		if (gte != null) {
			w.ge(getter, (int) longVal(gte));
		}
		if (lte != null) {
			w.le(getter, (int) longVal(lte));
		}
	}

	private static <T> void applyIntTimeRangeSvc(
			LambdaQueryWrapper<T> w,
			Map<String, Object> filter,
			String col,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, Integer> getter) {
		Object gte = filter.get(col + "|gte");
		Object lte = filter.get(col + "|lte");
		if (gte != null) {
			w.ge(getter, (int) longVal(gte));
		}
		if (lte != null) {
			w.le(getter, (int) longVal(lte));
		}
	}

	private static void applyIntTimeRangeSup(LambdaQueryWrapper<SupplierOrder> w, Map<String, Object> filter) {
		Object gte = filter.get("create_time|gte");
		Object lte = filter.get("create_time|lte");
		if (gte != null) {
			w.ge(SupplierOrder::getCreateTime, (int) longVal(gte));
		}
		if (lte != null) {
			w.le(SupplierOrder::getCreateTime, (int) longVal(lte));
		}
	}

	private static <T> void applyIntTimeRangeAsc(
			LambdaQueryWrapper<T> w,
			Map<String, Object> filter,
			String col,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, Integer> getter) {
		Object gte = filter.get(col + "|gte");
		Object lte = filter.get(col + "|lte");
		if (gte != null) {
			w.ge(getter, (int) longVal(gte));
		}
		if (lte != null) {
			w.le(getter, (int) longVal(lte));
		}
	}

	private static void putEqIfPresent(
			LambdaQueryWrapper<NormalOrders> w,
			Map<String, Object> filter,
			String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<NormalOrders, ?> col) {
		Object v = filter.get(key);
		if (v == null) {
			return;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		if ("user_id".equals(key)
				|| "shop_id".equals(key)
				|| "source_id".equals(key)
				|| "distributor_id".equals(key)
				|| "monitor_id".equals(key)
				|| "salesman_id".equals(key)) {
			w.eq(col, longVal(v));
			return;
		}
		w.eq(col, s);
	}

	private static <T> void putEqIfPresentSvc(
			LambdaQueryWrapper<T> w,
			Map<String, Object> filter,
			String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> col) {
		Object v = filter.get(key);
		if (v == null) {
			return;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		if ("user_id".equals(key) || "shop_id".equals(key) || "salesman_id".equals(key)) {
			w.eq(col, longVal(v));
			return;
		}
		w.eq(col, s);
	}

	private static <T> void putEqIfPresentSup(
			LambdaQueryWrapper<T> w,
			Map<String, Object> filter,
			String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> col) {
		Object v = filter.get(key);
		if (v == null) {
			return;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		if ("user_id".equals(key)) {
			w.eq(col, longVal(v));
			return;
		}
		w.eq(col, s);
	}

	private static <T> void putEqIfPresentAsc(
			LambdaQueryWrapper<T> w,
			Map<String, Object> filter,
			String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> col) {
		Object v = filter.get(key);
		if (v == null) {
			return;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		if ("user_id".equals(key)
				|| "shop_id".equals(key)
				|| "distributor_id".equals(key)
				|| "source_id".equals(key)
				|| "salesman_id".equals(key)) {
			w.eq(col, longVal(v));
			return;
		}
		w.eq(col, s);
	}

	private static boolean truthy(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		return "true".equalsIgnoreCase(String.valueOf(o).trim()) || "1".equals(String.valueOf(o).trim());
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

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
