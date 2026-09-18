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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import cn.shopex.ecshopx.salesperson.service.support.ShopSalespersonApiFields;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SalespersonGetInfoService {

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final WxShopsMapper wxShopsMapper;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public SalespersonGetInfoService(ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper, WxShopsMapper wxShopsMapper,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor, ObjectMapper objectMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.wxShopsMapper = wxShopsMapper;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getSalespersonInfo(long companyId, String salespersonIdRaw, int datapassBlock) {
		Long parsedId = parseSalespersonId(salespersonIdRaw);
		if (parsedId == null) {
			Map<String, Object> sparse = new LinkedHashMap<>();
			sparse.put("datapass_block", datapassBlock);
			return sparse;
		}

		ShopSalesperson entity = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonId, parsedId)
				.last("LIMIT 1"));
		if (entity == null) {
			Map<String, Object> sparse = new LinkedHashMap<>();
			sparse.put("datapass_block", datapassBlock);
			return sparse;
		}

		LinkedHashMap<String, Object> info = new LinkedHashMap<>();
		appendPayloadFieldsBeforeRole(info, entity);
		info.put("role", entity.getRole() != null ? entity.getRole() : "");
		appendPayloadFieldsAfterRole(info, entity);

		List<ShopsRelSalesperson> relRows = shopsRelSalespersonMapper.selectList(
				new LambdaQueryWrapper<ShopsRelSalesperson>()
						.eq(ShopsRelSalesperson::getCompanyId, companyId)
						.eq(ShopsRelSalesperson::getSalespersonId, entity.getSalespersonId())
						.orderByAsc(ShopsRelSalesperson::getShopId));

		List<Long> shopIds = new ArrayList<>();
		List<Long> distributorIds = new ArrayList<>();

		for (ShopsRelSalesperson rel : relRows) {
			String st = rel.getStoreType();
			Long sid = rel.getShopId();
			if (sid == null) {
				continue;
			}
			if ("shop".equals(st)) {
				shopIds.add(sid);
				info.put("store_type", "shop");
			} else if ("distributor".equals(st)) {
				distributorIds.add(sid);
				info.put("store_type", "distributor");
			}
		}

		info.put("shop_ids", new ArrayList<>(shopIds));
		info.put("distributor_ids", new ArrayList<>(distributorIds));

		Long firstDist = distributorIds.isEmpty() ? null : distributorIds.get(0);
		if (firstDist != null) {
			info.put("distributor_id", String.valueOf(firstDist));
		} else {
			info.put("distributor_id", Boolean.FALSE);
		}

		long nowSec = Instant.now().getEpochSecond();

		if (!shopIds.isEmpty()) {
			List<WxShops> wxRows = wxShopsMapper.selectList(new LambdaQueryWrapper<WxShops>()
					.eq(WxShops::getCompanyId, companyId)
					.in(WxShops::getWxShopId, shopIds)
					.orderByDesc(WxShops::getWxShopId));
			List<Map<String, Object>> shopList = new ArrayList<>(wxRows.size());
			for (WxShops ws : wxRows) {
				shopList.add(wxShopToMap(ws, nowSec));
			}
			info.put("shopList", shopList);
		}

		List<Map<String, Object>> easyRows = List.of();
		if (!distributorIds.isEmpty()) {
			easyRows = distributorRepositoryGetInfoSimpleService.listEasylistsByDistributorIds(companyId,
					distributorIds);
			info.put("distributorList", easyRows);
		}

		if (firstDist != null) {
			String storeName = "";
			String shopCode = "";
			for (Map<String, Object> row : easyRows) {
				if (row == null) {
					continue;
				}
				if (!distributorIdMatches(row.get("distributor_id"), firstDist)) {
					continue;
				}
				Object n = row.get("name");
				Object sc = row.get("shop_code");
				storeName = n != null ? String.valueOf(n) : "";
				shopCode = sc != null ? String.valueOf(sc) : "";
				break;
			}
			info.put("store_name", storeName);
			info.put("shop_code", shopCode);
		}

		info.put("datapass_block", datapassBlock);
		return info;
	}

	public Map<String, Object> getShoppingGuideDetailForH5(long salespersonId) {
		ShopSalesperson entity = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.eq(ShopSalesperson::getIsValid, "true")
				.last("LIMIT 1"));
		if (entity == null) {
			return new LinkedHashMap<>();
		}

		long companyId = entity.getCompanyId();

		LinkedHashMap<String, Object> info = new LinkedHashMap<>();
		appendPayloadFieldsBeforeRole(info, entity);
		info.put("role", entity.getRole() != null ? entity.getRole() : "");
		appendPayloadFieldsAfterRole(info, entity);

		List<ShopsRelSalesperson> relRows = shopsRelSalespersonMapper.selectList(
				new LambdaQueryWrapper<ShopsRelSalesperson>()
						.eq(ShopsRelSalesperson::getCompanyId, companyId)
						.eq(ShopsRelSalesperson::getSalespersonId, entity.getSalespersonId())
						.orderByAsc(ShopsRelSalesperson::getShopId));

		List<Long> shopIds = new ArrayList<>();
		List<Long> distributorIds = new ArrayList<>();

		for (ShopsRelSalesperson rel : relRows) {
			String st = rel.getStoreType();
			Long sid = rel.getShopId();
			if (sid == null) {
				continue;
			}
			if ("shop".equals(st)) {
				shopIds.add(sid);
				info.put("store_type", "shop");
			} else if ("distributor".equals(st)) {
				distributorIds.add(sid);
				info.put("store_type", "distributor");
			}
		}

		info.put("shop_ids", new ArrayList<>(shopIds));
		info.put("distributor_ids", new ArrayList<>(distributorIds));

		Long firstDist = distributorIds.isEmpty() ? null : distributorIds.get(0);
		if (firstDist != null) {
			info.put("distributor_id", String.valueOf(firstDist));
		} else {
			info.put("distributor_id", Boolean.FALSE);
		}

		List<Map<String, Object>> easyRows = List.of();
		if (!distributorIds.isEmpty()) {
			easyRows = distributorRepositoryGetInfoSimpleService.listEasylistsByDistributorIds(companyId,
					distributorIds);
		}

		if (firstDist != null) {
			String storeName = "";
			String shopCode = "";
			for (Map<String, Object> row : easyRows) {
				if (row == null) {
					continue;
				}
				if (!distributorIdMatches(row.get("distributor_id"), firstDist)) {
					continue;
				}
				Object n = row.get("name");
				Object sc = row.get("shop_code");
				storeName = n != null ? String.valueOf(n) : "";
				shopCode = sc != null ? String.valueOf(sc) : "";
				break;
			}
			info.put("store_name", storeName);
			info.put("shop_code", shopCode);
		}

		return info;
	}

	public Map<String, Object> getShoppingGuideDetailForWorkWechatLogin(long companyId, String workClearUserid) {
		if (!StringUtils.hasText(workClearUserid)) {
			return new LinkedHashMap<>();
		}
		ShopSalesperson entity = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getIsValid, "true")
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.eq(ShopSalesperson::getWorkClearUserid, workClearUserid)
				.last("LIMIT 1"));
		if (entity == null) {
			return new LinkedHashMap<>();
		}

		LinkedHashMap<String, Object> info = new LinkedHashMap<>();
		appendPayloadFieldsBeforeRole(info, entity);
		info.put("role", entity.getRole() != null ? entity.getRole() : "");
		appendPayloadFieldsAfterRole(info, entity);

		List<ShopsRelSalesperson> relRows = shopsRelSalespersonMapper.selectList(
				new LambdaQueryWrapper<ShopsRelSalesperson>()
						.eq(ShopsRelSalesperson::getCompanyId, companyId)
						.eq(ShopsRelSalesperson::getSalespersonId, entity.getSalespersonId())
						.orderByAsc(ShopsRelSalesperson::getShopId));

		List<Long> shopIds = new ArrayList<>();
		List<Long> distributorIds = new ArrayList<>();

		for (ShopsRelSalesperson rel : relRows) {
			String st = rel.getStoreType();
			Long sid = rel.getShopId();
			if (sid == null) {
				continue;
			}
			if ("shop".equals(st)) {
				shopIds.add(sid);
				info.put("store_type", "shop");
			} else if ("distributor".equals(st)) {
				distributorIds.add(sid);
				info.put("store_type", "distributor");
			}
		}

		info.put("shop_ids", new ArrayList<>(shopIds));
		info.put("distributor_ids", new ArrayList<>(distributorIds));

		Long firstDist = distributorIds.isEmpty() ? null : distributorIds.get(0);
		if (firstDist != null) {
			info.put("distributor_id", String.valueOf(firstDist));
		} else {
			info.put("distributor_id", Boolean.FALSE);
		}

		List<Map<String, Object>> easyRows = List.of();
		if (!distributorIds.isEmpty()) {
			easyRows = distributorRepositoryGetInfoSimpleService.listEasylistsByDistributorIds(companyId,
					distributorIds);
		}

		if (firstDist != null) {
			String storeName = "";
			String shopCode = "";
			for (Map<String, Object> row : easyRows) {
				if (row == null) {
					continue;
				}
				if (!distributorIdMatches(row.get("distributor_id"), firstDist)) {
					continue;
				}
				Object n = row.get("name");
				Object sc = row.get("shop_code");
				storeName = n != null ? String.valueOf(n) : "";
				shopCode = sc != null ? String.valueOf(sc) : "";
				break;
			}
			info.put("store_name", storeName);
			info.put("shop_code", shopCode);
		}

		return info;
	}

	public Map<String, Object> getShoppingGuideDetailByWorkUseridForInRuleCheck(long companyId, String workUserid) {
		if (!StringUtils.hasText(workUserid)) {
			return new LinkedHashMap<>();
		}
		ShopSalesperson entity = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getWorkUserid, workUserid.trim())
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.last("LIMIT 1"));
		if (entity == null) {
			return new LinkedHashMap<>();
		}

		LinkedHashMap<String, Object> info = new LinkedHashMap<>();
		appendPayloadFieldsBeforeRole(info, entity);
		info.put("role", entity.getRole() != null ? entity.getRole() : "");
		appendPayloadFieldsAfterRole(info, entity);

		List<ShopsRelSalesperson> relRows = shopsRelSalespersonMapper.selectList(
				new LambdaQueryWrapper<ShopsRelSalesperson>()
						.eq(ShopsRelSalesperson::getCompanyId, companyId)
						.eq(ShopsRelSalesperson::getSalespersonId, entity.getSalespersonId())
						.orderByAsc(ShopsRelSalesperson::getShopId));

		List<Long> shopIds = new ArrayList<>();
		List<Long> distributorIds = new ArrayList<>();

		for (ShopsRelSalesperson rel : relRows) {
			String st = rel.getStoreType();
			Long sid = rel.getShopId();
			if (sid == null) {
				continue;
			}
			if ("shop".equals(st)) {
				shopIds.add(sid);
				info.put("store_type", "shop");
			} else if ("distributor".equals(st)) {
				distributorIds.add(sid);
				info.put("store_type", "distributor");
			}
		}

		info.put("shop_ids", new ArrayList<>(shopIds));
		info.put("distributor_ids", new ArrayList<>(distributorIds));

		Long firstDist = distributorIds.isEmpty() ? null : distributorIds.get(0);
		if (firstDist != null) {
			info.put("distributor_id", String.valueOf(firstDist));
		} else {
			info.put("distributor_id", Boolean.FALSE);
		}

		List<Map<String, Object>> easyRows = List.of();
		if (!distributorIds.isEmpty()) {
			easyRows = distributorRepositoryGetInfoSimpleService.listEasylistsByDistributorIds(companyId,
					distributorIds);
		}

		if (firstDist != null) {
			String storeName = "";
			String shopCode = "";
			for (Map<String, Object> row : easyRows) {
				if (row == null) {
					continue;
				}
				if (!distributorIdMatches(row.get("distributor_id"), firstDist)) {
					continue;
				}
				Object n = row.get("name");
				Object sc = row.get("shop_code");
				storeName = n != null ? String.valueOf(n) : "";
				shopCode = sc != null ? String.valueOf(sc) : "";
				break;
			}
			info.put("store_name", storeName);
			info.put("shop_code", shopCode);
		}

		return info;
	}

	public Map<String, Object> getShoppingGuideDetailForH5Bind(long companyId, String workUserid) {
		if (!StringUtils.hasText(workUserid)) {
			return new LinkedHashMap<>();
		}
		ShopSalesperson entity = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getWorkUserid, workUserid.trim())
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.eq(ShopSalesperson::getIsValid, "true")
				.last("LIMIT 1"));
		if (entity == null) {
			return new LinkedHashMap<>();
		}

		long cid = entity.getCompanyId();

		LinkedHashMap<String, Object> info = new LinkedHashMap<>();
		appendPayloadFieldsBeforeRole(info, entity);
		info.put("role", entity.getRole() != null ? entity.getRole() : "");
		appendPayloadFieldsAfterRole(info, entity);

		List<ShopsRelSalesperson> relRows = shopsRelSalespersonMapper.selectList(
				new LambdaQueryWrapper<ShopsRelSalesperson>()
						.eq(ShopsRelSalesperson::getCompanyId, cid)
						.eq(ShopsRelSalesperson::getSalespersonId, entity.getSalespersonId())
						.orderByAsc(ShopsRelSalesperson::getShopId));

		List<Long> shopIds = new ArrayList<>();
		List<Long> distributorIds = new ArrayList<>();

		for (ShopsRelSalesperson rel : relRows) {
			String st = rel.getStoreType();
			Long sid = rel.getShopId();
			if (sid == null) {
				continue;
			}
			if ("shop".equals(st)) {
				shopIds.add(sid);
				info.put("store_type", "shop");
			} else if ("distributor".equals(st)) {
				distributorIds.add(sid);
				info.put("store_type", "distributor");
			}
		}

		info.put("shop_ids", new ArrayList<>(shopIds));
		info.put("distributor_ids", new ArrayList<>(distributorIds));

		Long firstDist = distributorIds.isEmpty() ? null : distributorIds.get(0);
		if (firstDist != null) {
			info.put("distributor_id", String.valueOf(firstDist));
		} else {
			info.put("distributor_id", Boolean.FALSE);
		}

		List<Map<String, Object>> easyRows = List.of();
		if (!distributorIds.isEmpty()) {
			easyRows = distributorRepositoryGetInfoSimpleService.listEasylistsByDistributorIds(cid,
					distributorIds);
		}

		if (firstDist != null) {
			String storeName = "";
			String shopCode = "";
			for (Map<String, Object> row : easyRows) {
				if (row == null) {
					continue;
				}
				if (!distributorIdMatches(row.get("distributor_id"), firstDist)) {
					continue;
				}
				Object n = row.get("name");
				Object sc = row.get("shop_code");
				storeName = n != null ? String.valueOf(n) : "";
				shopCode = sc != null ? String.valueOf(sc) : "";
				break;
			}
			info.put("store_name", storeName);
			info.put("shop_code", shopCode);
		}

		return info;
	}

	public Map<String, Object> getSalespersonDetailForUserSalespersonRelationship(long companyId, long salespersonId) {
		ShopSalesperson entity = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.last("LIMIT 1"));
		if (entity == null) {
			return new LinkedHashMap<>();
		}

		LinkedHashMap<String, Object> info = new LinkedHashMap<>();
		appendPayloadFieldsBeforeRole(info, entity);
		info.put("role", entity.getRole() != null ? entity.getRole() : "");
		appendPayloadFieldsAfterRole(info, entity);

		List<ShopsRelSalesperson> relRows = shopsRelSalespersonMapper.selectList(
				new LambdaQueryWrapper<ShopsRelSalesperson>()
						.eq(ShopsRelSalesperson::getCompanyId, companyId)
						.eq(ShopsRelSalesperson::getSalespersonId, entity.getSalespersonId())
						.orderByAsc(ShopsRelSalesperson::getShopId));

		List<Long> shopIds = new ArrayList<>();
		List<Long> distributorIds = new ArrayList<>();

		for (ShopsRelSalesperson rel : relRows) {
			String st = rel.getStoreType();
			Long sid = rel.getShopId();
			if (sid == null) {
				continue;
			}
			if ("shop".equals(st)) {
				shopIds.add(sid);
				info.put("store_type", "shop");
			} else if ("distributor".equals(st)) {
				distributorIds.add(sid);
				info.put("store_type", "distributor");
			}
		}

		info.put("shop_ids", new ArrayList<>(shopIds));
		info.put("distributor_ids", new ArrayList<>(distributorIds));

		Long firstDist = distributorIds.isEmpty() ? null : distributorIds.get(0);
		if (firstDist != null) {
			info.put("distributor_id", String.valueOf(firstDist));
		} else {
			info.put("distributor_id", Boolean.FALSE);
		}

		List<Map<String, Object>> easyRows = List.of();
		if (!distributorIds.isEmpty()) {
			easyRows = distributorRepositoryGetInfoSimpleService.listEasylistsByDistributorIds(companyId,
					distributorIds);
		}

		if (firstDist != null) {
			String storeName = "";
			String shopCode = "";
			for (Map<String, Object> row : easyRows) {
				if (row == null) {
					continue;
				}
				if (!distributorIdMatches(row.get("distributor_id"), firstDist)) {
					continue;
				}
				Object n = row.get("name");
				Object sc = row.get("shop_code");
				storeName = n != null ? String.valueOf(n) : "";
				shopCode = sc != null ? String.valueOf(sc) : "";
				break;
			}
			info.put("store_name", storeName);
			info.put("shop_code", shopCode);
		}

		return info;
	}

	private static boolean distributorIdMatches(Object raw, long expected) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() == expected;
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim()) == expected;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static Long parseSalespersonId(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void appendPayloadFieldsBeforeRole(LinkedHashMap<String, Object> out, ShopSalesperson row) {
		out.put("salesperson_id", String.valueOf(row.getSalespersonId()));
		out.put("name", row.getName() != null ? sensitiveFieldEncryptor.decrypt(row.getName()) : "");
		out.put("mobile", row.getMobile() != null ? sensitiveFieldEncryptor.decrypt(row.getMobile()) : "");
		out.put("created_time", ShopSalespersonApiFields.createdTimeForSalespersonDetail(row));
		out.put("salesperson_type", row.getSalespersonType() != null ? row.getSalespersonType() : "");
		out.put("company_id", String.valueOf(row.getCompanyId()));
		out.put("user_id", String.valueOf(row.getUserId()));
		out.put("child_count", row.getChildCount() != null ? row.getChildCount() : 0);
		out.put("is_valid", row.getIsValid() != null ? row.getIsValid() : "");
		out.put("shop_id", ShopSalespersonApiFields.shopIdForJson(row.getShopId()));
		out.put("shop_name", row.getShopName());
		out.put("number", row.getNumber() != null ? row.getNumber() : "");
		out.put("friend_count", row.getFriendCount() != null ? row.getFriendCount() : 0);
		out.put("avatar", row.getAvatar());
		out.put("work_userid", row.getWorkUserid());
		out.put("work_configid", row.getWorkConfigid());
		out.put("work_qrcode_configid", row.getWorkQrcodeConfigid());
	}

	private void appendPayloadFieldsAfterRole(LinkedHashMap<String, Object> out, ShopSalesperson row) {
		out.put("salesperson_job", row.getSalespersonJob() != null ? row.getSalespersonJob() : "");
		out.put("employee_status", row.getEmployeeStatus() != null ? row.getEmployeeStatus() : 0);
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		out.put("work_clear_userid", row.getWorkClearUserid());
	}

	private Map<String, Object> wxShopToMap(WxShops e, long nowSec) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("wxShopId", e.getWxShopId());
		out.put("mapPoiId", e.getMapPoiId());
		out.put("storeName", e.getStoreName());
		out.put("poiId", e.getPoiId());
		out.put("lng", e.getLng());
		out.put("lat", e.getLat());
		out.put("address", e.getAddress());
		out.put("category", e.getCategory());
		out.put("contractPhone", e.getContractPhone());
		out.put("hour", e.getHour());
		out.put("addType", e.getAddType());
		out.put("credential", e.getCredential());
		out.put("companyName", e.getCompanyName());
		out.put("qualificationList", e.getQualificationList());
		out.put("cardId", e.getCardId());
		out.put("status", e.getStatus());
		out.put("errmsg", e.getErrmsg());
		out.put("auditId", e.getAuditId());
		out.put("companyId", e.getCompanyId());
		out.put("distributorId", e.getDistributorId());
		out.put("resourceId", e.getResourceId());
		out.put("expiredAt", e.getExpiredAt());
		out.put("country", e.getCountry());
		out.put("city", e.getCity());
		out.put("isDomestic", e.getIsDomestic() != null ? e.getIsDomestic() : 1);
		out.put("isDirectStore", e.getIsDirectStore() != null ? e.getIsDirectStore() : 1);
		boolean open = e.getIsOpen() == null || Boolean.TRUE.equals(e.getIsOpen());
		out.put("isOpen", open ? 1 : 0);
		out.put("isDefault", e.getIsDefault() != null && e.getIsDefault());
		out.put("created", e.getCreated());
		out.put("updated", e.getUpdated());

		Object picParsed = null;
		if (StringUtils.hasText(e.getPicList())) {
			try {
				picParsed = objectMapper.readValue(e.getPicList(), Object.class);
			} catch (Exception ignored) {
				picParsed = null;
			}
		}
		out.put("picList", picParsed);

		Long exp = e.getExpiredAt();
		boolean isValid = !(exp != null && exp < nowSec);
		out.put("is_valid", isValid);

		return out;
	}
}
