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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class DistributorRowMaps {

	private DistributorRowMaps() {
	}

	static Map<String, Object> toApiRow(Distributor d, ObjectMapper objectMapper) {
		Map<String, Object> m = new LinkedHashMap<>();
		put(m, "distributor_id", d.getDistributorId());
		put(m, "shop_id", d.getShopId());
		put(m, "is_distributor", d.getIsDistributor());
		put(m, "company_id", d.getCompanyId());
		put(m, "mobile", d.getMobile());
		put(m, "address", d.getAddress());
		put(m, "house_number", d.getHouseNumber());
		put(m, "name", d.getName());
		put(m, "first_letter", d.getFirstLetter());
		put(m, "auto_sync_goods", d.getAutoSyncGoods());
		put(m, "logo", d.getLogo());
		put(m, "contract_phone", d.getContractPhone());
		put(m, "banner", d.getBanner());
		put(m, "contact", d.getContact());
		put(m, "is_valid", d.getIsValid());
		put(m, "lng", d.getLng());
		put(m, "lat", d.getLat());
		put(m, "child_count", d.getChildCount());
		put(m, "is_default", d.getIsDefault());
		put(m, "is_audit_goods", d.getIsAuditGoods());
		put(m, "is_ziti", d.getIsZiti());
		put(m, "regions_id", decodeJsonList(d.getRegionsId(), objectMapper));
		put(m, "regions", decodeJsonList(d.getRegions(), objectMapper));
		put(m, "is_domestic", d.getIsDomestic());
		put(m, "is_direct_store", d.getIsDirectStore());
		put(m, "province", d.getProvince());
		put(m, "is_delivery", d.getIsDelivery());
		put(m, "city", d.getCity());
		put(m, "area", d.getArea());
		put(m, "hour", d.getHour());
		put(m, "created", d.getCreated());
		put(m, "updated", d.getUpdated());
		put(m, "shop_code", d.getShopCode());
		put(m, "wechat_work_department_id", d.getWechatWorkDepartmentId());
		put(m, "distributor_self", d.getDistributorSelf());
		Long regionauthId = d.getRegionauthId();
		put(m, "regionauth_id", (regionauthId == null || regionauthId == 0L) ? 0L : regionauthId);
		put(m, "is_open", d.getIsOpen());
		put(m, "rate", d.getRate());
		put(m, "is_dada", d.getIsDada());
		put(m, "business", d.getBusiness());
		put(m, "dada_shop_create", d.getDadaShopCreate());
		put(m, "shansong_shop_create", d.getShansongShopCreate());
		put(m, "shansong_store_id", d.getShansongStoreId());
		put(m, "introduce", d.getIntroduce());
		put(m, "merchant_id", d.getMerchantId());
		put(m, "merchant_name", d.getMerchantName() != null ? d.getMerchantName() : "");
		put(m, "distribution_type", d.getDistributionType());
		put(m, "is_require_subdistrict", d.getIsRequireSubdistrict());
		put(m, "is_require_building", d.getIsRequireBuilding());
		put(m, "delivery_distance", d.getDeliveryDistance());
		put(m, "offline_aftersales", d.getOfflineAftersales());
		put(m, "offline_aftersales_self", d.getOfflineAftersalesSelf());
		put(m, "offline_aftersales_distributor_id", d.getOfflineAftersalesDistributorId());
		put(m, "offline_aftersales_other", d.getOfflineAftersalesOther());
		put(m, "is_self_delivery", d.getIsSelfDelivery());
		put(m, "freight_time", d.getFreightTime());
		put(m, "is_refund_freight", d.getIsRefundFreight());
		put(m, "wdt_shop_no", d.getWdtShopNo());
		put(m, "wdt_shop_id", d.getWdtShopId());
		put(m, "jst_shop_id", d.getJstShopId());
		put(m, "kuaizhen_store_id", d.getKuaizhenStoreId());
		put(m, "open_divided", d.getOpenDivided());
		put(m, "payment_subject", d.getPaymentSubject());
		Long distributorCategoryId = d.getDistributorCategoryId();
		put(m, "distributor_category_id", distributorCategoryId == null ? 0L : distributorCategoryId);
		put(m, "review_status", d.getReviewStatus());
		put(m, "dealer_id", d.getDealerId());
		put(m, "split_ledger_info", d.getSplitLedgerInfo());
		put(m, "bspay_split_ledger_info", d.getBspaySplitLedgerInfo());
		put(m, "source_from", d.getSourceFrom());
		put(m, "is_open_salesman", d.getIsOpenSalesman());
		put(m, "show_salesperson", d.getShowSalesperson());
		put(m, "fixed_salesperson_qrcode_url", d.getFixedSalespersonQrcodeUrl());
		return m;
	}

	private static void put(Map<String, Object> m, String k, Object v) {
		m.put(k, v);
	}

	private static Object decodeJsonList(String json, ObjectMapper objectMapper) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		String t = json.trim();
		if (t.startsWith("[")) {
			try {
				return objectMapper.readValue(t, new TypeReference<List<Object>>() {});
			} catch (Exception e) {
				return json;
			}
		}
		if (t.contains(",")) {
			String[] parts = t.split(",");
			List<String> list = new java.util.ArrayList<>();
			for (String p : parts) {
				if (StringUtils.hasText(p)) {
					list.add(p.trim());
				}
			}
			return list;
		}
		return List.of(t);
	}
}
