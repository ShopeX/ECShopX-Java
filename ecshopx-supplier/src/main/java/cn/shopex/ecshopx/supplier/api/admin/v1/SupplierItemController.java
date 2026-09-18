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

package cn.shopex.ecshopx.supplier.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.supplier.service.SupplierItemBatchReviewService;
import cn.shopex.ecshopx.supplier.service.SupplierItemBatchSyncToPoolService;
import cn.shopex.ecshopx.supplier.service.SupplierItemBatchSyncToShopService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("supplierAdminV1SupplierItem")
@RequestMapping("/api/v1/supplier")
public class SupplierItemController {

	private final SupplierItemBatchReviewService supplierItemBatchReviewService;
	private final SupplierItemBatchSyncToPoolService supplierItemBatchSyncToPoolService;
	private final SupplierItemBatchSyncToShopService supplierItemBatchSyncToShopService;

	public SupplierItemController(
			SupplierItemBatchReviewService supplierItemBatchReviewService,
			SupplierItemBatchSyncToPoolService supplierItemBatchSyncToPoolService,
			SupplierItemBatchSyncToShopService supplierItemBatchSyncToShopService) {
		this.supplierItemBatchReviewService = supplierItemBatchReviewService;
		this.supplierItemBatchSyncToPoolService = supplierItemBatchSyncToPoolService;
		this.supplierItemBatchSyncToShopService = supplierItemBatchSyncToShopService;
	}

	@Activated(routeAlias = "supplier.batch_review_items")
	@PostMapping(value = "/batch_review_items", name = "批量审核供应商商品")
	public ResponseEntity<List<Long>> batchReviewItems(
			HttpServletRequest httpRequest, @FlexibleBody Map<String, Object> body) {
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		Object raw = httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		List<Long> out = supplierItemBatchReviewService.batchReviewItems(companyId, body);
		return ResponseEntity.ok(out);
	}

	@Activated(routeAlias = "supplier.batch_sync_to_pool")
	@PostMapping(value = "/batch_sync_to_pool", name = "批量同步供应商商品到商品池")
	public ResponseEntity<List<Long>> batchSyncToPool(
			HttpServletRequest httpRequest, @FlexibleBody Map<String, Object> body) {
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		Object raw = httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		List<Long> out = supplierItemBatchSyncToPoolService.batchSyncToPool(companyId, body);
		return ResponseEntity.ok(out);
	}

	@Activated(routeAlias = "supplier.batch_sync_to_shop")
	@PostMapping(value = "/batch_sync_to_shop", name = "批量同步供应商商品到店铺")
	public ResponseEntity<List<Long>> batchSyncToShop(
			HttpServletRequest httpRequest, @FlexibleBody Map<String, Object> body) {
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		Object raw = httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		long distributorId = parsePositiveLongClaim(jwt, "distributor_id", "distributor_id 无效");
		List<Long> out = supplierItemBatchSyncToShopService.batchSyncToShop(companyId, distributorId, jwt, body);
		return ResponseEntity.ok(out);
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> m) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static long parsePositiveLongClaim(Map<String, Object> jwt, String key, String invalidMsg) {
		Object cid = jwt.get(key);
		if (cid == null) {
			throw new BadRequestException(invalidMsg);
		}
		long result;
		if (cid instanceof Number n) {
			result = n.longValue();
		} else if (cid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException(invalidMsg);
			}
			try {
				result = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		} else {
			try {
				result = Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		}
		if (result <= 0L) {
			throw new BadRequestException(invalidMsg);
		}
		return result;
	}
}
