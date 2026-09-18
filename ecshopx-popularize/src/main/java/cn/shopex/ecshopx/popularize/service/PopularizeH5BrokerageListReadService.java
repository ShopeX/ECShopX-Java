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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeH5BrokerageListReadService {

	private static final DateTimeFormatter PLAN_CLOSE_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static final DateTimeFormatter CREATED_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final BrokerageMapper brokerageMapper;

	private final SalesmanBrokerageLogsQueryService salesmanBrokerageLogsQueryService;

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private final WechatUsersMapper wechatUsersMapper;

	private final MembersAssociationsMapper membersAssociationsMapper;

	private final MembersContactByUserIdsLookupService membersContactByUserIdsLookupService;

	private final ObjectMapper objectMapper;

	public PopularizeH5BrokerageListReadService(
			BrokerageMapper brokerageMapper,
			SalesmanBrokerageLogsQueryService salesmanBrokerageLogsQueryService,
			NamedParameterJdbcTemplate namedParameterJdbcTemplate,
			WechatUsersMapper wechatUsersMapper,
			MembersAssociationsMapper membersAssociationsMapper,
			MembersContactByUserIdsLookupService membersContactByUserIdsLookupService,
			ObjectMapper objectMapper) {
		this.brokerageMapper = brokerageMapper;
		this.salesmanBrokerageLogsQueryService = salesmanBrokerageLogsQueryService;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.wechatUsersMapper = wechatUsersMapper;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.membersContactByUserIdsLookupService = membersContactByUserIdsLookupService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> loadDefaultBrokerageListSegment(
			long companyId,
			long userId,
			String brokerageSource,
			boolean isClose,
			int page,
			int pageSize) {
		LinkedHashMap<String, Object> baseFilter = new LinkedHashMap<>();
		baseFilter.put("company_id", Long.valueOf(companyId));
		baseFilter.put("user_id", Long.valueOf(userId));
		String source = brokerageSource;
		if (source != null && StringUtils.hasText(source.trim())) {
			baseFilter.put("source", source.trim());
		} else {
			baseFilter.put("source", "order");
		}
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>(baseFilter);
		filter.put("is_close", Boolean.valueOf(isClose));
		return fetchDefaultSegment(companyId, filter, page, pageSize);
	}

	public Object getBrokerageList(
			long companyId,
			long userId,
			String brokerageSource,
			String pageRaw,
			String pageSizeRaw,
			String isSalesmanPageRaw,
			String shopNameRaw,
			String mobileRaw,
			String orderIdRaw,
			String closeType) {
		int page = parseLenientPage(pageRaw);
		int pageSize = parseLenientPageSize(pageSizeRaw);
		boolean isSalesman = PopularizeH5BrokerageCountReadService.looksLikeSalesmanPageFlag(isSalesmanPageRaw);

		LinkedHashMap<String, Object> baseFilter = new LinkedHashMap<>();
		baseFilter.put("company_id", Long.valueOf(companyId));
		baseFilter.put("user_id", Long.valueOf(userId));
		String source = brokerageSource;
		if (source != null && StringUtils.hasText(source.trim())) {
			baseFilter.put("source", source.trim());
		} else {
			baseFilter.put("source", "order");
		}
		putTruthyStringFilter(baseFilter, "shopName", shopNameRaw);
		putTruthyStringFilter(baseFilter, "mobile", mobileRaw);
		putTruthyStringFilter(baseFilter, "order_id", orderIdRaw);

		Object dataClose = fetchOneSegment(isSalesman, baseFilter, true, page, pageSize);
		Object dataNoClose = fetchOneSegment(isSalesman, baseFilter, false, page, pageSize);

		if ("close".equals(closeType)) {
			return dataClose;
		}
		if ("noClose".equals(closeType)) {
			return dataNoClose;
		}
		LinkedHashMap<String, Object> both = new LinkedHashMap<>();
		both.put("close", dataClose);
		both.put("noClose", dataNoClose);
		return both;
	}

	private Object fetchOneSegment(
			boolean isSalesman, LinkedHashMap<String, Object> baseFilter, boolean isClose, int page, int pageSize) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>(baseFilter);
		filter.put("is_close", Boolean.valueOf(isClose));
		if (!isSalesman) {
			return fetchDefaultSegment(companyIdFromFilter(filter), filter, page, pageSize);
		}
		return fetchSalesmanSegment(companyIdFromFilter(filter), userIdFromFilter(filter), filter, page, pageSize);
	}

	private static long companyIdFromFilter(Map<String, Object> filter) {
		Object v = filter.get("company_id");
		if (v instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}

	private static long userIdFromFilter(Map<String, Object> filter) {
		Object v = filter.get("user_id");
		if (v instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}

	private Map<String, Object> fetchDefaultSegment(long companyId, Map<String, Object> filter, int page, int pageSize) {
		String source = String.valueOf(filter.get("source"));
		Boolean isClose = (Boolean) filter.get("is_close");
		LambdaQueryWrapper<Brokerage> w =
				new LambdaQueryWrapper<Brokerage>()
						.eq(Brokerage::getCompanyId, companyId)
						.eq(Brokerage::getUserId, filter.get("user_id"))
						.eq(Brokerage::getSource, source)
						.eq(Brokerage::getIsClose, isClose)
						.orderByDesc(Brokerage::getCreated);
		Page<Brokerage> pg = new Page<>(page, pageSize);
		brokerageMapper.selectPage(pg, w);
		long totalCount = pg.getTotal();
		List<Map<String, Object>> list = new ArrayList<>();
		for (Brokerage e : pg.getRecords()) {
			list.add(toH5DefaultRow(e));
		}
		if (totalCount > 0L && !list.isEmpty()) {
			applyBuyerContactsMaskingAndCreatedDate(companyId, pageSize, list);
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", Long.valueOf(totalCount));
		out.put("list", list);
		return out;
	}

	private Object fetchSalesmanSegment(
			long companyId, long userId, LinkedHashMap<String, Object> filter, int page, int pageSize) {
		List<Long> dIds = loadSalespersonShopIdsLimit500(companyId, userId);
		if (dIds.isEmpty()) {
			return Collections.emptyList();
		}
		if (filter.containsKey("shopName")) {
			String shopName = String.valueOf(filter.get("shopName")).trim();
			List<Long> narrowed = queryDistributorIdsByShopNameLike(companyId, shopName, dIds);
			if (narrowed.isEmpty()) {
				return Collections.emptyList();
			}
			dIds = narrowed;
		}
		LinkedHashMap<String, Object> sqlFilter = new LinkedHashMap<>();
		sqlFilter.put("company_id", Long.valueOf(companyId));
		sqlFilter.put("user_id", Long.valueOf(userId));
		sqlFilter.put("dIds", dIds);
		sqlFilter.put("is_close", filter.get("is_close"));
		if (filter.containsKey("mobile")) {
			sqlFilter.put("mobile", filter.get("mobile"));
		}
		if (filter.containsKey("order_id")) {
			sqlFilter.put("order_id", filter.get("order_id"));
		}

		long total = salesmanBrokerageLogsQueryService.countH5PromoterSalesmanBrokerageLogs(sqlFilter);
		List<Map<String, Object>> list =
				salesmanBrokerageLogsQueryService.selectPageH5PromoterSalesmanBrokerageLogs(sqlFilter, pageSize, page);

		for (Map<String, Object> row : list) {
			Object v = row.get("detail");
			if (v instanceof String s) {
				row.put("detail", parseDetailJson(this.objectMapper, s));
			} else if (v == null) {
				row.put("detail", parseDetailJson(this.objectMapper, null));
			} else {
				row.put("detail", v);
			}
		}

		if (total > 0L && !list.isEmpty()) {
			applyBuyerContactsMaskingAndCreatedDate(companyId, pageSize, list);
		}

		List<Map<String, Object>> spRows = salesmanBrokerageLogsQueryService.enrichListWithSalesperson(companyId, list);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", Long.valueOf(total));
		out.put("list", list);
		out.put("isSalesmanPage", Integer.valueOf(1));
		if (spRows == null || spRows.isEmpty()) {
			out.put("data_alesperson", null);
		} else {
			LinkedHashMap<String, Object> da = new LinkedHashMap<>();
			da.put("list", spRows);
			da.put("total_count", Long.valueOf(spRows.size()));
			out.put("data_alesperson", da);
		}
		return out;
	}

	private List<Long> loadSalespersonShopIdsLimit500(long companyId, long userId) {
		String sql =
				"SELECT shop_id FROM shop_salesperson WHERE company_id = :companyId AND user_id = :userId LIMIT 500";
		MapSqlParameterSource p =
				new MapSqlParameterSource()
						.addValue("companyId", companyId)
						.addValue("userId", (int) userId);
		List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sql, p);
		List<Long> out = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Object sid = row.get("shop_id");
			if (sid == null) {
				continue;
			}
			long v;
			if (sid instanceof Number n) {
				v = n.longValue();
			} else {
				try {
					v = Long.parseLong(String.valueOf(sid).trim());
				} catch (NumberFormatException e) {
					continue;
				}
			}
			if (v > 0L) {
				out.add(v);
			}
		}
		return out;
	}

	private List<Long> queryDistributorIdsByShopNameLike(long companyId, String shopName, List<Long> dIds) {
		String pat = "%" + escapeSqlLike(shopName) + "%";
		String sql =
				"SELECT distributor_id FROM distribution_distributor WHERE company_id = :companyId "
						+ "AND name LIKE :pat ESCAPE '\\\\' AND distributor_id IN (:dIds)";
		MapSqlParameterSource p =
				new MapSqlParameterSource()
						.addValue("companyId", companyId)
						.addValue("pat", pat)
						.addValue("dIds", dIds);
		List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sql, p);
		List<Long> out = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Object did = row.get("distributor_id");
			if (did instanceof Number n) {
				out.add(n.longValue());
			} else if (did != null) {
				try {
					out.add(Long.parseLong(String.valueOf(did).trim()));
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
		}
		return out;
	}

	private static String escapeSqlLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static Object parseDetailJson(ObjectMapper objectMapper, String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return null;
		}
		String s = raw.trim();
		try {
			return objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException first) {
			try {
				return objectMapper.readValue(s, Object.class);
			} catch (JsonProcessingException second) {
				return null;
			}
		}
	}

	private static int parseLenientPage(String pageRaw) {
		if (pageRaw == null || pageRaw.trim().isEmpty()) {
			return 1;
		}
		try {
			return Math.max(1, Integer.parseInt(pageRaw.trim()));
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parseLenientPageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || pageSizeRaw.trim().isEmpty()) {
			return 10;
		}
		try {
			return Math.max(1, Integer.parseInt(pageSizeRaw.trim()));
		} catch (NumberFormatException e) {
			return 10;
		}
	}

	private static void putTruthyStringFilter(LinkedHashMap<String, Object> filter, String key, String raw) {
		if (raw == null) {
			return;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equalsIgnoreCase(t)) {
			return;
		}
		filter.put(key, t);
	}

	private Map<String, Object> toH5DefaultRow(Brokerage e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("brokerage_type", e.getBrokerageType());
		m.put("order_id", e.getOrderId());
		m.put("user_id", e.getUserId());
		m.put("buy_user_id", e.getBuyUserId());
		m.put("source", e.getSource());
		m.put("order_type", e.getOrderType());
		m.put("company_id", e.getCompanyId());
		m.put("price", e.getPrice());
		m.put("is_close", e.getIsClose());
		Integer planCloseTime = e.getPlanCloseTime();
		m.put("plan_close_time", planCloseTime);
		if (planCloseTime == null) {
			m.put("plan_close_date", null);
		} else {
			Instant inst = Instant.ofEpochSecond(planCloseTime.intValue());
			m.put("plan_close_date", PLAN_CLOSE_FMT.format(inst));
		}
		m.put("commission_type", e.getCommissionType());
		m.put("rebate", e.getRebate());
		m.put("rebate_point", e.getRebatePoint());
		m.put("detail", parseDetailJson(this.objectMapper, e.getDetail()));
		m.put("created", e.getCreated());
		return m;
	}

	private void applyBuyerContactsMaskingAndCreatedDate(long companyId, int pageSize, List<Map<String, Object>> rows) {
		Set<Long> seen = new LinkedHashSet<>();
		List<Long> buyUserIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Long uid = extractLongFlexible(row.get("buy_user_id"));
			if (uid != null && uid > 0L && seen.add(uid)) {
				buyUserIds.add(uid);
			}
		}
		if (buyUserIds.isEmpty()) {
			for (Map<String, Object> row : rows) {
				putCreatedDate(row);
			}
			return;
		}
		Map<Long, Map<String, String>> contacts =
				membersContactByUserIdsLookupService.loadDecryptedContactsByUserIds(buyUserIds, pageSize);
		Map<Long, WechatMini> wechat = loadWechatProfilesFirstByUpdated(companyId, buyUserIds);
		for (Map<String, Object> row : rows) {
			Long buyUid = extractLongFlexible(row.get("buy_user_id"));
			String memberUsername = "";
			String memberMobile = "";
			if (buyUid != null && contacts.containsKey(buyUid)) {
				Map<String, String> c = contacts.get(buyUid);
				memberUsername = c.getOrDefault("username", "");
				memberMobile = c.getOrDefault("mobile", "");
			}
			row.put("username", DataMasking.maskTruenameIfBlocked(memberUsername, 1));
			row.put("mobile", DataMasking.maskMobileIfBlocked(memberMobile, 1));
			WechatMini wx = buyUid == null ? null : wechat.get(buyUid);
			row.put(
					"nickname",
					DataMasking.maskTruenameIfBlocked(wx == null ? null : wx.nickname(), 1));
			row.put("headimgurl", wx == null ? null : wx.headimgurl());
			putCreatedDate(row);
		}
	}

	private void putCreatedDate(Map<String, Object> row) {
		Object c = row.get("created");
		if (c == null) {
			row.put("created_date", null);
			return;
		}
		long sec;
		if (c instanceof Number n) {
			sec = n.longValue();
		} else {
			try {
				sec = Long.parseLong(String.valueOf(c).trim());
			} catch (NumberFormatException e) {
				row.put("created_date", null);
				return;
			}
		}
		row.put("created_date", CREATED_FMT.format(Instant.ofEpochSecond(sec)));
	}

	private Map<Long, WechatMini> loadWechatProfilesFirstByUpdated(long companyId, List<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<MembersAssociations> assocs =
				membersAssociationsMapper.selectList(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.in(MembersAssociations::getUserId, userIds)
								.isNotNull(MembersAssociations::getUnionid));
		if (assocs.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, MembersAssociations> bestAssocByUserId = new HashMap<>();
		for (MembersAssociations a : assocs) {
			String uni = a.getUnionid();
			if (uni == null || uni.trim().isEmpty()) {
				continue;
			}
			Long uid = a.getUserId();
			if (uid == null || uid <= 0L) {
				continue;
			}
			MembersAssociations prev = bestAssocByUserId.get(uid);
			if (prev == null || compareMembersAssociationsTupleMax(a, prev) > 0) {
				bestAssocByUserId.put(uid, a);
			}
		}
		Map<Long, String> userIdToUnionid = new LinkedHashMap<>();
		for (Map.Entry<Long, MembersAssociations> e : bestAssocByUserId.entrySet()) {
			String u = e.getValue().getUnionid();
			if (u != null && !u.trim().isEmpty()) {
				userIdToUnionid.put(e.getKey(), u.trim());
			}
		}
		LinkedHashSet<String> distinctUnionidSet = new LinkedHashSet<>();
		for (String u : userIdToUnionid.values()) {
			distinctUnionidSet.add(u);
		}
		List<String> distinctUnionids = new ArrayList<>(distinctUnionidSet);
		if (distinctUnionids.isEmpty()) {
			return Collections.emptyMap();
		}
		List<WechatUsers> wrows =
				wechatUsersMapper.selectList(
						new LambdaQueryWrapper<WechatUsers>()
								.eq(WechatUsers::getCompanyId, companyId)
								.in(WechatUsers::getUnionid, distinctUnionids));
		Map<String, WechatUsers> bestByUnionid = new HashMap<>();
		for (WechatUsers wu : wrows) {
			String uni = wu.getUnionid();
			if (!StringUtils.hasText(uni)) {
				continue;
			}
			String uniKey = uni.trim();
			WechatUsers cur = bestByUnionid.get(uniKey);
			if (cur == null || compareWechatUsersForSameUnionid(wu, cur) > 0) {
				bestByUnionid.put(uniKey, wu);
			}
		}
		LinkedHashMap<Long, WechatMini> out = new LinkedHashMap<>();
		for (Long uid : userIds) {
			if (uid == null || uid <= 0L || out.containsKey(uid)) {
				continue;
			}
			String unionid = userIdToUnionid.get(uid);
			if (!StringUtils.hasText(unionid)) {
				continue;
			}
			WechatUsers wu = bestByUnionid.get(unionid);
			if (wu == null) {
				continue;
			}
			String nickname = wu.getNickname() == null ? "" : wu.getNickname();
			out.put(uid, new WechatMini(nickname, wu.getHeadimgurl()));
		}
		return out;
	}

	private static int compareMembersAssociationsTupleMax(MembersAssociations x, MembersAssociations y) {
		int c = nullToEmpty(x.getUserType()).compareTo(nullToEmpty(y.getUserType()));
		if (c != 0) {
			return c;
		}
		return nullToEmpty(x.getUnionid()).compareTo(nullToEmpty(y.getUnionid()));
	}

	private static int compareWechatUsersForSameUnionid(WechatUsers a, WechatUsers b) {
		int c = compareLongNullLowDesc(a.getUpdated(), b.getUpdated());
		if (c != 0) {
			return c;
		}
		c = compareLongNullLowDesc(a.getCreated(), b.getCreated());
		if (c != 0) {
			return c;
		}
		return wechatAuthOpenTieKey(a).compareTo(wechatAuthOpenTieKey(b));
	}

	private static int compareLongNullLowDesc(Long a, Long b) {
		long la = a == null ? Long.MIN_VALUE : a.longValue();
		long lb = b == null ? Long.MIN_VALUE : b.longValue();
		return Long.compare(la, lb);
	}

	private static String wechatAuthOpenTieKey(WechatUsers w) {
		String app = w.getAuthorizerAppid() == null ? "" : w.getAuthorizerAppid();
		String oid = w.getOpenId() == null ? "" : w.getOpenId();
		return app + '\u0000' + oid;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static Long extractLongFlexible(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String t = String.valueOf(o).trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static record WechatMini(String nickname, String headimgurl) {}
}
