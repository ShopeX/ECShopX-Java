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

package cn.shopex.ecshopx.orders.service.invoice.export;

import cn.shopex.ecshopx.common.dispatch.OrderListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class InvoiceExportService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final InvoiceExportFilterAssembler invoiceExportFilterAssembler;
	private final OrderListExportFileJobDispatchPublisher orderListExportFileJobDispatchPublisher;

	public InvoiceExportService(
			NormalOrdersMapper normalOrdersMapper,
			SupplierOrderMapper supplierOrderMapper,
			InvoiceExportFilterAssembler invoiceExportFilterAssembler,
			OrderListExportFileJobDispatchPublisher orderListExportFileJobDispatchPublisher) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.invoiceExportFilterAssembler = invoiceExportFilterAssembler;
		this.orderListExportFileJobDispatchPublisher = orderListExportFileJobDispatchPublisher;
	}

	public void exportInvoiceData(
			long companyId,
			long operatorId,
			String operatorType,
			Long merchantIdOrNull,
			List<Long> distributorIdsFromJwt,
			HttpServletRequest request) {
		LinkedHashMap<String, Object> filter =
				invoiceExportFilterAssembler.assemble(
						companyId, operatorId, operatorType, merchantIdOrNull, distributorIdsFromJwt, request);

		String orderTypeRaw = request.getParameter("order_type");
		String orderTypeNorm = orderTypeRaw == null ? "" : orderTypeRaw.trim().toLowerCase(Locale.ROOT);
		if (!"normal".equals(orderTypeNorm) && !"supplier_order".equals(orderTypeNorm)) {
			throw new ResourceException("无此类型订单！");
		}

		long count;
		if ("normal".equals(orderTypeNorm)) {
			count =
					normalOrdersMapper.selectCount(
							InvoiceExportNormalOrderQuerySupport.toCountWrapper(companyId, filter));
		} else {
			count =
					supplierOrderMapper.selectCount(
							InvoiceExportSupplierOrderQuerySupport.toCountWrapper(companyId, filter));
		}

		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}

		orderListExportFileJobDispatchPublisher.enqueueInvoiceExport(companyId, operatorId, new LinkedHashMap<>(filter));
	}
}
