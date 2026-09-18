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

package cn.shopex.ecshopx.supplier.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.integration.SupplierItemsReviewGoodsExecutor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierItemBatchReviewService {

	private final SupplierItemsReviewGoodsExecutor supplierItemsReviewGoodsExecutor;

	public SupplierItemBatchReviewService(SupplierItemsReviewGoodsExecutor supplierItemsReviewGoodsExecutor) {
		this.supplierItemsReviewGoodsExecutor = supplierItemsReviewGoodsExecutor;
	}

	public List<Long> batchReviewItems(long companyIdFromJwt, Map<String, Object> body) {
		String auditRaw = Objects.toString(body.get("audit_status"), "").trim();
		if (!"rejected".equals(auditRaw) && !"approved".equals(auditRaw)) {
			throw new BadRequestException("审核结果错误");
		}
		String itemIdsStr = Objects.toString(body.get("item_ids"), "").trim();
		if (!StringUtils.hasText(itemIdsStr)) {
			throw new BadRequestException("请选择审核商品");
		}
		String auditReason = Objects.toString(body.get("audit_reason"), "").trim();
		if ("rejected".equals(auditRaw) && !StringUtils.hasText(auditReason)) {
			throw new BadRequestException("请输入拒绝原因");
		}
		Map<String, Object> params = new LinkedHashMap<>(body);
		params.put("company_id", companyIdFromJwt);
		params.put("audit_status", auditRaw);
		params.put("audit_reason", auditReason);

		List<Long> idList = new ArrayList<>();
		for (String segment : itemIdsStr.split(",")) {
			String s = segment == null ? "" : segment.trim();
			if (!StringUtils.hasText(s)) {
				continue;
			}
			try {
				idList.add(Long.parseLong(s));
			} catch (NumberFormatException e) {
				throw new BadRequestException("item_ids 格式错误");
			}
		}
		if (idList.isEmpty()) {
			throw new BadRequestException("请选择审核商品");
		}

		List<Long> result = new ArrayList<>();
		for (long pathItemId : idList) {
			Map<String, Object> itemParams = new LinkedHashMap<>(params);
			supplierItemsReviewGoodsExecutor.reviewGoods(itemParams, pathItemId);
			result.add(pathItemId);
		}
		return result;
	}
}
