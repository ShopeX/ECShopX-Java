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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.domain.SalespersonItemCartRow;
import cn.shopex.ecshopx.salesperson.domain.SalespersonItemsBarcodeRow;
import cn.shopex.ecshopx.salesperson.repository.SalespersonItemBarcodeReadRepository;
import cn.shopex.ecshopx.salesperson.repository.SalespersonItemCartReadRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SalespersonScanCodeAddCartService {

	private static final Logger log = LoggerFactory.getLogger(SalespersonScanCodeAddCartService.class);

	private final SalespersonItemBarcodeReadRepository itemBarcodeReadRepository;

	private final SalespersonItemCartReadRepository itemCartReadRepository;

	private final SalespersonCartAddDataService salespersonCartAddDataService;

	public SalespersonScanCodeAddCartService(
			SalespersonItemBarcodeReadRepository itemBarcodeReadRepository,
			SalespersonItemCartReadRepository itemCartReadRepository,
			SalespersonCartAddDataService salespersonCartAddDataService) {
		this.itemBarcodeReadRepository = itemBarcodeReadRepository;
		this.itemCartReadRepository = itemCartReadRepository;
		this.salespersonCartAddDataService = salespersonCartAddDataService;
	}

	public Map<String, Object> scanCodeSales(
			HttpServletRequest request, Map<String, Object> mergedInput, Map<String, Object> authMap) {
		Object rawBarcode = mergedInput.containsKey("barcode") ? mergedInput.get("barcode") : Integer.valueOf(0);
		String barcodeStr = rawBarcode == null ? "0" : String.valueOf(rawBarcode).trim();

		long companyId = longVal(authMap.get("company_id"));
		long distributorId = longVal(authMap.get("distributor_id"));

		SalespersonItemsBarcodeRow row = itemBarcodeReadRepository.findFirstByCompanyIdAndDistributorIdAndBarcode(
				companyId, distributorId, barcodeStr);
		Long itemIdFromBarcode = (row == null || row.getItemId() == null) ? null : row.getItemId();

		Map<String, Object> ifilter = new LinkedHashMap<>();
		ifilter.put("item_id", itemIdFromBarcode);
		ifilter.put("company_id", companyId);
		log.info("扫码查询商品{}", ifilter);
		log.info("扫码查询商品导购{}", authMap);

		long queryItemId = itemIdFromBarcode == null || itemIdFromBarcode <= 0L ? 0L : itemIdFromBarcode;
		SalespersonItemCartRow tempItem = itemCartReadRepository.getByItemIdAndCompany(queryItemId, companyId);
		if (tempItem == null) {
			throw new ResourceException("商品找不到.");
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("distributor_id", authMap.get("distributor_id"));
		filter.put("company_id", authMap.get("company_id"));
		filter.put("item_id", tempItem.getItemId());
		filter.put("salesperson_id", authMap.get("salesperson_id"));

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("num", 1);
		params.put("is_checked", true);

		Map<String, Object> addResult = salespersonCartAddDataService.addCartdata(filter, params, true);
		if (truthyCartId(addResult.get("cart_id"))) {
			return Map.of("status", true, "msg", "加入购物车成功");
		}
		return Map.of("status", false, "msg", "加入购物车失败");
	}

	private static boolean truthyCartId(Object cartId) {
		if (cartId == null) {
			return false;
		}
		if (cartId instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = cartId.toString().trim();
		return !s.isEmpty() && !"0".equals(s);
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
