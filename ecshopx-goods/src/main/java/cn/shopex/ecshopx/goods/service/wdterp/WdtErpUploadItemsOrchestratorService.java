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

package cn.shopex.ecshopx.goods.service.wdterp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.dispatch.UploadItemsToWdtErpJobDispatchPublisher;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WdtErpUploadItemsOrchestratorService {

	private static final int PAGE_SIZE = 100;

	private final DistributorListQueryService distributorListQueryService;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final WdtErpUploadItemsPageQueryService wdtErpUploadItemsPageQueryService;
	private final UploadItemsToWdtErpJobDispatchPublisher uploadItemsToWdtErpJobDispatchPublisher;
	private final StringRedisTemplate companysRedisTemplate;

	public WdtErpUploadItemsOrchestratorService(
			DistributorListQueryService distributorListQueryService,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			WdtErpUploadItemsPageQueryService wdtErpUploadItemsPageQueryService,
			UploadItemsToWdtErpJobDispatchPublisher uploadItemsToWdtErpJobDispatchPublisher,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.distributorListQueryService = distributorListQueryService;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.wdtErpUploadItemsPageQueryService = wdtErpUploadItemsPageQueryService;
		this.uploadItemsToWdtErpJobDispatchPublisher = uploadItemsToWdtErpJobDispatchPublisher;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public void run(
			HttpServletRequest request,
			long companyId,
			String operatorType,
			long distributorId,
			Map<String, Object> mergedInput) {
		if (distributorId > 0) {
			List<Distributor> dist = distributorListQueryService.listByIdsAndCompany(companyId, List.of(distributorId));
			if (dist == null || dist.isEmpty()) {
				throw new ResourceException("店铺没有绑定旺店通ERP门店");
			}
			String wdtNo = dist.get(0).getWdtShopNo();
			if (!StringUtils.hasText(wdtNo)) {
				throw new ResourceException("店铺没有绑定旺店通ERP门店");
			}
		}

		boolean isAll = false;
		Map<String, Object> filterBase = new LinkedHashMap<>();
		filterBase.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		filterBase.put("item_type", "normal");
		filterBase.put("is_default_true", Boolean.TRUE);
		filterBase.put("audit_status", "approved");

		Object itemIdRaw = mergedInput != null ? mergedInput.get("item_id") : null;
		if (itemIdRaw == null && request != null) {
			String qp = request.getParameter("item_id");
			if (StringUtils.hasText(qp)) {
				itemIdRaw = qp.trim();
			}
		}
		if (ValuePresence.hasEffectiveValue(itemIdRaw)) {
			long singleId = parsePositiveLong(itemIdRaw);
			filterBase.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, List.of(singleId));
		} else {
			String last = companysRedisTemplate.opsForValue().get(lastUploadRedisKey(companyId));
			int lower = 0;
			if (StringUtils.hasText(last)) {
				try {
					lower = (int) Long.parseLong(last.trim());
				} catch (NumberFormatException ignored) {
					lower = 0;
				}
			}
			if (lower > 0) {
				filterBase.put(ItemsListQueryRepository.KEY_UPDATED_GTE, lower);
			}
			isAll = true;
		}

		String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);

		int page = 1;
		Map<String, Object> filterResponseOut = new LinkedHashMap<>();
		long totalCount;
		do {
			Map<String, Object> pageResult = wdtErpUploadItemsPageQueryService.fetchPage(
					companyId,
					operatorType,
					distributorId,
					productModel,
					filterBase,
					page,
					PAGE_SIZE,
					filterResponseOut);
			totalCount = toLong(pageResult.get("total_count"));
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) pageResult.get("list");
			if (list != null && !list.isEmpty()) {
				List<Long> itemIds = collectItemIdsFromRows(list);
				if (!itemIds.isEmpty()) {
					uploadItemsToWdtErpJobDispatchPublisher.enqueueUploadItemsToWdtErp(companyId, itemIds, distributorId);
				}
			}
			page++;
		} while ((long) (page - 1) * PAGE_SIZE < totalCount);

		if (isAll) {
			companysRedisTemplate.opsForValue().set(lastUploadRedisKey(companyId), String.valueOf(System.currentTimeMillis() / 1000L));
		}
	}

	private static List<Long> collectItemIdsFromRows(List<Map<String, Object>> list) {
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Object iid = row.get("item_id");
			if (iid instanceof Number n) {
				itemIds.add(n.longValue());
			} else if (iid != null && StringUtils.hasText(iid.toString())) {
				try {
					itemIds.add(Long.parseLong(iid.toString().trim()));
				} catch (NumberFormatException ignored) {
					// skip bad row
				}
			}
		}
		return itemIds;
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parsePositiveLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(raw.toString().trim());
	}

	static String lastUploadRedisKey(long companyId) {
		return "WdtErpLastUploadTime:" + sha1Hex(String.valueOf(companyId));
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
