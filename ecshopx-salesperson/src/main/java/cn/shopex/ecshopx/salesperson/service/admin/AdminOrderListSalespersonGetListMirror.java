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

package cn.shopex.ecshopx.salesperson.service.admin;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorSalesmanRole;
import cn.shopex.ecshopx.distribution.mapper.DistributorSalesmanRoleMapper;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.service.support.ShopSalespersonApiFields;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
public class AdminOrderListSalespersonGetListMirror {

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final DistributorSalesmanRoleMapper distributorSalesmanRoleMapper;
	private final PromoterMapper promoterMapper;
	private final WorkWechatRelMapper workWechatRelMapper;
	private final DistributorListQueryService distributorListQueryService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public AdminOrderListSalespersonGetListMirror(
			ShopSalespersonMapper shopSalespersonMapper,
			DistributorSalesmanRoleMapper distributorSalesmanRoleMapper,
			PromoterMapper promoterMapper,
			WorkWechatRelMapper workWechatRelMapper,
			DistributorListQueryService distributorListQueryService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.distributorSalesmanRoleMapper = distributorSalesmanRoleMapper;
		this.promoterMapper = promoterMapper;
		this.workWechatRelMapper = workWechatRelMapper;
		this.distributorListQueryService = distributorListQueryService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> getSalespersonListForOrderRow(
			long companyId, long promoterUserId, String distributorIdAsShopId) {
		if (promoterUserId <= 0L) {
			return Map.of();
		}
		LambdaQueryWrapper<ShopSalesperson> w = new LambdaQueryWrapper<>();
		w.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getUserId, (int) Math.min(promoterUserId, Integer.MAX_VALUE))
				.eq(ShopSalesperson::getShopId, distributorIdAsShopId == null ? "0" : distributorIdAsShopId);
		ShopSalesperson sp = shopSalespersonMapper.selectOne(w);
		if (sp == null) {
			return Map.of();
		}
		decryptSp(sp);
		List<Map<String, Object>> rows = enrichSalespersonRows(companyId, List.of(sp));
		return rows.isEmpty() ? Map.of() : rows.get(0);
	}

	public Map<Long, Map<String, Object>> mapBySalespersonId(long companyId, List<Long> salespersonIds) {
		if (salespersonIds == null || salespersonIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<ShopSalesperson> records =
				shopSalespersonMapper.selectList(
						new LambdaQueryWrapper<ShopSalesperson>()
								.eq(ShopSalesperson::getCompanyId, companyId)
								.in(ShopSalesperson::getSalespersonId, salespersonIds));
		if (records.isEmpty()) {
			return Collections.emptyMap();
		}
		for (ShopSalesperson sp : records) {
			decryptSp(sp);
		}
		List<Map<String, Object>> rows = enrichSalespersonRows(companyId, records);
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			Object sid = row.get("salesperson_id");
			if (sid == null) {
				continue;
			}
			long id = sid instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(sid));
			out.put(id, row);
		}
		return out;
	}

	private void decryptSp(ShopSalesperson sp) {
		if (sp.getName() != null) {
			sp.setName(sensitiveFieldEncryptor.decrypt(sp.getName()));
		}
		if (sp.getMobile() != null) {
			sp.setMobile(sensitiveFieldEncryptor.decrypt(sp.getMobile()));
		}
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
			pw.eq(Promoter::getCompanyId, companyId).eq(Promoter::getIsPromoter, 1).in(Promoter::getUserId, distinctUserIds);
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
				childrenCountByPromoterId = toChildrenCountMap(promoterMapper.countDirectChildrenByPidList(companyId, pidList));
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
		List<Long> salespersonIds = records.stream().map(ShopSalesperson::getSalespersonId).filter(Objects::nonNull).toList();
		Map<Long, Long> workWechatCountBySalespersonId =
				salespersonIds.isEmpty()
						? Map.of()
						: toWorkWechatCountMap(workWechatRelMapper.countRowsGroupBySalespersonId(salespersonIds));
		List<Map<String, Object>> listOut = new ArrayList<>();
		for (ShopSalesperson sp : records) {
			listOut.add(
					assembleRow(
							sp,
							roleIdToName,
							promoterByUserId,
							childrenCountByPromoterId,
							storeByDistributorId,
							workWechatCountBySalespersonId));
		}
		return listOut;
	}

	private Map<Long, String> loadRoleNames(long companyId) {
		LambdaQueryWrapper<DistributorSalesmanRole> rw = new LambdaQueryWrapper<>();
		rw.eq(DistributorSalesmanRole::getCompanyId, (int) companyId)
				.orderByDesc(DistributorSalesmanRole::getSalesmanRoleId)
				.last("LIMIT 2000");
		List<DistributorSalesmanRole> roles = distributorSalesmanRoleMapper.selectList(rw);
		Map<Long, String> map = new LinkedHashMap<>();
		for (DistributorSalesmanRole r : roles) {
			if (r.getSalesmanRoleId() != null && r.getRoleName() != null) {
				map.put(r.getSalesmanRoleId(), r.getRoleName());
			}
		}
		return map;
	}

	private Map<String, Object> assembleRow(
			ShopSalesperson sp,
			Map<Long, String> roleIdToName,
			Map<Long, Promoter> promoterByUserId,
			Map<Long, Integer> childrenCountByPromoterId,
			Map<Long, Map<String, Object>> storeByDistributorId,
			Map<Long, Long> workWechatCountBySalespersonId) {
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
		long childCount =
				sp.getSalespersonId() == null ? 0L : workWechatCountBySalespersonId.getOrDefault(sp.getSalespersonId(), 0L);
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

	private static void putSalespersonFields(Map<String, Object> row, ShopSalesperson sp, String namePlain, String mobilePlain) {
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
		List<Distributor> dists = distributorListQueryService.listByIdsAndCompany(companyId, new ArrayList<>(shopIdNums));
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
}
