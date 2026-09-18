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

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorSalesmanRole;
import cn.shopex.ecshopx.distribution.mapper.DistributorSalesmanRoleMapper;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SalespersonListService {

	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final DistributorSalesmanRoleMapper distributorSalesmanRoleMapper;
	private final PromoterMapper promoterMapper;
	private final WorkWechatRelMapper workWechatRelMapper;
	private final DistributorListQueryService distributorListQueryService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public SalespersonListService(ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			ShopSalespersonMapper shopSalespersonMapper,
			DistributorSalesmanRoleMapper distributorSalesmanRoleMapper, PromoterMapper promoterMapper,
			WorkWechatRelMapper workWechatRelMapper, DistributorListQueryService distributorListQueryService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.distributorSalesmanRoleMapper = distributorSalesmanRoleMapper;
		this.promoterMapper = promoterMapper;
		this.workWechatRelMapper = workWechatRelMapper;
		this.distributorListQueryService = distributorListQueryService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> lists(long companyId, Map<String, Object> userData, List<Long> shopIdsFromRequest,
			List<Long> distributorIdsFromRequest, String mobile, String salespersonType, String pageRaw,
			String pageSizeRaw, int datapassBlock) {
		List<Long> effectiveShopIds = !shopIdsFromRequest.isEmpty() ? shopIdsFromRequest
				: extractJwtShopIds(userData);
		List<Long> effectiveDistributorIds = !distributorIdsFromRequest.isEmpty() ? distributorIdsFromRequest
				: extractJwtDistributorShopIds(userData);

		int page = parsePage(pageRaw);
		ParsedPageSize pps = parsePageSize(pageSizeRaw);
		int pageSize = pps.pageSize();
		boolean unlimited = pps.unlimited();

		List<Long> distributorBindingIds = null;
		if (!effectiveDistributorIds.isEmpty()) {
			distributorBindingIds = shopsRelSalespersonMapper.selectSalespersonIdsForDistributorBinding(companyId,
					effectiveDistributorIds, page, unlimited ? 0 : pageSize);
			if (distributorBindingIds == null || distributorBindingIds.isEmpty()) {
				return Map.of("total_count", 0L, "list", List.of(), "datapass_block", datapassBlock);
			}
		}

		LambdaQueryWrapper<ShopSalesperson> wrapper = buildWrapper(companyId, effectiveShopIds, mobile,
				salespersonType, distributorBindingIds);

		Long totalCount = shopSalespersonMapper.selectCount(wrapper);
		List<ShopSalesperson> records;
		if (unlimited) {
			records = shopSalespersonMapper.selectList(wrapper);
		} else {
			Page<ShopSalesperson> p = new Page<>(page, pageSize, false);
			records = shopSalespersonMapper.selectPage(p, wrapper).getRecords();
		}

		if (records.isEmpty()) {
			return buildResponse(totalCount, List.of(), datapassBlock);
		}

		for (ShopSalesperson sp : records) {
			if (sp.getName() != null) {
				sp.setName(sensitiveFieldEncryptor.decrypt(sp.getName()));
			}
			if (sp.getMobile() != null) {
				sp.setMobile(sensitiveFieldEncryptor.decrypt(sp.getMobile()));
			}
		}

		List<Map<String, Object>> listOut = enrichSalespersonRows(companyId, records);

		if (datapassBlock != 0) {
			for (Map<String, Object> row : listOut) {
				Object n = row.get("name");
				String nameStr = n == null ? null : (n instanceof String s ? s : String.valueOf(n));
				row.put("name", DataMasking.maskTruename(nameStr));
				Object mo = row.get("mobile");
				String mobileStr = mo == null ? null : (mo instanceof String s2 ? s2 : String.valueOf(mo));
				row.put("mobile", DataMasking.maskMobile(mobileStr));
			}
		}

		return buildResponse(totalCount, listOut, datapassBlock);
	}

	public Map<String, Object> listForH5SalespersonAdmin(long companyId, boolean distributorIdKeyPresent,
			Object distributorIdRaw, String mobileOpt, String usernameOpt, int pageSize) {
		LambdaQueryWrapper<ShopSalesperson> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ShopSalesperson::getCompanyId, companyId);
		if (distributorIdKeyPresent) {
			String s = distributorIdRaw == null ? "" : String.valueOf(distributorIdRaw).trim();
			wrapper.eq(ShopSalesperson::getShopId, s);
		}
		if (mobileOpt != null && StringUtils.hasText(mobileOpt.trim())) {
			String enc = sensitiveFieldEncryptor.encrypt(mobileOpt.trim());
			wrapper.eq(ShopSalesperson::getMobile, enc);
		}
		if (usernameOpt != null && StringUtils.hasText(usernameOpt.trim())) {
			String enc = sensitiveFieldEncryptor.encrypt(usernameOpt.trim());
			wrapper.eq(ShopSalesperson::getName, enc);
		}

		Long totalCount = shopSalespersonMapper.selectCount(wrapper);
		List<ShopSalesperson> records;
		if (pageSize <= 0) {
			records = shopSalespersonMapper.selectList(wrapper);
		} else {
			Page<ShopSalesperson> p = new Page<>(1, pageSize, false);
			records = shopSalespersonMapper.selectPage(p, wrapper).getRecords();
		}

		if (records.isEmpty()) {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("list", List.of());
			body.put("total_count", totalCount);
			return body;
		}

		for (ShopSalesperson sp : records) {
			if (sp.getName() != null) {
				sp.setName(sensitiveFieldEncryptor.decrypt(sp.getName()));
			}
			if (sp.getMobile() != null) {
				sp.setMobile(sensitiveFieldEncryptor.decrypt(sp.getMobile()));
			}
		}
		List<Map<String, Object>> listOut = enrichSalespersonRows(companyId, records);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("list", listOut);
		body.put("total_count", totalCount);
		return body;
	}

	public Map<String, Object> listForSalemanShopList(long companyId, int memberUserId, int pageSize) {
		LambdaQueryWrapper<ShopSalesperson> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ShopSalesperson::getCompanyId, companyId).eq(ShopSalesperson::getUserId, memberUserId);
		Long totalCount = shopSalespersonMapper.selectCount(wrapper);
		List<ShopSalesperson> records;
		if (pageSize > 0) {
			Page<ShopSalesperson> p = new Page<>(1, pageSize, false);
			records = shopSalespersonMapper.selectPage(p, wrapper).getRecords();
		} else {
			records = shopSalespersonMapper.selectList(wrapper);
		}
		if (records.isEmpty()) {
			return Map.of("total_count", 0L, "list", List.of());
		}
		for (ShopSalesperson sp : records) {
			if (sp.getName() != null) {
				sp.setName(sensitiveFieldEncryptor.decrypt(sp.getName()));
			}
			if (sp.getMobile() != null) {
				sp.setMobile(sensitiveFieldEncryptor.decrypt(sp.getMobile()));
			}
		}
		List<Map<String, Object>> listOut = enrichSalespersonRows(companyId, records);
		return buildResponse(totalCount, listOut, 0);
	}

	private List<Map<String, Object>> enrichSalespersonRows(long companyId, List<ShopSalesperson> records) {
		Map<Long, String> roleIdToName = loadRoleNames(companyId);

		Set<Long> distinctUserIds = new LinkedHashSet<>();
		for (ShopSalesperson sp : records) {
			Integer uid = sp.getUserId();
			if (uid != null && uid != 0) {
				distinctUserIds.add(uid.longValue());
			}
		}

		Map<Long, Promoter> promoterByUserId = new LinkedHashMap<>();
		Map<Long, Integer> childrenCountByPromoterId = Collections.emptyMap();
		if (!distinctUserIds.isEmpty()) {
			LambdaQueryWrapper<Promoter> pw = new LambdaQueryWrapper<>();
			pw.eq(Promoter::getCompanyId, companyId).eq(Promoter::getIsPromoter, 1)
					.in(Promoter::getUserId, distinctUserIds);
			List<Promoter> plist = promoterMapper.selectList(pw);
			for (Promoter pr : plist) {
				if (pr.getUserId() != null) {
					promoterByUserId.put(pr.getUserId(), pr);
				}
			}
			List<Long> pidList = new ArrayList<>();
			for (Promoter pr : plist) {
				if (pr.getId() != null) {
					pidList.add(pr.getId());
				}
			}
			if (!pidList.isEmpty()) {
				childrenCountByPromoterId = toChildrenCountMap(
						promoterMapper.countDirectChildrenByPidList(companyId, pidList));
			}
		}

		Set<Long> shopIdNums = new LinkedHashSet<>();
		for (ShopSalesperson sp : records) {
			Long sid = ShopSalespersonApiFields.shopIdAsLongOrNull(sp.getShopId());
			if (sid != null) {
				shopIdNums.add(sid);
			}
		}
		Map<Long, Map<String, Object>> storeByDistributorId = loadStoreInfo(companyId, shopIdNums);

		List<Long> salespersonIds = records.stream().map(ShopSalesperson::getSalespersonId).filter(Objects::nonNull)
				.toList();
		Map<Long, Long> workWechatCountBySalespersonId = salespersonIds.isEmpty() ? Map.of()
				: toWorkWechatCountMap(workWechatRelMapper.countRowsGroupBySalespersonId(salespersonIds));

		List<Map<String, Object>> listOut = new ArrayList<>();
		for (ShopSalesperson sp : records) {
			listOut.add(assembleRow(sp, roleIdToName, promoterByUserId, childrenCountByPromoterId,
					storeByDistributorId, workWechatCountBySalespersonId));
		}
		return listOut;
	}

	private static Map<String, Object> buildResponse(long totalCount, List<Map<String, Object>> listOut,
			int datapassBlock) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", totalCount);
		body.put("list", listOut);
		body.put("datapass_block", datapassBlock);
		return body;
	}

	private LambdaQueryWrapper<ShopSalesperson> buildWrapper(long companyId, List<Long> effectiveShopIds, String mobile,
			String salespersonType, List<Long> distributorBindingIds) {
		LambdaQueryWrapper<ShopSalesperson> w = new LambdaQueryWrapper<>();
		w.eq(ShopSalesperson::getCompanyId, companyId);
		if (distributorBindingIds != null && !distributorBindingIds.isEmpty()) {
			w.in(ShopSalesperson::getSalespersonId, distributorBindingIds);
		}
		if (!effectiveShopIds.isEmpty()) {
			List<String> shopStrs = new ArrayList<>();
			for (Long id : effectiveShopIds) {
				shopStrs.add(String.valueOf(id));
			}
			w.in(ShopSalesperson::getShopId, shopStrs);
		}
		if (StringUtils.hasText(mobile)) {
			String enc = sensitiveFieldEncryptor.encrypt(mobile.trim());
			w.like(ShopSalesperson::getMobile, "%" + enc + "%");
		}
		if (StringUtils.hasText(salespersonType)) {
			w.eq(ShopSalesperson::getSalespersonType, salespersonType.trim());
		}
		return w;
	}

	private Map<Long, String> loadRoleNames(long companyId) {
		LambdaQueryWrapper<DistributorSalesmanRole> rw = new LambdaQueryWrapper<>();
		rw.eq(DistributorSalesmanRole::getCompanyId, (int) companyId)
				.orderByDesc(DistributorSalesmanRole::getSalesmanRoleId).last("LIMIT 2000");
		List<DistributorSalesmanRole> roles = distributorSalesmanRoleMapper.selectList(rw);
		Map<Long, String> map = new LinkedHashMap<>();
		for (DistributorSalesmanRole r : roles) {
			if (r.getSalesmanRoleId() != null && r.getRoleName() != null) {
				map.put(r.getSalesmanRoleId(), r.getRoleName());
			}
		}
		return map;
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

	private static int parsePage(String pageRaw) {
		if (pageRaw == null || !StringUtils.hasText(pageRaw.trim())) {
			return 1;
		}
		try {
			int p = Integer.parseInt(pageRaw.trim());
			return p < 1 ? 1 : p;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private record ParsedPageSize(int pageSize, boolean unlimited) {
	}

	private static ParsedPageSize parsePageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || !StringUtils.hasText(pageSizeRaw.trim())) {
			return new ParsedPageSize(20, false);
		}
		try {
			int ps = Integer.parseInt(pageSizeRaw.trim());
			if (ps <= 0) {
				return new ParsedPageSize(0, true);
			}
			return new ParsedPageSize(ps, false);
		} catch (NumberFormatException e) {
			return new ParsedPageSize(20, false);
		}
	}

	private static List<Long> extractJwtShopIds(Map<String, Object> userData) {
		Object raw = userData.get("shop_ids");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> m) {
				Long id = readShopIdFromClaimEntry(m);
				if (id != null) {
					out.add(id);
				}
			}
		}
		return out;
	}

	private static List<Long> extractJwtDistributorShopIds(Map<String, Object> userData) {
		Object raw = userData.get("distributor_ids");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> m) {
				Long id = readShopIdFromClaimEntry(m);
				if (id != null) {
					out.add(id);
				}
			}
		}
		return out;
	}

	private static Long readShopIdFromClaimEntry(Map<?, ?> m) {
		Object v = m.get("shop_id");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
