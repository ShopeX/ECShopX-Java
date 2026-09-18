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

package cn.shopex.ecshopx.orders.service.orderexport;

import cn.shopex.ecshopx.common.dispatch.OrderListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.service.orderexport.support.OrderExportDadaOrderIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OrderExportService {

	private static final Set<String> ALLOWED_ORDER_TYPES = Set.of("normal", "service", "supplier_order");

	private final OrderExportFilterAssembler filterAssembler;
	private final OrderExportDadaOrderIdResolver orderExportDadaOrderIdResolver;
	private final OrderExportCountCoordinator orderExportCountCoordinator;
	private final OrderListExportFileJobDispatchPublisher orderListExportFileJobDispatchPublisher;

	public OrderExportService(
			OrderExportFilterAssembler filterAssembler,
			OrderExportDadaOrderIdResolver orderExportDadaOrderIdResolver,
			OrderExportCountCoordinator orderExportCountCoordinator,
			OrderListExportFileJobDispatchPublisher orderListExportFileJobDispatchPublisher) {
		this.filterAssembler = filterAssembler;
		this.orderExportDadaOrderIdResolver = orderExportDadaOrderIdResolver;
		this.orderExportCountCoordinator = orderExportCountCoordinator;
		this.orderListExportFileJobDispatchPublisher = orderListExportFileJobDispatchPublisher;
	}

	public void exportOrderData(
			long companyId,
			long operatorId,
			String operatorType,
			Long merchantIdOrNull,
			List<Long> distributorIdsFromJwt,
			List<Long> shopIdsFromJwt,
			HttpServletRequest request) {
		OrderExportAssemblyResult assembled =
				filterAssembler.assemble(
						companyId,
						operatorId,
						operatorType,
						merchantIdOrNull,
						distributorIdsFromJwt,
						shopIdsFromJwt,
						request);

		String orderTypeKey = assembled.orderTypeKey();
		if (!ALLOWED_ORDER_TYPES.contains(orderTypeKey)) {
			throw new ResourceException("无此类型订单！");
		}

		LinkedHashMap<String, Object> filter = assembled.filter();
		String rawOrderStatus = request.getParameter("order_status");
		if (rawOrderStatus != null && rawOrderStatus.startsWith("dada_")) {
			if ("supplier_order".equals(orderTypeKey)) {
				throw new ResourceException("无此类型订单！");
			}
			List<Long> dadaIds = orderExportDadaOrderIdResolver.resolve(filter);
			if (dadaIds.isEmpty()) {
				throw new ResourceException("导出有误,暂无数据导出");
			}
			filter.put("order_id|in", dadaIds);
			filter.remove("order_id");
			filter.remove("order_status");
			filter.remove("order_status|in");
		}

		if ("normal".equals(orderTypeKey)) {
			if (request.getParameter("subdistrict_parent_id") != null) {
				filter.put("subdistrict_parent_id", String.valueOf(request.getParameter("subdistrict_parent_id")));
			}
			if (request.getParameter("subdistrict_id") != null) {
				filter.put("subdistrict_id", String.valueOf(request.getParameter("subdistrict_id")));
			}
		}

		long count = orderExportCountCoordinator.count(orderTypeKey, assembled.exportTypeForJob(), filter);

		if (request.getParameter("promoter_identity") != null) {
			filter.put("promoter_identity", String.valueOf(request.getParameter("promoter_identity")));
		}
		if (request.getParameter("promoter_mobile") != null) {
			filter.put("promoter_mobile", String.valueOf(request.getParameter("promoter_mobile")));
		}

		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}

		LinkedHashMap<String, Object> filterForJob = new LinkedHashMap<>(filter);
		if ("supplier_order".equals(orderTypeKey)) {
			filterForJob.remove("order_type");
		}

		orderListExportFileJobDispatchPublisher.enqueueOrderListExport(
				companyId, operatorId, assembled.exportTypeForJob(), copyFilter(filterForJob));
	}

	private static LinkedHashMap<String, Object> copyFilter(LinkedHashMap<String, Object> source) {
		return new LinkedHashMap<>(source);
	}
}
