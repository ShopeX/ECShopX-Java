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
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorSalesmanRole;
import cn.shopex.ecshopx.distribution.mapper.DistributorSalesmanRoleMapper;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.members.service.WechatUserListByUserIdsService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.service.support.ShopSalespersonApiFields;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorSalesmanListService {

	private static final Logger log = LoggerFactory.getLogger(DistributorSalesmanListService.class);

	private final DistributorListQueryService distributorListQueryService;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final DistributorSalesmanRoleMapper distributorSalesmanRoleMapper;
	private final WechatUserListByUserIdsService wechatUserListByUserIdsService;
	private final PromoterMapper promoterMapper;
	private final WorkWechatRelMapper workWechatRelMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public DistributorSalesmanListService(DistributorListQueryService distributorListQueryService,
			ShopSalespersonMapper shopSalespersonMapper,
			DistributorSalesmanRoleMapper distributorSalesmanRoleMapper,
			WechatUserListByUserIdsService wechatUserListByUserIdsService,
			PromoterMapper promoterMapper,
			WorkWechatRelMapper workWechatRelMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.distributorListQueryService = distributorListQueryService;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.distributorSalesmanRoleMapper = distributorSalesmanRoleMapper;
		this.wechatUserListByUserIdsService = wechatUserListByUserIdsService;
		this.promoterMapper = promoterMapper;
		this.workWechatRelMapper = workWechatRelMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> getSalesmanList(Map<String, Object> userData, String distributorIdRaw,
			String salesmanNameRaw, String mobileRaw, String isValidRaw, String pageRaw, String pageSizeRaw) {
		long companyId = extractCompanyId(userData);
		long merchantId = extractMerchantId(userData);
		String operatorType = extractOperatorType(userData);

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		List<Long> shopIds = List.of();
		if ("merchant".equals(operatorType)) {
			shopIds = new ArrayList<>(distributorListQueryService.listValidDistributorIdsForMerchantOrdered(
					companyId, merchantId, 10000));
			filter.put("shop_id", shopIds);
		}

		if (distributorIdRaw != null && !distributorIdRaw.trim().isEmpty()) {
			filter.put("shop_id", distributorIdRaw.trim());
		}

		boolean noMerchantShops = "merchant".equals(operatorType) && shopIds.isEmpty();
		boolean hasDistributorOverride = distributorIdRaw != null && !distributorIdRaw.trim().isEmpty();

		if (salesmanNameRaw != null && StringUtils.hasText(salesmanNameRaw.trim())) {
			filter.put("name|contains", salesmanNameRaw.trim());
		}
		if (mobileRaw != null && StringUtils.hasText(mobileRaw.trim())) {
			filter.put("mobile|contains", mobileRaw.trim());
		}
		if (isValidRaw != null && StringUtils.hasText(isValidRaw.trim())) {
			filter.put("is_valid", isValidRaw.trim());
		} else {
			filter.put("is_valid|neq", "delete");
		}
		filter.put("salesperson_type", "shopping_guide");

		String pageStr = pageRaw == null ? "" : pageRaw.trim();
		String pageSizeStr = pageSizeRaw == null ? "" : pageSizeRaw.trim();
		int page = 1;
		if (StringUtils.hasText(pageStr)) {
			try {
				page = Integer.parseInt(pageStr);
			} catch (NumberFormatException ignored) {
				page = 1;
			}
		}
		int pageSize = 0;
		if (StringUtils.hasText(pageSizeStr)) {
			try {
				int parsed = Integer.parseInt(pageSizeStr);
				if (parsed > 0) {
					pageSize = parsed;
				}
			} catch (NumberFormatException ignored) {
				pageSize = 0;
			}
		}

		LambdaQueryWrapper<ShopSalesperson> wrapper = buildMainWrapper(companyId, filter, noMerchantShops,
				hasDistributorOverride);

		Long totalCount = shopSalespersonMapper.selectCount(wrapper);
		List<ShopSalesperson> pageRows;
		if (pageSize > 0) {
			Page<ShopSalesperson> p = new Page<>(page, pageSize, false);
			pageRows = shopSalespersonMapper.selectPage(p, wrapper).getRecords();
		} else {
			pageRows = shopSalespersonMapper.selectList(wrapper);
		}

		if (pageRows.isEmpty()) {
			log.info("getSalesmanList companyId={} filterKeys={} totalCount=0 listSize=0", companyId,
					filter.keySet());
			return buildBody(0L, List.of(), filter, shopIds);
		}

		for (ShopSalesperson sp : pageRows) {
			if (sp.getName() != null) {
				sp.setName(sensitiveFieldEncryptor.decrypt(sp.getName()));
			}
			if (sp.getMobile() != null) {
				sp.setMobile(sensitiveFieldEncryptor.decrypt(sp.getMobile()));
			}
		}

		Map<Long, String> roleIdToName = loadRoleNames(companyId);

		Set<Long> distinctUserIds = new LinkedHashSet<>();
		for (ShopSalesperson sp : pageRows) {
			Integer uid = sp.getUserId();
			if (uid != null && uid != 0) {
				distinctUserIds.add(uid.longValue());
			}
		}
		wechatUserListByUserIdsService.mapByUserId(companyId, distinctUserIds);

		Map<Long, Promoter> promoterByUserId = new LinkedHashMap<>();
		Map<Long, Integer> childrenCountByPromoterId = Collections.emptyMap();
		if (!distinctUserIds.isEmpty()) {
			LambdaQueryWrapper<Promoter> pw = new LambdaQueryWrapper<>();
			pw.eq(Promoter::getCompanyId, companyId).eq(Promoter::getIsPromoter, 1)
					.in(Promoter::getUserId, distinctUserIds);
			List<Promoter> plist = promoterMapper.selectList(pw);
			Map<String, Object> promoterQueryResult = Map.of("list", plist);
			List<?> promoRows = extractPromoterRows(promoterQueryResult);
			for (Object o : promoRows) {
				if (o instanceof Promoter pr) {
					if (pr.getUserId() != null) {
						promoterByUserId.put(pr.getUserId(), pr);
					}
				}
			}
			List<Long> pidList = new ArrayList<>();
			for (Object o : promoRows) {
				if (o instanceof Promoter pr && pr.getId() != null) {
					pidList.add(pr.getId());
				}
			}
			if (!pidList.isEmpty()) {
				childrenCountByPromoterId = toChildrenCountMap(
						promoterMapper.countDirectChildrenByPidList(companyId, pidList));
			}
		}

		Set<Long> shopIdNums = new LinkedHashSet<>();
		for (ShopSalesperson sp : pageRows) {
			Long sid = ShopSalespersonApiFields.shopIdAsLongOrNull(sp.getShopId());
			if (sid != null) {
				shopIdNums.add(sid);
			}
		}
		Map<Long, Map<String, Object>> storeByDistributorId = loadStoreInfo(companyId, shopIdNums);

		List<Long> salespersonIds = pageRows.stream().map(ShopSalesperson::getSalespersonId).filter(Objects::nonNull)
				.toList();
		Map<Long, Long> workWechatCountBySalespersonId = salespersonIds.isEmpty() ? Map.of()
				: toWorkWechatCountMap(workWechatRelMapper.countRowsGroupBySalespersonId(salespersonIds));

		List<Map<String, Object>> listOut = new ArrayList<>();
		for (ShopSalesperson sp : pageRows) {
			listOut.add(assembleRow(sp, roleIdToName, promoterByUserId, childrenCountByPromoterId,
					storeByDistributorId, workWechatCountBySalespersonId));
		}

		log.info("getSalesmanList companyId={} filterKeys={} totalCount={} listSize={}", companyId, filter.keySet(),
				totalCount, listOut.size());

		return buildBody(totalCount, listOut, filter, shopIds);
	}

	private static Map<String, Object> buildBody(long totalCount, List<Map<String, Object>> list,
			Map<String, Object> filter, List<Long> shopIds) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", totalCount);
		body.put("list", list);
		body.put("filter", filter);
		body.put("shopIds", shopIds);
		return body;
	}

	private Map<String, Object> assembleRow(ShopSalesperson sp, Map<Long, String> roleIdToName,
			Map<Long, Promoter> promoterByUserId, Map<Long, Integer> childrenCountByPromoterId,
			Map<Long, Map<String, Object>> storeByDistributorId, Map<Long, Long> workWechatCountBySalespersonId) {
		String namePlain = sp.getName() == null ? "" : sp.getName();
		String mobilePlain = sp.getMobile() == null ? "" : sp.getMobile();
		long userIdLong = sp.getUserId() == null ? 0L : sp.getUserId().longValue();

		Promoter promoter = promoterByUserId.get(userIdLong);
		long promoterIdOut = 0L;
		int childrenCount = 0;
		if (promoter != null && promoter.getId() != null) {
			promoterIdOut = promoter.getId();
			childrenCount = childrenCountByPromoterId.getOrDefault(promoter.getId(), 0);
		}

		Long shopNum = ShopSalespersonApiFields.shopIdAsLongOrNull(sp.getShopId());
		Map<String, Object> storeInfo = Map.of("name", "");
		if (shopNum != null) {
			storeInfo = storeByDistributorId.getOrDefault(shopNum, Map.of("name", ""));
		}

		String roleKey = sp.getRole() == null ? "" : sp.getRole().trim();
		String roleName = "无角色";
		if (StringUtils.hasText(roleKey)) {
			try {
				long rid = Long.parseLong(roleKey);
				roleName = roleIdToName.getOrDefault(rid, "无角色");
			} catch (NumberFormatException ignored) {
				roleName = "无角色";
			}
		}

		long childCount = sp.getSalespersonId() == null ? 0L
				: workWechatCountBySalespersonId.getOrDefault(sp.getSalespersonId(), 0L);

		Map<String, Object> row = new LinkedHashMap<>();
		putSalespersonFields(row, sp, namePlain, mobilePlain);
		row.put("salesman_name", namePlain);
		row.put("child_count", (int) childCount);
		row.put("children_count", childrenCount);
		row.put("promoter_id", promoterIdOut);
		row.put("salespersonId", sp.getSalespersonId());
		row.put("companyId", sp.getCompanyId());
		row.put("storeInfo", storeInfo);
		row.put("createdTime", sp.getCreatedTime());
		row.put("role_name", roleName);
		row.put("salespersonType", sp.getSalespersonType());
		return row;
	}

	private static void putSalespersonFields(Map<String, Object> row, ShopSalesperson sp, String namePlain,
			String mobilePlain) {
		row.put("salesperson_id", sp.getSalespersonId());
		row.put("company_id", sp.getCompanyId());
		row.put("shop_id", ShopSalespersonApiFields.shopIdForJson(sp.getShopId()));
		row.put("shop_name", sp.getShopName());
		row.put("name", namePlain);
		row.put("mobile", mobilePlain);
		row.put("salesperson_type", sp.getSalespersonType());
		row.put("created_time", sp.getCreatedTime());
		row.put("user_id", sp.getUserId() == null ? 0 : sp.getUserId());
		row.put("is_valid", sp.getIsValid());
		row.put("role", sp.getRole());
		row.put("number", sp.getNumber());
		row.put("friend_count", sp.getFriendCount());
		row.put("avatar", sp.getAvatar());
		row.put("work_userid", sp.getWorkUserid());
		row.put("work_clear_userid", sp.getWorkClearUserid());
		row.put("work_configid", sp.getWorkConfigid());
		row.put("work_qrcode_configid", sp.getWorkQrcodeConfigid());
		row.put("salesperson_job", sp.getSalespersonJob());
		row.put("employee_status", sp.getEmployeeStatus());
		row.put("created", sp.getCreated());
		row.put("updated", sp.getUpdated());
	}

	private Map<Long, String> loadRoleNames(long companyId) {
		LambdaQueryWrapper<DistributorSalesmanRole> rw = new LambdaQueryWrapper<>();
		rw.eq(DistributorSalesmanRole::getCompanyId, (int) companyId);
		List<DistributorSalesmanRole> roles = distributorSalesmanRoleMapper.selectList(rw);
		Map<Long, String> map = new LinkedHashMap<>();
		for (DistributorSalesmanRole r : roles) {
			if (r.getSalesmanRoleId() != null && r.getRoleName() != null) {
				map.put(r.getSalesmanRoleId(), r.getRoleName());
			}
		}
		return map;
	}

	private Map<Long, Map<String, Object>> loadStoreInfo(long companyId, Set<Long> shopIdNums) {
		if (shopIdNums.isEmpty()) {
			return Map.of();
		}
		List<Distributor> dists = distributorListQueryService.listByIdsAndCompany(companyId,
				new ArrayList<>(shopIdNums));
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Distributor d : dists) {
			if (d.getDistributorId() == null) {
				continue;
			}
			out.put(d.getDistributorId(), toStoreInfoRow(d));
		}
		return out;
	}

	private Map<String, Object> toStoreInfoRow(Distributor d) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("address", d.getAddress() != null ? d.getAddress() : "");
		m.put("name", d.getName() != null ? d.getName() : "");
		m.put("shop_code", d.getShopCode() != null ? d.getShopCode() : "");
		String mobileRaw = d.getMobile() != null ? d.getMobile() : "";
		m.put("mobile", sensitiveFieldEncryptor.decrypt(mobileRaw));
		String contactRaw = d.getContact() != null ? d.getContact() : "";
		m.put("contact", sensitiveFieldEncryptor.decrypt(contactRaw));
		m.put("distributor_id", d.getDistributorId());
		m.put("logo", d.getLogo());
		m.put("hour", d.getHour() != null ? d.getHour() : "");
		long parent = d.getShopId() != null ? d.getShopId() : 0L;
		m.put("parent_distributor_id", parent);
		m.put("lng", d.getLng() != null ? d.getLng() : "");
		m.put("lat", d.getLat() != null ? d.getLat() : "");
		int isDistVal = (d.getIsDistributor() == null || Boolean.TRUE.equals(d.getIsDistributor())) ? 1 : 0;
		m.put("is_distributor", isDistVal);
		m.put("is_default", d.getIsDefault() != null ? d.getIsDefault() : 0);
		return m;
	}

	private LambdaQueryWrapper<ShopSalesperson> buildMainWrapper(long companyId, Map<String, Object> filter,
			boolean noMerchantShops, boolean hasDistributorOverride) {
		LambdaQueryWrapper<ShopSalesperson> w = new LambdaQueryWrapper<>();
		w.eq(ShopSalesperson::getCompanyId, companyId).eq(ShopSalesperson::getSalespersonType, "shopping_guide");
		if (noMerchantShops && !hasDistributorOverride) {
			w.and(x -> x.apply("1 = 0"));
		}
		Object shopIdVal = filter.get("shop_id");
		if (shopIdVal instanceof List<?> list && !list.isEmpty()) {
			List<String> shopStrs = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Number n) {
					shopStrs.add(String.valueOf(n.longValue()));
				} else if (o != null && StringUtils.hasText(String.valueOf(o).trim())) {
					shopStrs.add(String.valueOf(o).trim());
				}
			}
			if (!shopStrs.isEmpty()) {
				w.in(ShopSalesperson::getShopId, shopStrs);
			}
		} else if (shopIdVal instanceof String s && StringUtils.hasText(s)) {
			w.eq(ShopSalesperson::getShopId, s.trim());
		}
		if (filter.containsKey("is_valid")) {
			Object v = filter.get("is_valid");
			w.eq(ShopSalesperson::getIsValid, v == null ? "" : String.valueOf(v));
		} else {
			w.ne(ShopSalesperson::getIsValid, "delete");
		}
		Object nameContains = filter.get("name|contains");
		if (nameContains != null && StringUtils.hasText(String.valueOf(nameContains))) {
			String kw = String.valueOf(nameContains);
			w.like(ShopSalesperson::getName, kw);
		}
		Object mobileContains = filter.get("mobile|contains");
		if (mobileContains != null && StringUtils.hasText(String.valueOf(mobileContains))) {
			String kw = String.valueOf(mobileContains);
			w.like(ShopSalesperson::getMobile, kw);
		}
		return w;
	}

	private static List<?> extractPromoterRows(Map<String, Object> promoterQueryResult) {
		if (promoterQueryResult == null) {
			return Collections.emptyList();
		}
		Object level1 = promoterQueryResult.get("list");
		if (level1 instanceof Map<?, ?> m1) {
			Object level2 = m1.get("list");
			if (level2 instanceof List<?> l2) {
				return l2;
			}
		}
		if (level1 instanceof List<?> l1) {
			return l1;
		}
		return Collections.emptyList();
	}

	private static Map<Long, Integer> toChildrenCountMap(List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, Integer> m = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			Object p = row.get("pid");
			if (p == null) {
				continue;
			}
			long pid = p instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(p));
			Object c = row.get("cnt");
			if (c == null) {
				c = row.get("count");
			}
			int cnt = c instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(c));
			m.put(pid, cnt);
		}
		return m;
	}

	private static Map<Long, Long> toWorkWechatCountMap(List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, Long> m = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			Object sid = row.get("salespersonId");
			if (sid == null) {
				sid = row.get("salesperson_id");
			}
			if (sid == null) {
				continue;
			}
			long spId = sid instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(sid));
			Object c = row.get("cnt");
			if (c == null) {
				c = row.get("count");
			}
			long cnt = c instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(c));
			m.put(spId, cnt);
		}
		return m;
	}

	private static long extractCompanyId(Map<String, Object> userData) {
		Object cid = userData.get("company_id");
		return cid instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(cid));
	}

	private static long extractMerchantId(Map<String, Object> userData) {
		Object mid = userData.get("merchant_id");
		if (mid == null) {
			return 0L;
		}
		return mid instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(mid));
	}

	private static String extractOperatorType(Map<String, Object> userData) {
		Object ot = userData.get("operator_type");
		return ot == null ? "" : String.valueOf(ot);
	}
}
