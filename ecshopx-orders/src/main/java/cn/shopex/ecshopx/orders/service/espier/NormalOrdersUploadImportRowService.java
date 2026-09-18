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

package cn.shopex.ecshopx.orders.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrdersUploadImportRowService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final NormalOrdersEspierDeliveryCorpResolver deliveryCorpResolver;
	private final NormalOrderEspierBatchDeliveryService batchDeliveryService;

	public NormalOrdersUploadImportRowService(
			NormalOrdersMapper normalOrdersMapper,
			SupplierOrderMapper supplierOrderMapper,
			NormalOrdersEspierDeliveryCorpResolver deliveryCorpResolver,
			NormalOrderEspierBatchDeliveryService batchDeliveryService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.deliveryCorpResolver = deliveryCorpResolver;
		this.batchDeliveryService = batchDeliveryService;
	}

	public void acceptRow(
			long companyId,
			long operatorId,
			long distributorId,
			long supplierId,
			long merchantId,
			Map<String, Object> row,
			@SuppressWarnings("unused") String operatorType) {
		String orderIdRaw = sanitizeImportOrderId(trim(row.get("order_id")));
		if (!StringUtils.hasText(orderIdRaw)) {
			throw new BadRequestException("订单号错误");
		}
		long orderId = parseOrderId(orderIdRaw);
		String deliveryCode = trim(row.get("delivery_code"));
		String deliveryCorpName = trim(row.get("delivery_corp_name"));
		if (!StringUtils.hasText(deliveryCode) || !StringUtils.hasText(deliveryCorpName)) {
			throw new BadRequestException("缺少快递信息");
		}
		// 平台账号导入不得通过行内 supplier_id 代发供应商商品；仅供应商登录可按自身 supplierId 发货
		long rowSupplierId = supplierId;
		if (supplierId > 0L) {
			long parsed = parseSupplierIdAsInt(row.get("supplier_id"));
			if (parsed > 0L) {
				rowSupplierId = parsed;
			}
		} else {
			rowSupplierId = 0L;
		}
		NormalOrders order;
		if (rowSupplierId > 0L) {
			SupplierOrder so =
					supplierOrderMapper.selectOne(
							new LambdaQueryWrapper<SupplierOrder>()
									.eq(SupplierOrder::getCompanyId, companyId)
									.eq(SupplierOrder::getOrderId, orderId)
									.eq(SupplierOrder::getSupplierId, rowSupplierId));
			if (so == null) {
				throw new BadRequestException("订单不存在");
			}
			order = loadNormalOrder(companyId, orderId);
		} else {
			order = loadNormalOrder(companyId, orderId);
		}
		if (order == null) {
			throw new BadRequestException("订单不存在");
		}
		if ("CANCEL".equalsIgnoreCase(safe(order.getOrderStatus()))) {
			throw new BadRequestException("已取消订单不能发货");
		}
		String deliveryCorp =
				deliveryCorpResolver.resolveDeliveryCorpCode(companyId, deliveryCorpName, rowSupplierId);
		batchDeliveryService.deliverBatchForUpload(companyId, orderId, rowSupplierId, deliveryCorp, deliveryCode);
	}

	private NormalOrders loadNormalOrder(long companyId, long orderId) {
		return normalOrdersMapper.selectOne(
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId));
	}

	private static long parseOrderId(String raw) {
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("订单号格式错误");
		}
	}

	/** Strip export-CSV quotes, tab padding, and UTF-8 BOM from order_id. */
	private static String sanitizeImportOrderId(String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		String t = s.replace("\uFEFF", "");
		if (t.startsWith("\u00EF\u00BB\u00BF")) {
			t = t.substring(3);
		}
		int start = 0;
		int end = t.length();
		while (start < end && isOrderIdTrimChar(t.charAt(start))) {
			start++;
		}
		while (end > start && isOrderIdTrimChar(t.charAt(end - 1))) {
			end--;
		}
		return t.substring(start, end);
	}

	private static boolean isOrderIdTrimChar(char c) {
		return c == '"' || c == '\'' || c == ' ' || c == '\t' || c == '\r' || c == '\n';
	}

	/** Empty / blank supplier_id is 0 (whole-order ship). */
	private static long parseSupplierIdAsInt(Object raw) {
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
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String trim(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}
}
