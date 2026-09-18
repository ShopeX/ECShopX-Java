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

package cn.shopex.ecshopx.distribution.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.dispatch.CreateDistributorJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributionAddEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributorCreateEventDispatchPublisher;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.integration.LocalDeliveryShopCreateClient;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.openapi.OpenapiDistributorOpenApiRowFormatSupport;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.distribution.service.DistributorFirstLetterUtil;
import cn.shopex.ecshopx.distribution.service.DistributorMultiLangWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OpenapiThirdApiV2DistributorCreateService {

	private final DistributorMapper distributorMapper;
	private final DistributorWriteRepository distributorWriteRepository;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorMultiLangWriteService distributorMultiLangWriteService;
	private final LocalDeliveryShopCreateClient localDeliveryShopCreateClient;
	private final CreateDistributorJobDispatchPublisher createDistributorJobDispatchPublisher;
	private final DistributionAddEventDispatchPublisher distributionAddEventDispatchPublisher;
	private final DistributorCreateEventDispatchPublisher distributorCreateEventDispatchPublisher;
	private final LangueProperties langueProperties;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;

	public OpenapiThirdApiV2DistributorCreateService(
			DistributorMapper distributorMapper,
			DistributorWriteRepository distributorWriteRepository,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorMultiLangWriteService distributorMultiLangWriteService,
			LocalDeliveryShopCreateClient localDeliveryShopCreateClient,
			CreateDistributorJobDispatchPublisher createDistributorJobDispatchPublisher,
			DistributionAddEventDispatchPublisher distributionAddEventDispatchPublisher,
			DistributorCreateEventDispatchPublisher distributorCreateEventDispatchPublisher,
			LangueProperties langueProperties,
			ObjectMapper objectMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.distributorMapper = distributorMapper;
		this.distributorWriteRepository = distributorWriteRepository;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorMultiLangWriteService = distributorMultiLangWriteService;
		this.localDeliveryShopCreateClient = localDeliveryShopCreateClient;
		this.createDistributorJobDispatchPublisher = createDistributorJobDispatchPublisher;
		this.distributionAddEventDispatchPublisher = distributionAddEventDispatchPublisher;
		this.distributorCreateEventDispatchPublisher = distributorCreateEventDispatchPublisher;
		this.langueProperties = langueProperties;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public Map<String, Object> executeOpenapiCreate(
			long companyId,
			String shopCodeRaw,
			String distributorNameRaw,
			String contactUsernameRaw,
			String contactMobileRaw,
			String hourRaw,
			String isZitiRaw,
			String isDeliveryRaw,
			String isAutoSyncGoodsRaw,
			String isDadaRaw,
			String isDefaultRaw,
			String logoRaw) {
		validateParams(
				shopCodeRaw,
				distributorNameRaw,
				contactUsernameRaw,
				contactMobileRaw,
				hourRaw,
				isZitiRaw,
				isDeliveryRaw,
				isAutoSyncGoodsRaw,
				isDadaRaw,
				isDefaultRaw,
				logoRaw);

		String shopCode = shopCodeRaw;
		String distributorName = distributorNameRaw;
		String contactUsername = contactUsernameRaw;
		String contactMobilePlain = contactMobileRaw;
		String hour = hourRaw == null ? "" : hourRaw;
		String logo = logoRaw == null ? "" : logoRaw;

		boolean isZiti = parseBool01OrDefault(isZitiRaw, false);
		boolean isDelivery = parseBool01OrDefault(isDeliveryRaw, true);
		boolean isAutoSyncGoods = parseBool01OrDefault(isAutoSyncGoodsRaw, false);
		boolean isDada = parseBool01OrDefault(isDadaRaw, false);
		boolean isDefault = parseBool01OrDefault(isDefaultRaw, false);

		if (distributorWriteRepository.existsOtherWithShopCode(companyId, shopCode, null)) {
			throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_EXIST, "店铺号已存在");
		}
		if (distributorWriteRepository.countByCompanyAndNameNotDeleted(companyId, distributorName) > 0) {
			throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_EXIST, "店铺名称已存在");
		}
		String mobileEnc = sensitiveFieldEncryptor.encrypt(contactMobilePlain);
		if (distributorWriteRepository.existsOtherWithMobile(companyId, mobileEnc, null)) {
			throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_EXIST, "店铺手机号已存在");
		}

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("shop_code", shopCode);
		merged.put("name", distributorName);
		merged.put("contact", contactUsername);
		merged.put("mobile", contactMobilePlain);
		merged.put("hour", hour);
		merged.put("logo", logo);
		merged.put("auto_sync_goods", isAutoSyncGoods);
		merged.put("is_ziti", isZiti);
		merged.put("is_delivery", isDelivery);
		merged.put("is_dada", isDada);
		merged.put("is_default", isDefault);
		merged.put("is_distributor", true);
		merged.put("is_valid", "false");
		merged.put("review_status", true);
		merged.put("source_from", 3);
		merged.put("contract_phone", "0");
		merged.put("banner", "");
		merged.put("province", "");
		merged.put("city", "");
		merged.put("area", "");
		merged.put("address", "");
		merged.put("lng", "");
		merged.put("lat", "");
		merged.put("regions_id", null);
		merged.put("regions", null);
		merged.put("dada_shop_create", 0);
		merged.put("shansong_shop_create", 0);

		localDeliveryShopCreateClient.applyOpenapiCreateShopIfDada(companyId, merged);

		String finalShopCode = str(merged.get("shop_code"));
		if (!finalShopCode.equals(shopCode)
				&& distributorWriteRepository.existsOtherWithShopCode(companyId, finalShopCode, null)) {
			throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_EXIST, "店铺号已存在");
		}

		String contactEnc = sensitiveFieldEncryptor.encrypt(contactUsername);
		String mobileStored = sensitiveFieldEncryptor.encrypt(contactMobilePlain);
		String requestLang = langueProperties.getDefaultLang();

		Map<String, Object>[] internalHolder = new Map[1];
		transactionTemplate.executeWithoutResult(status -> {
			Distributor e = new Distributor();
			e.setCompanyId(companyId);
			e.setShopCode(str(merged.get("shop_code")));
			e.setName(distributorName);
			e.setFirstLetter(DistributorFirstLetterUtil.firstLetter(distributorName));
			e.setContact(contactEnc);
			e.setMobile(mobileStored);
			e.setHour(hour);
			e.setLogo(logo);
			e.setAutoSyncGoods(isAutoSyncGoods);
			e.setIsZiti(isZiti);
			e.setIsDelivery(isDelivery);
			e.setIsDada(isDada);
			e.setIsDefault(isDefault ? 1 : 0);
			e.setIsDistributor(true);
			e.setIsValid("false");
			e.setReviewStatus(true);
			e.setSourceFrom(3);
			e.setContractPhone("0");
			e.setBanner("");
			e.setProvince("");
			e.setCity("");
			e.setArea("");
			e.setAddress("");
			e.setLng("");
			e.setLat("");
			e.setRegionsId(null);
			e.setRegions(null);
			e.setDadaShopCreate(Boolean.TRUE.equals(merged.get("dada_shop_create")));
			e.setShansongShopCreate(Boolean.TRUE.equals(merged.get("shansong_shop_create")));
			if (merged.get("shansong_store_id") instanceof Number n) {
				e.setShansongStoreId(n.longValue());
			}
			long now = System.currentTimeMillis() / 1000L;
			e.setCreated(now);
			e.setUpdated(now);

			distributorMapper.insert(e);
			distributorMultiLangWriteService.applyAfterInsert(
					e.getDistributorId(), companyId, merged, requestLang);

			Map<String, Object> internal = OpenapiDistributorOpenApiRowFormatSupport.toInternalRowMap(
					e, sensitiveFieldEncryptor, objectMapper);
			internalHolder[0] = internal;
			Map<String, Object> rowSnapshot = new LinkedHashMap<>(internal);

			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					createDistributorJobDispatchPublisher.enqueueAfterDistributorCreate(rowSnapshot);
					distributionAddEventDispatchPublisher.publish(rowSnapshot);
					distributorCreateEventDispatchPublisher.publish(rowSnapshot);
				}
			});
		});

		return OpenapiDistributorOpenApiRowFormatSupport.toOpenapiRow(internalHolder[0]);
	}

	private void validateParams(
			String shopCodeRaw,
			String distributorNameRaw,
			String contactUsernameRaw,
			String contactMobileRaw,
			String hourRaw,
			String isZitiRaw,
			String isDeliveryRaw,
			String isAutoSyncGoodsRaw,
			String isDadaRaw,
			String isDefaultRaw,
			String logoRaw) {
		if (shopCodeRaw == null || shopCodeRaw.isEmpty()) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺号参数错误");
		}
		if (!shopCodeRaw.matches("^[A-Za-z0-9-]+$")) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺号参数错误");
		}
		if (distributorNameRaw == null || distributorNameRaw.isEmpty()) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺名称参数错误");
		}
		if (contactUsernameRaw == null || contactUsernameRaw.isEmpty()) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "联系人姓名参数错误");
		}
		if (contactMobileRaw == null || contactMobileRaw.isEmpty()) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "联系方式参数错误");
		}
		if (!contactMobileRaw.matches("^1[3456789]\\d{9}$")) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "联系方式参数错误");
		}
		if (hourRaw != null && !isValidOptionalHour(hourRaw)) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "经营时间（开始-结束）参数错误");
		}
		validateOptionalInt01(isZitiRaw, "是否支持自提参数错误");
		validateOptionalInt01(isDeliveryRaw, "是否支持快递参数错误");
		validateOptionalInt01(isAutoSyncGoodsRaw, "是否自动同步总部商品参数错误");
		validateOptionalInt01(isDadaRaw, "是否开启同城配参数错误");
		validateOptionalInt01(isDefaultRaw, "是否默认店铺参数错误");
		if (logoRaw != null && !isAcceptableLogo(logoRaw)) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺Logo图片Url参数错误");
		}
	}

	private static boolean isValidOptionalHour(String hourRaw) {
		return true;
	}

	private static boolean isAcceptableLogo(String logoRaw) {
		return true;
	}

	private static void validateOptionalInt01(String raw, String message) {
		if (raw == null) {
			return;
		}
		if (raw.isEmpty()) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
		}
		try {
			int v = Integer.parseInt(raw.trim());
			if (v != 0 && v != 1) {
				throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
			}
		} catch (NumberFormatException e) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
		}
	}

	private static boolean parseBool01OrDefault(String raw, boolean defaultWhenNull) {
		if (raw == null) {
			return defaultWhenNull;
		}
		return Integer.parseInt(raw.trim()) == 1;
	}

	private static OpenapiDistributorV2FailException v2Fail(String code, String message) {
		return new OpenapiDistributorV2FailException(code, message);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
