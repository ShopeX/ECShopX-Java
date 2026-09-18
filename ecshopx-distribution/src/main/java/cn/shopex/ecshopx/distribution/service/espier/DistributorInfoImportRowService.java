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

package cn.shopex.ecshopx.distribution.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.integration.MapGeocodeClient;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorCategoryService;
import cn.shopex.ecshopx.distribution.service.DistributorCreateService;
import cn.shopex.ecshopx.espier.service.address.EspierRegionLabelResolveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DistributorInfoImportRowService {

	private static final Pattern HOUR_MIN = Pattern.compile("^([0-1][0-9]|2[0-3]):([0-5][0-9])$");

	private final DistributorMapper distributorMapper;
	private final EspierRegionLabelResolveService espierRegionLabelResolveService;
	private final MapGeocodeClient mapGeocodeClient;
	private final ObjectMapper objectMapper;
	private final DistributorCreateService distributorCreateService;
	private final DistributorCategoryService distributorCategoryService;

	public DistributorInfoImportRowService(
			DistributorMapper distributorMapper,
			EspierRegionLabelResolveService espierRegionLabelResolveService,
			MapGeocodeClient mapGeocodeClient,
			ObjectMapper objectMapper,
			DistributorCreateService distributorCreateService,
			DistributorCategoryService distributorCategoryService) {
		this.distributorMapper = distributorMapper;
		this.espierRegionLabelResolveService = espierRegionLabelResolveService;
		this.mapGeocodeClient = mapGeocodeClient;
		this.objectMapper = objectMapper;
		this.distributorCreateService = distributorCreateService;
		this.distributorCategoryService = distributorCategoryService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void acceptRow(long companyId, long operatorId, long distributorId, long supplierId, long merchantId, Map<String, Object> row) {
		Map<String, String> d = pick(row);
		String addr1 = d.get("addr1");
		if (!StringUtils.hasText(addr1)) {
			throw new BadRequestException("店铺所在省市区不能为空");
		}
		String[] regions = addr1.split(",");
		if (regions.length < 3) {
			throw new ResourceException("错误，省市区错误");
		}
		String fir = regions[0].trim();
		String sec = regions[1].trim();
		String thi = regions[2].trim();
		int[] ids = espierRegionLabelResolveService.resolveIds(fir, sec, thi);
		if (ids[0] <= 1 || ids[1] <= 1 || ids[2] <= 1) {
			throw new ResourceException("错误，省市区错误");
		}
		String regionsIdJson = toJson(List.of(ids[0], ids[1], ids[2]));
		String regionsJson = toJson(List.of(fir, sec, thi));

		String shopCode = d.get("shop_code");
		if (!StringUtils.hasText(shopCode)) {
			throw new BadRequestException("店铺号不能为空");
		}
		String name = d.get("name");
		if (!StringUtils.hasText(name)) {
			throw new BadRequestException("店铺名称不能为空");
		}
		String contact = d.get("contact");
		if (!StringUtils.hasText(contact)) {
			throw new BadRequestException("联系人姓名不能为空");
		}
		String contractPhone = d.get("contract_phone");
		if (!StringUtils.hasText(contractPhone)) {
			throw new BadRequestException("联系方式不能为空");
		}
		String addr2 = nz(d.get("addr2"));
		if (!StringUtils.hasText(addr2)) {
			throw new BadRequestException("店铺详细地址不能为空");
		}
		String isDeliveryCell = d.get("is_delivery");
		if (!StringUtils.hasText(isDeliveryCell)) {
			throw new BadRequestException("开启快递配送不能为空");
		}
		String autoSyncCell = d.get("auto_sync_goods");
		if (!StringUtils.hasText(autoSyncCell)) {
			throw new BadRequestException("自动同步商品不能为空");
		}

		String fullAddress;
		if (fir.equals(sec)) {
			fullAddress = sec + thi + addr2;
		} else {
			fullAddress = fir + sec + thi + addr2;
		}
		String regionName = fir.equals(sec) ? fir : sec;
		String cityName =
				regionName.replace("市", "").replace("省", "").replace("自治区", "").replace("特别行政区", "");

		MapGeocodeClient.LngLat geo = mapGeocodeClient.getLatAndLng(companyId, cityName, fullAddress);

		Long matchedCategoryId = null;
		if (StringUtils.hasText(d.get("distributor_category"))) {
			matchedCategoryId =
					distributorCategoryService.getCategoryIdByName(companyId, d.get("distributor_category"));
		}

		boolean isDelivery = "是".equals(isDeliveryCell);
		boolean autoSync = "是".equals(autoSyncCell);
		String hour = "";
		String hour1 = d.get("hour1");
		String hour2 = d.get("hour2");
		if (StringUtils.hasText(hour1) && StringUtils.hasText(hour2)) {
			String h1 = hour1.trim();
			String h2 = hour2.trim();
			if (!HOUR_MIN.matcher(h1).matches()) {
				throw new ResourceException("错误，经营开始时间格式错误");
			}
			if (!HOUR_MIN.matcher(h2).matches()) {
				throw new ResourceException("错误，经营结束时间格式错误");
			}
			hour = h1 + "-" + h2;
		}

		Distributor existing =
				distributorMapper.selectOne(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getShopCode, shopCode.trim())
								.last("LIMIT 1"));
		if (existing != null && existing.getDistributorId() != null) {
			LambdaUpdateWrapper<Distributor> uw = new LambdaUpdateWrapper<>();
			uw.eq(Distributor::getShopCode, shopCode.trim());
			uw.set(Distributor::getProvince, fir)
					.set(Distributor::getCity, sec)
					.set(Distributor::getArea, thi)
					.set(Distributor::getIsDada, true)
					.set(Distributor::getRegionsId, regionsIdJson)
					.set(Distributor::getRegions, regionsJson)
					.set(Distributor::getContact, contact)
					.set(Distributor::getMobile, contractPhone)
					.set(Distributor::getContractPhone, contractPhone)
					.set(Distributor::getName, name)
					.set(Distributor::getAddress, addr2)
					.set(Distributor::getHouseNumber, nz(d.get("addr3")))
					.set(Distributor::getDadaShopCreate, false)
					.set(Distributor::getShansongShopCreate, false)
					.set(Distributor::getLng, geo.lng())
					.set(Distributor::getLat, geo.lat())
					.set(Distributor::getIsDelivery, isDelivery)
					.set(Distributor::getAutoSyncGoods, autoSync)
					.set(Distributor::getUpdated, System.currentTimeMillis() / 1000L);
			if (StringUtils.hasText(hour)) {
				uw.set(Distributor::getHour, hour);
			}
			if (StringUtils.hasText(d.get("distribution_type"))) {
				try {
					uw.set(Distributor::getDistributionType, Integer.parseInt(d.get("distribution_type").trim()));
				} catch (NumberFormatException ignored) {
					// keep existing
				}
			}
			if (StringUtils.hasText(d.get("logo"))) {
				uw.set(Distributor::getLogo, d.get("logo").trim());
			}
			if (StringUtils.hasText(d.get("banner"))) {
				uw.set(Distributor::getBanner, d.get("banner").trim());
			}
			if (StringUtils.hasText(d.get("wdt_shop_no"))) {
				uw.set(Distributor::getWdtShopNo, d.get("wdt_shop_no").trim());
			}
			if (StringUtils.hasText(d.get("jst_shop_id"))) {
				uw.set(Distributor::getJstShopId, d.get("jst_shop_id").trim());
			}
			if (matchedCategoryId != null && matchedCategoryId > 0L) {
				uw.set(Distributor::getDistributorCategoryId, matchedCategoryId);
			}
			distributorMapper.update(null, uw);
			return;
		}

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("mobile", contractPhone);
		merged.put("address", addr2);
		merged.put("house_number", nz(d.get("addr3")));
		merged.put("name", name);
		merged.put("first_letter", "");
		merged.put("auto_sync_goods", autoSync);
		merged.put("logo", nz(d.get("logo")));
		merged.put("contract_phone", contractPhone);
		merged.put("banner", nz(d.get("banner")));
		merged.put("contact", contact);
		merged.put("lng", geo.lng());
		merged.put("lat", geo.lat());
		merged.put("province", fir);
		merged.put("city", sec);
		merged.put("area", thi);
		merged.put("hour", hour);
		merged.put("regions", regionsJson);
		merged.put("regions_id", regionsIdJson);
		merged.put("is_ziti", false);
		merged.put("is_delivery", isDelivery);
		merged.put("is_self_delivery", false);
		merged.put("shop_code", shopCode.trim());
		merged.put("distributor_self", 0);
		merged.put("regionauth_id", 0L);
		merged.put("is_open", "false");
		merged.put("rate", 0);
		merged.put("is_dada", true);
		merged.put("business", null);
		merged.put("dada_shop_create", false);
		merged.put("shansong_shop_create", false);
		merged.put("introduce", "");
		merged.put("merchant_id", merchantId);
		if (StringUtils.hasText(d.get("distribution_type"))) {
			try {
				merged.put("distribution_type", Integer.parseInt(d.get("distribution_type").trim()));
			} catch (NumberFormatException e) {
				merged.put("distribution_type", 0);
			}
		} else {
			merged.put("distribution_type", 0);
		}
		merged.put("is_require_subdistrict", false);
		merged.put("is_require_building", false);
		merged.put("offline_aftersales", 0);
		merged.put("offline_aftersales_self", 0);
		merged.put("offline_aftersales_distributor_id", "");
		merged.put("offline_aftersales_other", 0);
		merged.put("freight_time", 2);
		merged.put("is_refund_freight", 0);
		merged.put("wdt_shop_no", nz(d.get("wdt_shop_no")));
		merged.put("wdt_shop_id", 0L);
		if (StringUtils.hasText(d.get("jst_shop_id"))) {
			try {
				merged.put("jst_shop_id", Long.parseLong(d.get("jst_shop_id").trim()));
			} catch (NumberFormatException e) {
				merged.put("jst_shop_id", 0L);
			}
		} else {
			merged.put("jst_shop_id", 0L);
		}
		merged.put("payment_subject", 0);
		merged.put("source_from", 1);
		merged.put("is_audit_goods", false);
		merged.put("wechat_work_department_id", 0);
		if (matchedCategoryId != null && matchedCategoryId > 0L) {
			merged.put("distributor_category_id", matchedCategoryId);
		}
		distributorCreateService.performInsertAndEvents(merged, new LinkedHashMap<>(), "zh-CN");
	}

	private Map<String, String> pick(Map<String, Object> row) {
		String[] keys = {
			"distribution_type",
			"shop_code",
			"name",
			"distributor_category",
			"contact",
			"contract_phone",
			"addr1",
			"addr2",
			"addr3",
			"hour1",
			"hour2",
			"is_delivery",
			"auto_sync_goods",
			"logo",
			"banner",
			"wdt_shop_no",
			"jst_shop_id"
		};
		Map<String, String> m = new LinkedHashMap<>();
		for (String k : keys) {
			Object v = row.get(k);
			m.put(k, v == null ? "" : String.valueOf(v).trim());
		}
		return m;
	}

	private String toJson(Object v) {
		try {
			return objectMapper.writeValueAsString(v);
		} catch (JsonProcessingException e) {
			throw new ResourceException("地区数据序列化失败");
		}
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
