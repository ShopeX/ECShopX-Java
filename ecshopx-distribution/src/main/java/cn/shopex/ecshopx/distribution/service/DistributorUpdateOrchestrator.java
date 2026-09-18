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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.dispatch.DistributionEditEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributorUpdateEventDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.integration.JushuitanSettingReadService;
import cn.shopex.ecshopx.distribution.integration.LocalDeliveryShopCreateClient;
import cn.shopex.ecshopx.distribution.integration.WdtErpShopQueryClient;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.distribution.support.CompanyMenuTypeProductModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class DistributorUpdateOrchestrator {

	private final HfpayLedgerConfigReadService hfpayLedgerConfigReadService;
	private final LocalDeliveryShopCreateClient localDeliveryShopCreateClient;
	private final WdtErpShopQueryClient wdtErpShopQueryClient;
	private final JushuitanSettingReadService jushuitanSettingReadService;
	private final DistributorWriteRepository distributorWriteRepository;
	private final DistributorUpdateService distributorUpdateService;
	private final DistributorAftersalesAddressWriteService distributorAftersalesAddressWriteService;
	private final DistributorAftersalesAddressReadService distributorAftersalesAddressReadService;
	private final CompanysMapper companysMapper;
	private final DistributionEditEventDispatchPublisher distributionEditEventDispatchPublisher;
	private final DistributorUpdateEventDispatchPublisher distributorUpdateEventDispatchPublisher;

	public DistributorUpdateOrchestrator(
			HfpayLedgerConfigReadService hfpayLedgerConfigReadService,
			LocalDeliveryShopCreateClient localDeliveryShopCreateClient,
			WdtErpShopQueryClient wdtErpShopQueryClient,
			JushuitanSettingReadService jushuitanSettingReadService,
			DistributorWriteRepository distributorWriteRepository,
			DistributorUpdateService distributorUpdateService,
			DistributorAftersalesAddressWriteService distributorAftersalesAddressWriteService,
			DistributorAftersalesAddressReadService distributorAftersalesAddressReadService,
			CompanysMapper companysMapper,
			DistributionEditEventDispatchPublisher distributionEditEventDispatchPublisher,
			DistributorUpdateEventDispatchPublisher distributorUpdateEventDispatchPublisher) {
		this.hfpayLedgerConfigReadService = hfpayLedgerConfigReadService;
		this.localDeliveryShopCreateClient = localDeliveryShopCreateClient;
		this.wdtErpShopQueryClient = wdtErpShopQueryClient;
		this.jushuitanSettingReadService = jushuitanSettingReadService;
		this.distributorWriteRepository = distributorWriteRepository;
		this.distributorUpdateService = distributorUpdateService;
		this.distributorAftersalesAddressWriteService = distributorAftersalesAddressWriteService;
		this.distributorAftersalesAddressReadService = distributorAftersalesAddressReadService;
		this.companysMapper = companysMapper;
		this.distributionEditEventDispatchPublisher = distributionEditEventDispatchPublisher;
		this.distributorUpdateEventDispatchPublisher = distributorUpdateEventDispatchPublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> update(
			Map<String, Object> merged,
			Map<String, Object> user,
			long pathDistributorId,
			String requestLangTag,
			Set<String> datapassBlockCols) {
		long companyId = toLong(merged.get("company_id"));
		if (truthyIsOpen(merged.get("is_open"))) {
			hfpayLedgerConfigReadService.assertOpenAndRateAllowedIfNeeded(companyId, merged);
		}
		Optional<Distributor> distributorInfoOpt = distributorWriteRepository.selectSimpleByCompanyAndId(companyId, pathDistributorId);
		Distributor distributorInfo = distributorInfoOpt.orElse(null);
		if (datapassBlockCols != null && !datapassBlockCols.isEmpty() && distributorInfo != null) {
			applyDatapassRefill(merged, distributorInfo, datapassBlockCols);
		}
		String shopCode = str(merged.get("shop_code"));
		if (StringUtils.hasText(shopCode)) {
			String name = str(merged.get("name"));
			if (StringUtils.hasText(name)) {
				Optional<Distributor> dup =
						distributorWriteRepository.selectByCompanyAndNameNotSelf(companyId, name, pathDistributorId);
				if (dup.isPresent()) {
					throw new ResourceException("修改的店铺名称已存在");
				}
			}
		}
		if (distributorInfo != null) {
			localDeliveryShopCreateClient.applyUpdateForDistributor(companyId, pathDistributorId, merged, distributorInfo);
		}
		wdtErpShopQueryClient.assertShopExistsIfWdtNo(companyId, merged);
		jushuitanSettingReadService.assertEnabledIfJstIdPositive(companyId, merged);

		if (merged.containsKey("offline_aftersales_distributor_id") && distributorInfo != null) {
			String joined = str(merged.get("offline_aftersales_distributor_id"));
			if (StringUtils.hasText(joined)) {
				long merchantForCheck = merged.containsKey("merchant_id") && toLong(merged.get("merchant_id")) > 0
						? toLong(merged.get("merchant_id"))
						: distributorInfo.getMerchantId();
				List<Long> idList = parseCommaLongIds(joined);
				if (merchantForCheck > 0
						&& !idList.isEmpty()
						&& distributorWriteRepository.countMerchantMismatchForOfflineAftersalesIds(companyId, merchantForCheck, idList)
								> 0) {
					throw new ResourceException("售后门店只能选择同一商户下的其他门店");
				}
			}
		}

		Map<String, Object> row = distributorUpdateService.performUpdateAndEvents(merged, pathDistributorId, requestLangTag);

		Map<String, Object> rowSnapshot = new LinkedHashMap<>(row);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				distributionEditEventDispatchPublisher.publish(rowSnapshot);
				distributorUpdateEventDispatchPublisher.publish(rowSnapshot);
			}
		});

		@SuppressWarnings("unchecked")
		Map<String, Object> addr = (Map<String, Object>) merged.get("offline_aftersales_address");
		if (addr != null && StringUtils.hasText(str(addr.get("name")))
				&& StringUtils.hasText(str(addr.get("address")))
				&& StringUtils.hasText(str(addr.get("mobile")))) {
			String areaCode = str(addr.get("area_code"));
			if (StringUtils.hasText(areaCode)) {
				addr.put("mobile", areaCode + "-" + str(addr.get("mobile")));
			}
			long merchantId = toLong(user.get("merchant_id"));
			String productModelSlug = resolveProductModelSlug(companyId);
			boolean exists = distributorAftersalesAddressReadService.existsOfflineAftersalesAddress(companyId, pathDistributorId);
			if (!exists) {
				distributorAftersalesAddressWriteService.setAfterSalesAddressForNewDistributor(
						companyId, pathDistributorId, merchantId, addr, productModelSlug);
			} else {
				distributorAftersalesAddressWriteService.updateOfflineAfterSalesAddress(companyId, pathDistributorId, addr);
			}
		}

		return row;
	}

	private void applyDatapassRefill(Map<String, Object> merged, Distributor info, Set<String> cols) {
		for (String col : cols) {
			if ("mobile".equals(col) && info.getMobile() != null) {
				merged.put("mobile", info.getMobile());
			}
			if ("contact".equals(col) && info.getContact() != null) {
				merged.put("contact", info.getContact());
			}
		}
	}

	private String resolveProductModelSlug(long companyId) {
		Companys c = companysMapper.selectById(companyId);
		if (c == null || c.getMenuType() == null) {
			return "platform";
		}
		return CompanyMenuTypeProductModel.toSlug(c.getMenuType());
	}

	private static List<Long> parseCommaLongIds(String s) {
		List<Long> out = new ArrayList<>();
		for (String part : s.split(",")) {
			String t = part.trim();
			if (StringUtils.hasText(t)) {
				try {
					out.add(Long.parseLong(t));
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
		}
		return out;
	}

	private static boolean truthyIsOpen(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString().trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
