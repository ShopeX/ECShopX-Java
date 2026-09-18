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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.dispatch.DistributionEditEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributorUpdateEventDispatchPublisher;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.openapi.OpenapiDistributorOpenApiRowFormatSupport;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.distribution.service.DistributorFirstLetterUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OpenapiThirdApiV2DistributorUpdateService {

	private final DistributorMapper distributorMapper;
	private final DistributorWriteRepository distributorWriteRepository;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributionEditEventDispatchPublisher distributionEditEventDispatchPublisher;
	private final DistributorUpdateEventDispatchPublisher distributorUpdateEventDispatchPublisher;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;

	public OpenapiThirdApiV2DistributorUpdateService(
			DistributorMapper distributorMapper,
			DistributorWriteRepository distributorWriteRepository,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributionEditEventDispatchPublisher distributionEditEventDispatchPublisher,
			DistributorUpdateEventDispatchPublisher distributorUpdateEventDispatchPublisher,
			ObjectMapper objectMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.distributorMapper = distributorMapper;
		this.distributorWriteRepository = distributorWriteRepository;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributionEditEventDispatchPublisher = distributionEditEventDispatchPublisher;
		this.distributorUpdateEventDispatchPublisher = distributorUpdateEventDispatchPublisher;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public Map<String, Object> executeOpenapiUpdate(
			long companyId,
			String shopCodeRaw,
			Optional<String> distributorNamePresent,
			Optional<String> contactUsernamePresent,
			Optional<String> contactMobilePresent,
			Optional<String> hourPresent,
			Optional<String> isZitiPresent,
			Optional<String> isDeliveryPresent,
			Optional<String> isAutoSyncGoodsPresent,
			Optional<String> isDadaPresent,
			Optional<String> isDefaultPresent,
			Optional<String> logoPresent,
			Optional<String> statusPresent,
			Optional<String> provincePresent,
			Optional<String> cityPresent,
			Optional<String> areaPresent,
			Optional<String> addressPresent,
			Optional<String> lngPresent,
			Optional<String> latPresent) {
		validateParams(
				shopCodeRaw,
				contactMobilePresent,
				isZitiPresent,
				isDeliveryPresent,
				isAutoSyncGoodsPresent,
				isDadaPresent,
				isDefaultPresent,
				statusPresent,
				lngPresent,
				latPresent);

		if (shopCodeRaw == null || shopCodeRaw.isEmpty()) {
			throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_NOT_FOUND, "店铺找不到");
		}

		LambdaQueryWrapper<Distributor> locateWrapper = new LambdaQueryWrapper<>();
		locateWrapper.eq(Distributor::getCompanyId, companyId).eq(Distributor::getShopCode, shopCodeRaw);
		Distributor existing = distributorMapper.selectOne(locateWrapper);
		if (existing == null) {
			throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_NOT_FOUND, "店铺找不到");
		}
		long distributorId = existing.getDistributorId();

		PartialUpdateParams params = buildPartialUpdateParams(
				companyId,
				distributorId,
				shopCodeRaw,
				distributorNamePresent,
				contactUsernamePresent,
				contactMobilePresent,
				hourPresent,
				isZitiPresent,
				isDeliveryPresent,
				isAutoSyncGoodsPresent,
				isDadaPresent,
				isDefaultPresent,
				logoPresent,
				statusPresent,
				provincePresent,
				cityPresent,
				areaPresent,
				addressPresent,
				lngPresent,
				latPresent);

		if (params.isEmpty()) {
			Map<String, Object> internal = OpenapiDistributorOpenApiRowFormatSupport.toInternalRowMap(
					existing, sensitiveFieldEncryptor, objectMapper);
			return OpenapiDistributorOpenApiRowFormatSupport.toOpenapiRow(internal);
		}

		Distributor entity = existing;
		applyPartialToEntity(entity, params);
		entity.setUpdated(System.currentTimeMillis() / 1000L);

		Map<String, Object>[] internalHolder = new Map[1];
		transactionTemplate.executeWithoutResult(status -> {
			int updated = distributorMapper.updateById(entity);
			if (updated == 0) {
				throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_NOT_FOUND, "店铺找不到");
			}
			Map<String, Object> internal = OpenapiDistributorOpenApiRowFormatSupport.toInternalRowMap(
					entity, sensitiveFieldEncryptor, objectMapper);
			internalHolder[0] = internal;
			Map<String, Object> rowSnapshot = new LinkedHashMap<>(internal);

			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					distributionEditEventDispatchPublisher.publish(rowSnapshot);
					distributorUpdateEventDispatchPublisher.publish(rowSnapshot);
				}
			});
		});

		return OpenapiDistributorOpenApiRowFormatSupport.toOpenapiRow(internalHolder[0]);
	}

	private void validateParams(
			String shopCodeRaw,
			Optional<String> contactMobilePresent,
			Optional<String> isZitiPresent,
			Optional<String> isDeliveryPresent,
			Optional<String> isAutoSyncGoodsPresent,
			Optional<String> isDadaPresent,
			Optional<String> isDefaultPresent,
			Optional<String> statusPresent,
			Optional<String> lngPresent,
			Optional<String> latPresent) {
		if (shopCodeRaw != null && !shopCodeRaw.matches("^[A-Za-z0-9-]+$")) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺号参数错误");
		}
		if (contactMobilePresent.isPresent()) {
			String raw = contactMobilePresent.get();
			if (raw != null && !raw.isEmpty() && !raw.matches("^1[3456789]\\d{9}$")) {
				throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "联系方式参数错误");
			}
		}
		validatePresentInt01(isZitiPresent, "是否支持自提参数错误");
		validatePresentInt01(isDeliveryPresent, "是否支持快递参数错误");
		validatePresentInt01(isAutoSyncGoodsPresent, "是否自动同步总部商品参数错误");
		validatePresentInt01(isDadaPresent, "是否开启同城配参数错误");
		validatePresentInt01(isDefaultPresent, "是否默认店铺参数错误");
		if (statusPresent.isPresent()) {
			String raw = statusPresent.get();
			if (raw != null) {
				if (raw.isEmpty()) {
					throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺状态参数错误");
				}
				validateStatusInt012(raw);
			}
		}
		if (lngPresent.isPresent()) {
			String raw = lngPresent.get();
			if (raw != null && !raw.isEmpty() && !isNumeric(raw)) {
				throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "经度参数错误");
			}
		}
		if (latPresent.isPresent()) {
			String raw = latPresent.get();
			if (raw != null && !raw.isEmpty() && !isNumeric(raw)) {
				throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "纬度参数错误");
			}
		}
	}

	private static void validatePresentInt01(Optional<String> present, String message) {
		if (present.isEmpty()) {
			return;
		}
		String raw = present.get();
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

	private static void validateStatusInt012(String raw) {
		try {
			int v = Integer.parseInt(raw.trim());
			if (v != 0 && v != 1 && v != 2) {
				throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺状态参数错误");
			}
		} catch (NumberFormatException e) {
			throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺状态参数错误");
		}
	}

	private static boolean isNumeric(String raw) {
		try {
			Double.parseDouble(raw.trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private PartialUpdateParams buildPartialUpdateParams(
			long companyId,
			long distributorId,
			String shopCodeRaw,
			Optional<String> distributorNamePresent,
			Optional<String> contactUsernamePresent,
			Optional<String> contactMobilePresent,
			Optional<String> hourPresent,
			Optional<String> isZitiPresent,
			Optional<String> isDeliveryPresent,
			Optional<String> isAutoSyncGoodsPresent,
			Optional<String> isDadaPresent,
			Optional<String> isDefaultPresent,
			Optional<String> logoPresent,
			Optional<String> statusPresent,
			Optional<String> provincePresent,
			Optional<String> cityPresent,
			Optional<String> areaPresent,
			Optional<String> addressPresent,
			Optional<String> lngPresent,
			Optional<String> latPresent) {
		PartialUpdateParams out = new PartialUpdateParams();

		if (shopCodeRaw != null) {
			out.shopCode = shopCodeRaw;
			if (distributorWriteRepository.existsOtherWithShopCode(companyId, shopCodeRaw, distributorId)) {
				throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_EXIST, "店铺号已存在");
			}
		}
		if (presentNonNull(distributorNamePresent)) {
			out.name = distributorNamePresent.get();
			throwIfNameConflict(companyId, out.name, distributorId);
		}
		if (presentNonNull(contactMobilePresent)) {
			out.mobilePlain = contactMobilePresent.get();
			String mobileEnc = sensitiveFieldEncryptor.encrypt(out.mobilePlain);
			if (distributorWriteRepository.existsOtherWithMobile(companyId, mobileEnc, distributorId)) {
				throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_EXIST, "店铺手机号已存在");
			}
		}
		applyPresentString(out, hourPresent, out::setHour);
		applyPresentString(out, contactUsernamePresent, out::setContact);
		applyPresentString(out, logoPresent, out::setLogo);
		applyPresentString(out, provincePresent, out::setProvince);
		applyPresentString(out, cityPresent, out::setCity);
		applyPresentString(out, areaPresent, out::setArea);
		applyPresentString(out, addressPresent, out::setAddress);
		applyPresentString(out, lngPresent, out::setLng);
		applyPresentString(out, latPresent, out::setLat);

		applyPresentBool01(out, isZitiPresent, out::setIsZiti);
		applyPresentBool01(out, isDeliveryPresent, out::setIsDelivery);
		applyPresentBool01(out, isAutoSyncGoodsPresent, out::setAutoSyncGoods);
		applyPresentBool01(out, isDadaPresent, out::setIsDada);
		applyPresentBool01(out, isDefaultPresent, out::setIsDefaultInt);

		if (statusPresent.isPresent() && statusPresent.get() != null && !statusPresent.get().isEmpty()) {
			int status = Integer.parseInt(statusPresent.get().trim());
			out.isValid = mapStatusToIsValid(status);
		}
		return out;
	}

	private void throwIfNameConflict(long companyId, String name, long distributorId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getName, name)
				.ne(Distributor::getDistributorId, distributorId)
				.ne(Distributor::getIsValid, "delete");
		if (distributorMapper.selectCount(w) > 0) {
			throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_EXIST, "店铺名称已存在");
		}
	}

	private void applyPartialToEntity(Distributor entity, PartialUpdateParams params) {
		if (params.shopCode != null) {
			entity.setShopCode(params.shopCode);
		}
		if (params.name != null) {
			entity.setName(params.name);
			entity.setFirstLetter(DistributorFirstLetterUtil.firstLetter(params.name));
		}
		if (params.contact != null) {
			entity.setContact(sensitiveFieldEncryptor.encrypt(params.contact));
		}
		if (params.mobilePlain != null) {
			entity.setMobile(sensitiveFieldEncryptor.encrypt(params.mobilePlain));
		}
		if (params.hour != null) {
			entity.setHour(params.hour);
		}
		if (params.logo != null) {
			entity.setLogo(params.logo);
		}
		if (params.province != null) {
			entity.setProvince(params.province);
		}
		if (params.city != null) {
			entity.setCity(params.city);
		}
		if (params.area != null) {
			entity.setArea(params.area);
		}
		if (params.address != null) {
			entity.setAddress(params.address);
		}
		if (params.lng != null) {
			entity.setLng(params.lng);
		}
		if (params.lat != null) {
			entity.setLat(params.lat);
		}
		if (params.isZiti != null) {
			entity.setIsZiti(params.isZiti);
		}
		if (params.isDelivery != null) {
			entity.setIsDelivery(params.isDelivery);
		}
		if (params.autoSyncGoods != null) {
			entity.setAutoSyncGoods(params.autoSyncGoods);
		}
		if (params.isDada != null) {
			entity.setIsDada(params.isDada);
		}
		if (params.isDefaultInt != null) {
			entity.setIsDefault(params.isDefaultInt);
		}
		if (params.isValid != null) {
			entity.setIsValid(params.isValid);
		}
	}

	private static void applyPresentString(
			PartialUpdateParams out, Optional<String> present, Consumer<String> setter) {
		if (presentNonNull(present)) {
			setter.accept(present.get());
		}
	}

	private static void applyPresentBool01(
			PartialUpdateParams out, Optional<String> present, Consumer<Boolean> setter) {
		if (present.isPresent() && present.get() != null) {
			setter.accept(parseBool01(present.get()));
		}
	}

	private static boolean presentNonNull(Optional<String> o) {
		return o.isPresent() && o.get() != null;
	}

	private static String mapStatusToIsValid(int status) {
		return switch (status) {
			case 0 -> "delete";
			case 1 -> "true";
			case 2 -> "false";
			default -> throw v2Fail(OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺状态参数错误");
		};
	}

	private static boolean parseBool01(String raw) {
		return Integer.parseInt(raw.trim()) == 1;
	}

	private static OpenapiDistributorV2FailException v2Fail(String code, String message) {
		return new OpenapiDistributorV2FailException(code, message);
	}

	private static final class PartialUpdateParams {
		private String shopCode;
		private String name;
		private String contact;
		private String mobilePlain;
		private String hour;
		private String logo;
		private String province;
		private String city;
		private String area;
		private String address;
		private String lng;
		private String lat;
		private Boolean isZiti;
		private Boolean isDelivery;
		private Boolean autoSyncGoods;
		private Boolean isDada;
		private Integer isDefaultInt;
		private String isValid;

		boolean isEmpty() {
			return shopCode == null
					&& name == null
					&& contact == null
					&& mobilePlain == null
					&& hour == null
					&& logo == null
					&& province == null
					&& city == null
					&& area == null
					&& address == null
					&& lng == null
					&& lat == null
					&& isZiti == null
					&& isDelivery == null
					&& autoSyncGoods == null
					&& isDada == null
					&& isDefaultInt == null
					&& isValid == null;
		}

		void setHour(String hour) {
			this.hour = hour;
		}

		void setContact(String contact) {
			this.contact = contact;
		}

		void setLogo(String logo) {
			this.logo = logo;
		}

		void setProvince(String province) {
			this.province = province;
		}

		void setCity(String city) {
			this.city = city;
		}

		void setArea(String area) {
			this.area = area;
		}

		void setAddress(String address) {
			this.address = address;
		}

		void setLng(String lng) {
			this.lng = lng;
		}

		void setLat(String lat) {
			this.lat = lat;
		}

		void setIsZiti(Boolean isZiti) {
			this.isZiti = isZiti;
		}

		void setIsDelivery(Boolean isDelivery) {
			this.isDelivery = isDelivery;
		}

		void setAutoSyncGoods(Boolean autoSyncGoods) {
			this.autoSyncGoods = autoSyncGoods;
		}

		void setIsDada(Boolean isDada) {
			this.isDada = isDada;
		}

		void setIsDefaultInt(Boolean isDefault) {
			this.isDefaultInt = isDefault ? 1 : 0;
		}
	}
}
