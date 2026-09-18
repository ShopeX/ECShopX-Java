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

import cn.shopex.ecshopx.common.dispatch.DistributorCreateEventDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorCreateService {

	private final DistributorMapper distributorMapper;
	private final DistributorWriteRepository distributorWriteRepository;
	private final DistributorMultiLangWriteService distributorMultiLangWriteService;
	private final DistributorCreateEventDispatchPublisher distributorCreateEventDispatchPublisher;
	private final ObjectMapper objectMapper;

	public DistributorCreateService(
			DistributorMapper distributorMapper,
			DistributorWriteRepository distributorWriteRepository,
			DistributorMultiLangWriteService distributorMultiLangWriteService,
			DistributorCreateEventDispatchPublisher distributorCreateEventDispatchPublisher,
			ObjectMapper objectMapper) {
		this.distributorMapper = distributorMapper;
		this.distributorWriteRepository = distributorWriteRepository;
		this.distributorMultiLangWriteService = distributorMultiLangWriteService;
		this.distributorCreateEventDispatchPublisher = distributorCreateEventDispatchPublisher;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> performInsertAndEvents(Map<String, Object> merged, Map<String, Object> user, String requestLangTag) {
		String mobile = str(merged.get("mobile"));
		int wechatDeptIdForInsert = intOrDefault(merged.get("wechat_work_department_id"), 0);
		boolean workWechatSyntheticMobile = wechatDeptIdForInsert > 0 && mobile.matches("^\\d+-\\d+-\\d+$");
		if (!validMobileOrTel(mobile) && !workWechatSyntheticMobile) {
			throw new ResourceException("手机号格式不正确");
		}
		int sourceFrom = intOrDefault(merged.get("source_from"), 1);
		long companyId = toLong(merged.get("company_id"));
		boolean needDefault = sourceFrom != 2 && !distributorWriteRepository.existsDefaultDistributor(companyId);
		boolean distributorSelf = "1".equals(str(merged.get("distributor_self"))) || Boolean.TRUE.equals(merged.get("distributor_self"));
		if (distributorSelf) {
			distributorWriteRepository.findDistributorSelfId(companyId).ifPresent(x -> {
				throw new ResourceException("总部自提店铺已存在");
			});
		}
		String shopCode = str(merged.get("shop_code"));
		if (StringUtils.hasText(shopCode)
				&& distributorWriteRepository.existsOtherWithShopCode(companyId, shopCode, null)) {
			throw new ResourceException("店铺编号已存在");
		}
		if (distributorWriteRepository.existsOtherWithMobile(companyId, mobile, null)) {
			throw new ResourceException("店铺手机号已存在");
		}
		String wdtNo = str(merged.get("wdt_shop_no"));
		if (StringUtils.hasText(wdtNo)
				&& distributorWriteRepository.existsOtherWithWdtShopNo(companyId, wdtNo, null)) {
			throw new ResourceException("旺店通门店已绑定其他店铺");
		}
		long jst = longOrZero(merged.get("jst_shop_id"));
		if (jst > 0 && distributorWriteRepository.existsOtherWithJstShopId(companyId, jst, null)) {
			throw new ResourceException("聚水潭店铺已绑定其他店铺");
		}

		Distributor e = new Distributor();
		e.setCompanyId(companyId);
		e.setMobile(mobile);
		e.setAddress(str(merged.get("address")));
		e.setHouseNumber(str(merged.get("house_number")));
		e.setName(str(merged.get("name")));
		e.setFirstLetter(DistributorFirstLetterUtil.firstLetter(e.getName()));
		e.setAutoSyncGoods(truthy(merged.get("auto_sync_goods")));
		e.setLogo(str(merged.get("logo")));
		e.setContractPhone(StringUtils.hasText(str(merged.get("contract_phone"))) ? str(merged.get("contract_phone")) : "0");
		e.setBanner(str(merged.get("banner")));
		e.setContact(str(merged.get("contact")));
		e.setIsValid(distributorSelf ? "true" : "true");
		e.setLng(str(merged.get("lng")));
		e.setLat(str(merged.get("lat")));
		e.setProvince(str(merged.get("province")));
		e.setCity(str(merged.get("city")));
		e.setArea(str(merged.get("area")));
		e.setHour(str(merged.get("hour")));
		e.setRegions(jsonOrRaw(merged.get("regions")));
		e.setRegionsId(jsonOrRaw(merged.get("regions_id")));
		e.setIsZiti(truthy(merged.get("is_ziti")));
		e.setIsDelivery(truthy(merged.get("is_delivery")));
		e.setIsSelfDelivery(truthy(merged.get("is_self_delivery")));
		e.setShopCode(shopCode);
		e.setDistributorSelf(distributorSelf ? 1 : 0);
		e.setRegionauthId(longOrZero(merged.get("regionauth_id")));
		e.setIsOpen(truthyIsOpenString(merged.get("is_open")) ? "true" : "false");
		Object rateObj = merged.get("rate");
		if (rateObj instanceof Number n) {
			e.setRate(n.intValue());
		} else if (rateObj != null && StringUtils.hasText(rateObj.toString())) {
			e.setRate(Integer.parseInt(rateObj.toString().trim()));
		} else {
			e.setRate(0);
		}
		e.setIsDada(truthy(merged.get("is_dada")));
		e.setBusiness(intOrNull(merged.get("business")));
		e.setDadaShopCreate(Boolean.TRUE.equals(merged.get("dada_shop_create")));
		e.setShansongShopCreate(Boolean.TRUE.equals(merged.get("shansong_shop_create")));
		if (merged.get("shansong_store_id") instanceof Number n) {
			e.setShansongStoreId(n.longValue());
		}
		e.setIntroduce(str(merged.get("introduce")));
		e.setMerchantId(longOrZero(merged.get("merchant_id")));
		e.setDistributorCategoryId(longOrZero(merged.get("distributor_category_id")));
		e.setDistributionType(intOrDefault(merged.get("distribution_type"), 0));
		e.setIsRequireSubdistrict(truthy(merged.get("is_require_subdistrict")));
		e.setIsRequireBuilding(truthy(merged.get("is_require_building")));
		e.setOfflineAftersales(truthyInt(merged.get("offline_aftersales")));
		e.setOfflineAftersalesSelf(truthyInt(merged.get("offline_aftersales_self")));
		e.setOfflineAftersalesDistributorId(str(merged.get("offline_aftersales_distributor_id")));
		e.setOfflineAftersalesOther(truthyInt(merged.get("offline_aftersales_other")));
		e.setFreightTime(intOrDefault(merged.get("freight_time"), 2));
		e.setIsRefundFreight(intOrDefault(merged.get("is_refund_freight"), 0));
		e.setWdtShopNo(wdtNo);
		e.setWdtShopId(longOrZero(merged.get("wdt_shop_id")));
		e.setJstShopId(jst);
		e.setPaymentSubject(intOrDefault(merged.get("payment_subject"), 0));
		e.setSourceFrom(sourceFrom);
		e.setIsAuditGoods(truthy(merged.get("is_audit_goods")));
		e.setWechatWorkDepartmentId(wechatDeptIdForInsert);
		e.setIsDefault(needDefault ? 1 : 0);
		e.setShopId(0L);
		e.setIsDistributor(merged.get("is_distributor") == null || truthy(merged.get("is_distributor")));
		long now = System.currentTimeMillis() / 1000L;
		e.setCreated(now);
		e.setUpdated(now);

		distributorMapper.insert(e);
		Long id = e.getDistributorId();
		distributorMultiLangWriteService.applyAfterInsert(id, companyId, merged, requestLangTag);

		Map<String, Object> row = DistributorRowMaps.toApiRow(e, objectMapper);
		distributorCreateEventDispatchPublisher.publish(row);
		return row;
	}

	private static boolean validMobileOrTel(String s) {
		if (!StringUtils.hasText(s)) {
			return false;
		}
		return s.matches("^1[3-9]\\d{9}$") || s.matches("^\\d{3,4}-?\\d{7,8}$");
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
