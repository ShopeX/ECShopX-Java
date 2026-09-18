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
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.distribution.repository.ItemsAuditByDistributorJdbcRepository;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorUpdateService {

	private static final Set<String> COLUMN_KEYS = Set.of(
			"name",
			"address",
			"house_number",
			"mobile",
			"auto_sync_goods",
			"logo",
			"contract_phone",
			"banner",
			"contact",
			"is_valid",
			"lng",
			"lat",
			"province",
			"city",
			"area",
			"hour",
			"regions_id",
			"regions",
			"is_ziti",
			"is_delivery",
			"is_self_delivery",
			"shop_code",
			"distributor_self",
			"regionauth_id",
			"is_open",
			"rate",
			"is_dada",
			"business",
			"introduce",
			"merchant_id",
			"distributor_category_id",
			"distribution_type",
			"is_require_subdistrict",
			"is_require_building",
			"offline_aftersales",
			"offline_aftersales_self",
			"offline_aftersales_distributor_id",
			"offline_aftersales_other",
			"freight_time",
			"is_open_salesman",
			"show_salesperson",
			"fixed_salesperson_qrcode_url",
			"is_refund_freight",
			"wdt_shop_no",
			"wdt_shop_id",
			"jst_shop_id",
			"open_divided",
			"dada_shop_create",
			"shansong_shop_create",
			"shansong_store_id",
			"is_audit_goods",
			"is_default",
			"review_status",
			"payment_subject",
			"wechat_work_department_id");

	private final DistributorMapper distributorMapper;
	private final DistributorWriteRepository distributorWriteRepository;
	private final DistributorMultiLangWriteService distributorMultiLangWriteService;
	private final ObjectMapper objectMapper;
	private final ItemsAuditByDistributorJdbcRepository itemsAuditByDistributorJdbcRepository;

	public DistributorUpdateService(
			DistributorMapper distributorMapper,
			DistributorWriteRepository distributorWriteRepository,
			DistributorMultiLangWriteService distributorMultiLangWriteService,
			ObjectMapper objectMapper,
			ItemsAuditByDistributorJdbcRepository itemsAuditByDistributorJdbcRepository) {
		this.distributorMapper = distributorMapper;
		this.distributorWriteRepository = distributorWriteRepository;
		this.distributorMultiLangWriteService = distributorMultiLangWriteService;
		this.objectMapper = objectMapper;
		this.itemsAuditByDistributorJdbcRepository = itemsAuditByDistributorJdbcRepository;
	}

	/** Persists distributor changes and returns the API row snapshot for post-commit dispatch. */
	public Map<String, Object> performUpdateAndEvents(Map<String, Object> merged, long distributorId, String requestLangTag) {
		long companyId = toLong(merged.get("company_id"));
		if (!distributorWriteRepository.existsDefaultDistributor(companyId)) {
			merged.put("is_default", 1);
		}
		Distributor info = distributorWriteRepository
				.selectSimpleByCompanyAndId(companyId, distributorId)
				.orElseThrow(() -> new ResourceException("请确认修改数据是否正确"));
		if (merged.containsKey("mobile")) {
			String mob = str(merged.get("mobile"));
			if (!validMobileOrTel(mob)) {
				throw new ResourceException("手机号格式不正确");
			}
		}
		String wdtNo = str(merged.get("wdt_shop_no"));
		if (StringUtils.hasText(wdtNo)
				&& distributorWriteRepository.existsOtherWithWdtShopNo(companyId, wdtNo, distributorId)) {
			throw new ResourceException("旺店通门店已绑定其他店铺");
		}
		if (merged.containsKey("jst_shop_id")) {
			long jst = longOrZero(merged.get("jst_shop_id"));
			if (jst > 0 && distributorWriteRepository.existsOtherWithJstShopId(companyId, jst, distributorId)) {
				throw new ResourceException("聚水潭店铺已绑定其他店铺");
			}
		}
		Object auditGoodsVal = merged.get("is_audit_goods");
		boolean auditGoodsFalsy = auditGoodsVal instanceof Number n && n.longValue() == 0L
				|| "false".equalsIgnoreCase(String.valueOf(auditGoodsVal).trim());
		if (Boolean.TRUE.equals(info.getIsAuditGoods())
				&& merged.containsKey("is_audit_goods")
				&& auditGoodsFalsy) {
			itemsAuditByDistributorJdbcRepository.updateAuditStatusApprovedByDistributorId(companyId, distributorId);
		}
		applyMergedColumns(companyId, distributorId, merged);
		distributorMultiLangWriteService.applyAfterUpdate(distributorId, companyId, merged, requestLangTag);
		Distributor refreshed = distributorWriteRepository
				.selectSimpleByCompanyAndId(companyId, distributorId)
				.orElse(info);
		return DistributorRowMaps.toApiRow(refreshed, objectMapper);
	}

	private void applyMergedColumns(long companyId, long distributorId, Map<String, Object> merged) {
		LambdaUpdateWrapper<Distributor> u = new LambdaUpdateWrapper<>();
		u.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorId, distributorId)
				.ne(Distributor::getIsValid, "delete");
		boolean any = false;
		for (String key : merged.keySet()) {
			if ("company_id".equals(key) || "distributor_id".equals(key)) {
				continue;
			}
			if (!COLUMN_KEYS.contains(key)) {
				continue;
			}
			if (setColumn(u, key, merged)) {
				any = true;
			}
		}
		if (any) {
			u.set(Distributor::getUpdated, System.currentTimeMillis() / 1000L);
			distributorMapper.update(null, u);
		}
	}

	private boolean setColumn(LambdaUpdateWrapper<Distributor> u, String key, Map<String, Object> merged) {
		return switch (key) {
			case "name" -> {
				u.set(Distributor::getName, str(merged.get("name")));
				u.set(Distributor::getFirstLetter, DistributorFirstLetterUtil.firstLetter(str(merged.get("name"))));
				yield true;
			}
			case "address" -> {
				u.set(Distributor::getAddress, str(merged.get("address")));
				yield true;
			}
			case "house_number" -> {
				u.set(Distributor::getHouseNumber, str(merged.get("house_number")));
				yield true;
			}
			case "mobile" -> {
				u.set(Distributor::getMobile, str(merged.get("mobile")));
				yield true;
			}
			case "auto_sync_goods" -> {
				u.set(Distributor::getAutoSyncGoods, truthy(merged.get("auto_sync_goods")));
				yield true;
			}
			case "logo" -> {
				u.set(Distributor::getLogo, str(merged.get("logo")));
				yield true;
			}
			case "contract_phone" -> {
				String c = str(merged.get("contract_phone"));
				u.set(Distributor::getContractPhone, StringUtils.hasText(c) ? c : "0");
				yield true;
			}
			case "banner" -> {
				u.set(Distributor::getBanner, str(merged.get("banner")));
				yield true;
			}
			case "contact" -> {
				u.set(Distributor::getContact, str(merged.get("contact")));
				yield true;
			}
			case "is_valid" -> {
				u.set(Distributor::getIsValid, str(merged.get("is_valid")));
				yield true;
			}
			case "lng" -> {
				u.set(Distributor::getLng, str(merged.get("lng")));
				yield true;
			}
			case "lat" -> {
				u.set(Distributor::getLat, str(merged.get("lat")));
				yield true;
			}
			case "province" -> {
				u.set(Distributor::getProvince, str(merged.get("province")));
				yield true;
			}
			case "city" -> {
				u.set(Distributor::getCity, str(merged.get("city")));
				yield true;
			}
			case "area" -> {
				u.set(Distributor::getArea, str(merged.get("area")));
				yield true;
			}
			case "hour" -> {
				u.set(Distributor::getHour, str(merged.get("hour")));
				yield true;
			}
			case "regions_id" -> {
				u.set(Distributor::getRegionsId, jsonOrRaw(merged.get("regions_id")));
				yield true;
			}
			case "regions" -> {
				u.set(Distributor::getRegions, jsonOrRaw(merged.get("regions")));
				yield true;
			}
			case "is_ziti" -> {
				u.set(Distributor::getIsZiti, truthy(merged.get("is_ziti")));
				yield true;
			}
			case "is_delivery" -> {
				u.set(Distributor::getIsDelivery, truthy(merged.get("is_delivery")));
				yield true;
			}
			case "is_self_delivery" -> {
				u.set(Distributor::getIsSelfDelivery, truthy(merged.get("is_self_delivery")));
				yield true;
			}
			case "shop_code" -> {
				u.set(Distributor::getShopCode, str(merged.get("shop_code")));
				yield true;
			}
			case "distributor_self" -> {
				u.set(Distributor::getDistributorSelf, truthyOne(merged.get("distributor_self")) ? 1 : 0);
				yield true;
			}
			case "regionauth_id" -> {
				u.set(Distributor::getRegionauthId, longOrZero(merged.get("regionauth_id")));
				yield true;
			}
			case "is_open" -> {
				u.set(Distributor::getIsOpen, truthyIsOpenString(merged.get("is_open")) ? "true" : "false");
				yield true;
			}
			case "rate" -> {
				u.set(Distributor::getRate, intOrZero(merged.get("rate")));
				yield true;
			}
			case "is_dada" -> {
				u.set(Distributor::getIsDada, truthy(merged.get("is_dada")));
				yield true;
			}
			case "business" -> {
				u.set(Distributor::getBusiness, intOrNull(merged.get("business")));
				yield true;
			}
			case "introduce" -> {
				u.set(Distributor::getIntroduce, str(merged.get("introduce")));
				yield true;
			}
			case "merchant_id" -> {
				u.set(Distributor::getMerchantId, longOrZero(merged.get("merchant_id")));
				yield true;
			}
			case "distributor_category_id" -> {
				u.set(Distributor::getDistributorCategoryId, longOrZero(merged.get("distributor_category_id")));
				yield true;
			}
			case "distribution_type" -> {
				u.set(Distributor::getDistributionType, intOrDefault(merged.get("distribution_type"), 0));
				yield true;
			}
			case "is_require_subdistrict" -> {
				u.set(Distributor::getIsRequireSubdistrict, truthy(merged.get("is_require_subdistrict")));
				yield true;
			}
			case "is_require_building" -> {
				u.set(Distributor::getIsRequireBuilding, truthy(merged.get("is_require_building")));
				yield true;
			}
			case "offline_aftersales" -> {
				u.set(Distributor::getOfflineAftersales, truthyInt(merged.get("offline_aftersales")));
				yield true;
			}
			case "offline_aftersales_self" -> {
				u.set(Distributor::getOfflineAftersalesSelf, truthyInt(merged.get("offline_aftersales_self")));
				yield true;
			}
			case "offline_aftersales_distributor_id" -> {
				u.set(Distributor::getOfflineAftersalesDistributorId, str(merged.get("offline_aftersales_distributor_id")));
				yield true;
			}
			case "offline_aftersales_other" -> {
				u.set(Distributor::getOfflineAftersalesOther, truthyInt(merged.get("offline_aftersales_other")));
				yield true;
			}
			case "freight_time" -> {
				u.set(Distributor::getFreightTime, intOrDefault(merged.get("freight_time"), 2));
				yield true;
			}
			case "is_open_salesman" -> {
				u.set(Distributor::getIsOpenSalesman, truthy(merged.get("is_open_salesman")));
				yield true;
			}
			case "show_salesperson" -> {
				u.set(Distributor::getShowSalesperson, intOrDefault(merged.get("show_salesperson"), 0));
				yield true;
			}
			case "fixed_salesperson_qrcode_url" -> {
				u.set(Distributor::getFixedSalespersonQrcodeUrl, str(merged.get("fixed_salesperson_qrcode_url")));
				yield true;
			}
			case "is_refund_freight" -> {
				u.set(Distributor::getIsRefundFreight, intOrDefault(merged.get("is_refund_freight"), 0));
				yield true;
			}
			case "wdt_shop_no" -> {
				u.set(Distributor::getWdtShopNo, str(merged.get("wdt_shop_no")));
				yield true;
			}
			case "wdt_shop_id" -> {
				u.set(Distributor::getWdtShopId, longOrZero(merged.get("wdt_shop_id")));
				yield true;
			}
			case "jst_shop_id" -> {
				u.set(Distributor::getJstShopId, longOrZero(merged.get("jst_shop_id")));
				yield true;
			}
			case "open_divided" -> {
				Object od = merged.get("open_divided");
				long v = od instanceof Number n ? n.longValue() : longOrZero(od);
				u.set(Distributor::getOpenDivided, v);
				yield true;
			}
			case "dada_shop_create" -> {
				u.set(Distributor::getDadaShopCreate, truthy(merged.get("dada_shop_create")));
				yield true;
			}
			case "shansong_shop_create" -> {
				u.set(Distributor::getShansongShopCreate, truthy(merged.get("shansong_shop_create")));
				yield true;
			}
			case "shansong_store_id" -> {
				Object s = merged.get("shansong_store_id");
				u.set(Distributor::getShansongStoreId, s instanceof Number n ? n.longValue() : longOrZero(s));
				yield true;
			}
			case "is_audit_goods" -> {
				u.set(Distributor::getIsAuditGoods, truthy(merged.get("is_audit_goods")));
				yield true;
			}
			case "is_default" -> {
				Object d = merged.get("is_default");
				int v = d instanceof Number n ? n.intValue() : (truthy(d) ? 1 : 0);
				u.set(Distributor::getIsDefault, v);
				yield true;
			}
			case "review_status" -> {
				u.set(Distributor::getReviewStatus, Boolean.TRUE.equals(merged.get("review_status")) || truthy(merged.get("review_status")));
				yield true;
			}
			case "payment_subject" -> {
				u.set(Distributor::getPaymentSubject, intOrDefault(merged.get("payment_subject"), 0));
				yield true;
			}
			case "wechat_work_department_id" -> {
				u.set(Distributor::getWechatWorkDepartmentId, intOrDefault(merged.get("wechat_work_department_id"), 0));
				yield true;
			}
			default -> false;
		};
	}

	private String jsonOrRaw(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof String s) {
			return s;
		}
		try {
			return objectMapper.writeValueAsString(v);
		} catch (Exception e) {
			return v.toString();
		}
	}

	private static boolean validMobileOrTel(String s) {
		if (!StringUtils.hasText(s)) {
			return false;
		}
		return s.matches("^1[3-9]\\d{9}$") || s.matches("^\\d{3,4}-?\\d{7,8}$");
	}

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"false".equalsIgnoreCase(s) && !"0".equals(s);
	}

	private static boolean truthyOne(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static boolean truthyIsOpenString(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString().trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static int truthyInt(Object v) {
		return truthy(v) ? 1 : 0;
	}

	private static int intOrDefault(Object o, int d) {
		if (o == null) {
			return d;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (Exception e) {
			return d;
		}
	}

	private static int intOrZero(Object o) {
		return intOrDefault(o, 0);
	}

	private static Integer intOrNull(Object o) {
		if (o == null || !StringUtils.hasText(o.toString())) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(o.toString().trim());
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static long longOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
