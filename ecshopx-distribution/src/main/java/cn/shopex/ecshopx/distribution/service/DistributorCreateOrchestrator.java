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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.distribution.integration.JushuitanSettingReadService;
import cn.shopex.ecshopx.distribution.integration.LocalDeliveryShopCreateClient;
import cn.shopex.ecshopx.distribution.integration.WdtErpShopQueryClient;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.distribution.repository.PickupLocationRelDistributorRepository;
import cn.shopex.ecshopx.common.dispatch.CreateDistributorJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributionAddEventDispatchPublisher;
import cn.shopex.ecshopx.distribution.support.CompanyMenuTypeProductModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 分销商/门店创建编排。旧版 {@code POST …/v1/shops}（{@code DistributorShopController#createShops}）与
 * {@link cn.shopex.ecshopx.distribution.api.admin.v1.DistributorController#createDistributor(java.servlet.http.HttpServletRequest, java.util.Map)} 共用本类及
 * {@code afterCommit} 内的 Job 与 DistributionAdd 发布顺序。
 */
@Service
public class DistributorCreateOrchestrator {

	private final HfpayLedgerConfigReadService hfpayLedgerConfigReadService;
	private final LocalDeliveryShopCreateClient localDeliveryShopCreateClient;
	private final WdtErpShopQueryClient wdtErpShopQueryClient;
	private final JushuitanSettingReadService jushuitanSettingReadService;
	private final DistributorCreateService distributorCreateService;
	private final DistributorWriteRepository distributorWriteRepository;
	private final PickupLocationRelDistributorRepository pickupLocationRelDistributorRepository;
	private final DistributorAftersalesAddressWriteService distributorAftersalesAddressWriteService;
	private final CreateDistributorJobDispatchPublisher createDistributorJobDispatchPublisher;
	private final DistributionAddEventDispatchPublisher distributionAddEventDispatchPublisher;
	private final CompanysMapper companysMapper;

	public DistributorCreateOrchestrator(
			HfpayLedgerConfigReadService hfpayLedgerConfigReadService,
			LocalDeliveryShopCreateClient localDeliveryShopCreateClient,
			WdtErpShopQueryClient wdtErpShopQueryClient,
			JushuitanSettingReadService jushuitanSettingReadService,
			DistributorCreateService distributorCreateService,
			DistributorWriteRepository distributorWriteRepository,
			PickupLocationRelDistributorRepository pickupLocationRelDistributorRepository,
			DistributorAftersalesAddressWriteService distributorAftersalesAddressWriteService,
			CreateDistributorJobDispatchPublisher createDistributorJobDispatchPublisher,
			DistributionAddEventDispatchPublisher distributionAddEventDispatchPublisher,
			CompanysMapper companysMapper) {
		this.hfpayLedgerConfigReadService = hfpayLedgerConfigReadService;
		this.localDeliveryShopCreateClient = localDeliveryShopCreateClient;
		this.wdtErpShopQueryClient = wdtErpShopQueryClient;
		this.jushuitanSettingReadService = jushuitanSettingReadService;
		this.distributorCreateService = distributorCreateService;
		this.distributorWriteRepository = distributorWriteRepository;
		this.pickupLocationRelDistributorRepository = pickupLocationRelDistributorRepository;
		this.distributorAftersalesAddressWriteService = distributorAftersalesAddressWriteService;
		this.createDistributorJobDispatchPublisher = createDistributorJobDispatchPublisher;
		this.distributionAddEventDispatchPublisher = distributionAddEventDispatchPublisher;
		this.companysMapper = companysMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> create(Map<String, Object> merged, Map<String, Object> user, String requestLangTag) {
		long companyId = toLong(merged.get("company_id"));
		String name = str(merged.get("name"));
		if (distributorWriteRepository.countByCompanyAndNameNotDeleted(companyId, name) > 0) {
			throw new ResourceException("店铺名称已存在");
		}
		String sc0 = str(merged.get("shop_code"));
		if (StringUtils.hasText(sc0) && distributorWriteRepository.countByCompanyAndShopCodeNotDeleted(companyId, sc0) > 0) {
			throw new ResourceException("店铺编号已存在");
		}
		hfpayLedgerConfigReadService.assertOpenAndRateAllowedIfNeeded(companyId, merged);
		localDeliveryShopCreateClient.applyCreateShopIfDada(companyId, merged);
		String sc1 = str(merged.get("shop_code"));
		if (StringUtils.hasText(sc1) && distributorWriteRepository.countByCompanyAndShopCodeNotDeleted(companyId, sc1) > 0) {
			throw new ResourceException("店铺编号已存在");
		}
		wdtErpShopQueryClient.assertShopExistsIfWdtNo(companyId, merged);
		jushuitanSettingReadService.assertEnabledIfJstIdPositive(companyId, merged);

		Map<String, Object> row = distributorCreateService.performInsertAndEvents(merged, user, requestLangTag);
		long distributorId = toLong(row.get("distributor_id"));

		for (Long pickupId : pickupLocationIds(merged.get("pickup_location"))) {
			pickupLocationRelDistributorRepository.relDistributor(companyId, pickupId, distributorId);
		}

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
			distributorAftersalesAddressWriteService.setAfterSalesAddressForNewDistributor(
					companyId, distributorId, merchantId, addr, productModelSlug);
		}

		Map<String, Object> rowSnapshot = new LinkedHashMap<>(row);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				createDistributorJobDispatchPublisher.enqueueAfterDistributorCreate(rowSnapshot);
				distributionAddEventDispatchPublisher.publish(rowSnapshot);
			}
		});
		return row;
	}

	private String resolveProductModelSlug(long companyId) {
		Companys c = companysMapper.selectById(companyId);
		if (c == null || c.getMenuType() == null) {
			return "platform";
		}
		return CompanyMenuTypeProductModel.toSlug(c.getMenuType());
	}

	private static List<Long> pickupLocationIds(Object v) {
		List<Long> out = new ArrayList<>();
		if (v == null) {
			return out;
		}
		if (v instanceof List<?> list) {
			for (Object o : list) {
				if (o instanceof Number n) {
					out.add(n.longValue());
				} else if (o != null && StringUtils.hasText(o.toString())) {
					out.add(Long.parseLong(o.toString().trim()));
				}
			}
			return out;
		}
		if (v instanceof Number n) {
			out.add(n.longValue());
			return out;
		}
		if (StringUtils.hasText(v.toString())) {
			out.add(Long.parseLong(v.toString().trim()));
		}
		return out;
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
