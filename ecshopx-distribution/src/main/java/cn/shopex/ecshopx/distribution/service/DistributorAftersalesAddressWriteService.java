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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.distribution.CompanyMapGeocodePort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.DistributorAftersalesAddress;
import cn.shopex.ecshopx.distribution.mapper.DistributorAftersalesAddressMapper;
import cn.shopex.ecshopx.distribution.service.dto.DistributorAftersalesAddressPutUpdateInput;
import cn.shopex.ecshopx.distribution.service.dto.DistributorAftersalesAddressSetLogisticsInput;
import cn.shopex.ecshopx.distribution.service.multilang.DistributorAftersalesAddressOutsideMultiLangWriteService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DistributorAftersalesAddressWriteService {

	private final DistributorAftersalesAddressMapper distributorAftersalesAddressMapper;
	private final CompanyMapGeocodePort companyMapGeocodePort;
	private final ObjectMapper objectMapper;
	private final ShopMenuService shopMenuService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorAftersalesAddressOutsideMultiLangWriteService aftersalesAddressOutsideMultiLangWriteService;

	public DistributorAftersalesAddressWriteService(
			DistributorAftersalesAddressMapper distributorAftersalesAddressMapper,
			CompanyMapGeocodePort companyMapGeocodePort,
			ObjectMapper objectMapper,
			ShopMenuService shopMenuService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorAftersalesAddressOutsideMultiLangWriteService aftersalesAddressOutsideMultiLangWriteService) {
		this.distributorAftersalesAddressMapper = distributorAftersalesAddressMapper;
		this.companyMapGeocodePort = companyMapGeocodePort;
		this.objectMapper = objectMapper;
		this.shopMenuService = shopMenuService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.aftersalesAddressOutsideMultiLangWriteService = aftersalesAddressOutsideMultiLangWriteService;
	}

	@SuppressWarnings("unchecked")
	public void setAfterSalesAddressForNewDistributor(
			long companyId,
			long distributorId,
			long merchantId,
			Map<String, Object> offlineAftersalesAddress,
			String productModelSlug) {
		String name = str(offlineAftersalesAddress.get("name"));
		String address = str(offlineAftersalesAddress.get("address"));
		String mobile = str(offlineAftersalesAddress.get("mobile"));
		if (!StringUtils.hasText(name) || !StringUtils.hasText(address) || !StringUtils.hasText(mobile)) {
			return;
		}
		String city = "";
		String province = "";
		String area = "";
		Object regionsObj = offlineAftersalesAddress.get("regions");
		if (regionsObj instanceof List<?> list && list.size() >= 1) {
			province = str(list.get(0));
		}
		if (regionsObj instanceof List<?> list && list.size() >= 2) {
			city = str(list.get(1));
		}
		if (regionsObj instanceof List<?> list && list.size() >= 3) {
			area = str(list.get(2));
		}
		Object regionsIdObj = offlineAftersalesAddress.get("regions_id");
		String regionsIdJson;
		try {
			regionsIdJson = objectMapper.writeValueAsString(regionsIdObj == null ? List.of() : regionsIdObj);
		} catch (Exception e) {
			regionsIdJson = "[]";
		}
		String regionsJson;
		try {
			regionsJson = objectMapper.writeValueAsString(regionsObj == null ? List.of() : regionsObj);
		} catch (Exception e) {
			regionsJson = "[]";
		}
		String lng = "";
		String lat = "";
		if ("standard".equals(productModelSlug) || "platform".equals(productModelSlug)) {
			CompanyMapGeocodePort.GeocodeLatLng coords = companyMapGeocodePort.geocode(companyId, city, address);
			lng = coords.lng();
			lat = coords.lat();
		}
		DistributorAftersalesAddress entity = new DistributorAftersalesAddress();
		entity.setDistributorId(distributorId);
		entity.setCompanyId(companyId);
		entity.setMerchantId(merchantId);
		entity.setName(name);
		entity.setProvince(province);
		entity.setCity(city);
		entity.setArea(area);
		entity.setRegionsId(regionsIdJson);
		entity.setRegions(regionsJson);
		entity.setAddress(address);
		entity.setLng(lng);
		entity.setLat(lat);
		entity.setMobile(mobile);
		entity.setHours(str(offlineAftersalesAddress.get("hours")));
		entity.setReturnType("offline");
		entity.setSupplierId(0);
		entity.setContact("");
		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreated((long) now);
		entity.setUpdated((long) now);
		distributorAftersalesAddressMapper.insert(entity);
	}

	/**
	 * Updates an existing offline return address row only (no geocoding).
	 */
	public void updateOfflineAfterSalesAddress(long companyId, long distributorId, Map<String, Object> offlineAftersalesAddress) {
		String city = "";
		String province = "";
		String area = "";
		Object regionsObj = offlineAftersalesAddress.get("regions");
		if (regionsObj instanceof List<?> list && list.size() >= 1) {
			province = str(list.get(0));
		}
		if (regionsObj instanceof List<?> list && list.size() >= 2) {
			city = str(list.get(1));
		}
		if (regionsObj instanceof List<?> list && list.size() >= 3) {
			area = str(list.get(2));
		}
		Object regionsIdObj = offlineAftersalesAddress.get("regions_id");
		String regionsIdJson;
		String regionsJson;
		try {
			regionsIdJson = objectMapper.writeValueAsString(regionsIdObj == null ? List.of() : regionsIdObj);
			regionsJson = objectMapper.writeValueAsString(regionsObj == null ? List.of() : regionsObj);
		} catch (Exception e) {
			regionsIdJson = "[]";
			regionsJson = "[]";
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<DistributorAftersalesAddress> u = new LambdaUpdateWrapper<>();
		u.eq(DistributorAftersalesAddress::getCompanyId, companyId)
				.eq(DistributorAftersalesAddress::getDistributorId, distributorId)
				.eq(DistributorAftersalesAddress::getReturnType, "offline")
				.set(DistributorAftersalesAddress::getName, str(offlineAftersalesAddress.get("name")))
				.set(DistributorAftersalesAddress::getProvince, province)
				.set(DistributorAftersalesAddress::getCity, city)
				.set(DistributorAftersalesAddress::getArea, area)
				.set(DistributorAftersalesAddress::getRegionsId, regionsIdJson)
				.set(DistributorAftersalesAddress::getRegions, regionsJson)
				.set(DistributorAftersalesAddress::getAddress, str(offlineAftersalesAddress.get("address")))
				.set(DistributorAftersalesAddress::getMobile, str(offlineAftersalesAddress.get("mobile")))
				.set(DistributorAftersalesAddress::getHours, str(offlineAftersalesAddress.get("hours")))
				.set(DistributorAftersalesAddress::getUpdated, (long) now);
		distributorAftersalesAddressMapper.update(null, u);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> setDefaultAddress(long addressId, long companyId) {
		DistributorAftersalesAddress row = distributorAftersalesAddressMapper.selectById(addressId);
		if (row == null) {
			throw new ResourceException("地址不存在");
		}
		long distributorId = row.getDistributorId();
		LambdaUpdateWrapper<DistributorAftersalesAddress> clear = new LambdaUpdateWrapper<>();
		clear.eq(DistributorAftersalesAddress::getDistributorId, distributorId)
				.eq(DistributorAftersalesAddress::getCompanyId, companyId)
				.set(DistributorAftersalesAddress::getIsDefault, 2);
		distributorAftersalesAddressMapper.update(null, clear);
		row.setIsDefault(1);
		row.setUpdated(System.currentTimeMillis() / 1000L);
		distributorAftersalesAddressMapper.updateById(row);
		return Map.of("status", true);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> deleteDistributorAfterSalesAddress(long companyId, long addressId) {
		LambdaQueryWrapper<DistributorAftersalesAddress> w = new LambdaQueryWrapper<>();
		w.eq(DistributorAftersalesAddress::getCompanyId, companyId)
				.eq(DistributorAftersalesAddress::getAddressId, addressId);
		distributorAftersalesAddressMapper.delete(w);
		return Map.of("status", true);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateDistributorAfterSalesAddress(
			long companyId,
			long addressId,
			DistributorAftersalesAddressPutUpdateInput input,
			String requestLangTag) {
		LambdaQueryWrapper<DistributorAftersalesAddress> w = new LambdaQueryWrapper<>();
		w.eq(DistributorAftersalesAddress::getCompanyId, companyId)
				.eq(DistributorAftersalesAddress::getAddressId, addressId);
		DistributorAftersalesAddress entity = distributorAftersalesAddressMapper.selectOne(w);
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}
		long distId = input.distributorId();
		// Default address row is 1; new non-default insert downgrades other rows to 0.
		// set to 2 only when another row already has is_default=1 for this company+distributor.
		boolean has = hasOtherDefaultAddressForDistributor(companyId, distId, entity.getAddressId());
		int newDefault = has ? 2 : 1;
		entity.setDistributorId(distId);
		entity.setProvince(input.province());
		entity.setCity(input.city());
		entity.setArea(input.area());
		entity.setRegionsId(input.regionsId());
		entity.setRegions(input.regions());
		entity.setAddress(input.address());
		entity.setMobile(sensitiveFieldEncryptor.encrypt(input.mobile()));
		entity.setContact(
				sensitiveFieldEncryptor.encrypt(truncateByCodePoints(input.contact(), 50)));
		entity.setMerchantId(input.merchantId());
		entity.setSupplierId(input.supplierId());
		entity.setReturnType(input.returnType());
		entity.setIsDefault(newDefault);
		entity.setUpdated(System.currentTimeMillis() / 1000L);
		int affected = distributorAftersalesAddressMapper.updateById(entity);
		if (affected == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		LinkedHashMap<String, Object> langBag = new LinkedHashMap<>();
		langBag.put("province", input.province());
		langBag.put("city", input.city());
		langBag.put("area", input.area());
		langBag.put("address", input.address());
		langBag.put("contact", input.contact());
		aftersalesAddressOutsideMultiLangWriteService.updateLangData(companyId, addressId, langBag, requestLangTag);
		DistributorAftersalesAddress fresh = distributorAftersalesAddressMapper.selectById(addressId);
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return Map.of("status", true, "result", buildResultMap(fresh));
	}

	private boolean hasOtherDefaultAddressForDistributor(long companyId, long distributorId, long excludeAddressId) {
		LambdaQueryWrapper<DistributorAftersalesAddress> w = new LambdaQueryWrapper<>();
		w.eq(DistributorAftersalesAddress::getCompanyId, companyId)
				.eq(DistributorAftersalesAddress::getDistributorId, distributorId)
				.eq(DistributorAftersalesAddress::getIsDefault, 1)
				.ne(DistributorAftersalesAddress::getAddressId, excludeAddressId);
		return distributorAftersalesAddressMapper.selectCount(w) > 0;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> setDistributorAfterSalesAddress(
			DistributorAftersalesAddressSetLogisticsInput input, String requestLangTag) {
		String productModel = shopMenuService.resolveProductModelKeyForCompany(input.companyId());

		String lng = null;
		String lat = null;
		if ("standard".equals(productModel) || "platform".equals(productModel)) {
			CompanyMapGeocodePort.GeocodeLatLng coords =
					companyMapGeocodePort.geocode(input.companyId(), input.city(), input.address());
			lng = coords.lng();
			lat = coords.lat();
		}

		if (input.distributorIds() != null && !input.distributorIds().isEmpty()) {
			Map<String, Object> lastResult = null;
			for (long distId : input.distributorIds()) {
				lastResult = insertLogisticsRow(input, distId, lng, lat, requestLangTag);
			}
			return Map.of("status", true, "result", lastResult);
		}

		long distributorId;
		if (input.distributorIds() != null && input.distributorIds().size() == 1) {
			distributorId = input.distributorIds().get(0);
		} else if (StringUtils.hasText(input.distributorIdRaw())) {
			try {
				distributorId = Long.parseLong(input.distributorIdRaw().trim());
			} catch (NumberFormatException e) {
				distributorId = 0L;
			}
		} else {
			distributorId = 0L;
		}

		Map<String, Object> single = insertLogisticsRow(input, distributorId, lng, lat, requestLangTag);
		return Map.of("status", true, "result", single);
	}

	private Map<String, Object> insertLogisticsRow(
			DistributorAftersalesAddressSetLogisticsInput input,
			long distributorId,
			String lng,
			String lat,
			String requestLangTag) {
		String contactPlain = truncateByCodePoints(input.contact(), 50);
		String mobilePlain = input.mobile();

		int isDefault = resolveIsDefaultFlag(input.companyId(), distributorId);

		DistributorAftersalesAddress entity = new DistributorAftersalesAddress();
		entity.setDistributorId(distributorId);
		entity.setCompanyId(input.companyId());
		entity.setMerchantId(input.merchantId());
		entity.setSupplierId(input.supplierId());
		entity.setProvince(input.province());
		entity.setCity(input.city());
		entity.setArea(input.area());
		entity.setRegionsId(input.regionsId());
		entity.setRegions(input.regions());
		entity.setAddress(input.address());
		entity.setLng(lng);
		entity.setLat(lat);
		entity.setMobile(sensitiveFieldEncryptor.encrypt(mobilePlain));
		entity.setContact(sensitiveFieldEncryptor.encrypt(contactPlain));
		entity.setPostCode(null);
		entity.setIsDefault(isDefault);
		entity.setName("");
		entity.setHours("");
		entity.setReturnType("logistics");
		long now = System.currentTimeMillis() / 1000L;
		entity.setCreated(now);
		entity.setUpdated(now);

		try {
			distributorAftersalesAddressMapper.insert(entity);
		} catch (DataIntegrityViolationException e) {
			throw new ResourceException("保存失败");
		}

		Long id = entity.getAddressId();
		Map<String, Object> sourceRow = new LinkedHashMap<>();
		sourceRow.put("name", "");
		sourceRow.put("contact", contactPlain);
		sourceRow.put("logo", "");
		sourceRow.put("province", input.province());
		sourceRow.put("city", input.city());
		sourceRow.put("area", input.area());
		sourceRow.put("address", input.address());
		sourceRow.put("introduce", "");
		aftersalesAddressOutsideMultiLangWriteService.addMultiLangAfterInsert(
				id != null ? id : 0L, input.companyId(), sourceRow, requestLangTag);

		return buildResultMap(entity);
	}

	private int resolveIsDefaultFlag(long companyId, long distributorId) {
		LambdaQueryWrapper<DistributorAftersalesAddress> w = new LambdaQueryWrapper<>();
		w.eq(DistributorAftersalesAddress::getCompanyId, companyId)
				.eq(DistributorAftersalesAddress::getDistributorId, distributorId)
				.eq(DistributorAftersalesAddress::getReturnType, "logistics")
				.eq(DistributorAftersalesAddress::getIsDefault, 1)
				.last("LIMIT 1");
		return distributorAftersalesAddressMapper.selectCount(w) > 0 ? 2 : 1;
	}

	private Map<String, Object> buildResultMap(DistributorAftersalesAddress row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("address_id", row.getAddressId());
		m.put("distributor_id", row.getDistributorId());
		m.put("company_id", row.getCompanyId());
		m.put("province", row.getProvince());
		m.put("city", row.getCity());
		m.put("area", row.getArea());
		// regions_id / regions: keep as stored JSON text (String or null, e.g. "[]"); never decode to collections in this map.
		m.put("regions_id", row.getRegionsId());
		m.put("regions", row.getRegions());
		m.put("address", row.getAddress());
		m.put("lng", row.getLng());
		m.put("lat", row.getLat());
		m.put("contact", sensitiveFieldEncryptor.decrypt(row.getContact()));
		m.put("mobile", sensitiveFieldEncryptor.decrypt(row.getMobile()));
		m.put("post_code", row.getPostCode());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		m.put("is_default", row.getIsDefault());
		m.put("merchant_id", row.getMerchantId());
		m.put("name", row.getName());
		m.put("hours", row.getHours());
		m.put("return_type", row.getReturnType());
		m.put("supplier_id", row.getSupplierId());
		return m;
	}

	private static boolean isBlankLngLat(String lng, String lat) {
		if (lng == null || lat == null) {
			return true;
		}
		String ls = lng.strip();
		String ts = lat.strip();
		if (!StringUtils.hasText(ls) || !StringUtils.hasText(ts)) {
			return true;
		}
		return "0".equals(ls) && "0".equals(ts);
	}

	private static String truncateByCodePoints(String s, int maxCodePoints) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		int cp = 0;
		int i = 0;
		while (i < s.length() && cp < maxCodePoints) {
			int ch = s.codePointAt(i);
			cp++;
			i += Character.charCount(ch);
		}
		return s.substring(0, i);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
