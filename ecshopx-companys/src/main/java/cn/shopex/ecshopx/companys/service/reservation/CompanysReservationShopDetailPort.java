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

package cn.shopex.ecshopx.companys.service.reservation;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Resources;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.ResourcesMapper;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanysReservationShopDetailPort implements ReservationShopDetailPort {

	private final WxShopsMapper wxShopsMapper;
	private final ResourcesMapper resourcesMapper;

	public CompanysReservationShopDetailPort(WxShopsMapper wxShopsMapper, ResourcesMapper resourcesMapper) {
		this.wxShopsMapper = wxShopsMapper;
		this.resourcesMapper = resourcesMapper;
	}

	@Override
	public Map<String, Object> getShopsDetail(long shopId, long companyId) {
		return loadShopDetail(shopId, companyId, "该门店已经过期无法预约");
	}

	@Override
	public Map<String, Object> getShopsDetailForWorkShiftCreate(long shopId, long companyId) {
		return loadShopDetail(shopId, companyId, "店铺已经过期，无法完成排班");
	}

	@Override
	public Optional<Map<String, Object>> findShopForWxappTimelist(long shopId, long companyId) {
		WxShops shop = wxShopsMapper.selectById(shopId);
		if (shop == null) {
			return Optional.empty();
		}
		if (companyId > 0 && !Objects.equals(shop.getCompanyId(), companyId)) {
			return Optional.empty();
		}
		Long expiredAt = shop.getExpiredAt();
		long now = Instant.now().getEpochSecond();
		if (expiredAt == null || expiredAt <= now) {
			return Optional.empty();
		}
		String hour = shop.getHour();
		if (!StringUtils.hasText(hour == null ? "" : hour.trim())) {
			return Optional.empty();
		}
		Long resourceId = shop.getResourceId();
		if (resourceId != null && resourceId > 0) {
			Resources resource = resourcesMapper.selectById(resourceId);
			if (resource == null) {
				throw new ResourceException("资源包不存在");
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("company_id", shop.getCompanyId());
		out.put("hour", hour);
		return Optional.of(out);
	}

	@Override
	public Optional<Map<String, Object>> findShopForEveryDayTimePeriod(long shopId, long companyId) {
		WxShops shop = wxShopsMapper.selectById(shopId);
		if (shop == null) {
			return Optional.empty();
		}
		if (companyId > 0 && !Objects.equals(shop.getCompanyId(), companyId)) {
			return Optional.empty();
		}
		Long expiredAt = shop.getExpiredAt();
		long now = Instant.now().getEpochSecond();
		if (expiredAt == null || expiredAt <= now) {
			return Optional.empty();
		}
		String hour = shop.getHour();
		if (!StringUtils.hasText(hour == null ? "" : hour.trim())) {
			return Optional.empty();
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("company_id", shop.getCompanyId());
		out.put("hour", hour);
		return Optional.of(out);
	}

	private Map<String, Object> loadShopDetail(long shopId, long companyId, String expiredMessage) {
		WxShops shop = wxShopsMapper.selectById(shopId);
		if (shop == null) {
			throw new ResourceException("您预约的门店不存在");
		}
		if (shop.getCompanyId() != null && companyId > 0 && !Objects.equals(shop.getCompanyId(), companyId)) {
			throw new ResourceException("您预约的门店不存在");
		}
		Long expiredAt = shop.getExpiredAt();
		long now = Instant.now().getEpochSecond();
		if (expiredAt == null || expiredAt <= now) {
			throw new ResourceException(expiredMessage);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("wx_shop_id", shop.getWxShopId());
		out.put("company_id", shop.getCompanyId());
		String storeName = shop.getStoreName();
		if (StringUtils.hasText(storeName)) {
			out.put("shop_name", storeName.trim());
			out.put("store_name", storeName.trim());
		}
		String shopAddress = shop.getAddress();
		if (StringUtils.hasText(shopAddress)) {
			out.put("shop_address", shopAddress.trim());
			out.put("address", shopAddress.trim());
		}
		String contractPhone = shop.getContractPhone();
		if (StringUtils.hasText(contractPhone)) {
			out.put("contract_phone", contractPhone.trim());
		}
		out.put("hour", shop.getHour());
		out.put("expired_at", shop.getExpiredAt());
		Long resourceId = shop.getResourceId();
		if (resourceId != null && resourceId > 0 && expiredAt != null && expiredAt > now) {
			Resources resource = resourcesMapper.selectById(resourceId);
			if (resource == null) {
				throw new ResourceException("资源包不存在");
			}
			out.put("resource_id", resourceId);
			out.put("resource_name", resource.getResourceName());
		} else {
			out.put("resource_id", null);
		}
		return out;
	}
}
