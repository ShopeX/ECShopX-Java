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

package cn.shopex.ecshopx.point.service;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.point.domain.PointMemberLog;
import cn.shopex.ecshopx.point.mapper.PointMemberLogMapper;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmPointDetailListPort;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmPointDetailListRequest;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmPointDetailListResult;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointMemberListService {

	private final PointMemberLogMapper pointMemberLogMapper;
	private final MemberAccountService memberAccountService;
	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final DmCrmPointDetailListPort dmCrmPointDetailListPort;

	public PointMemberListService(
			PointMemberLogMapper pointMemberLogMapper,
			MemberAccountService memberAccountService,
			DmCrmSettingReadPort dmCrmSettingReadPort,
			DmCrmPointDetailListPort dmCrmPointDetailListPort) {
		this.pointMemberLogMapper = pointMemberLogMapper;
		this.memberAccountService = memberAccountService;
		this.dmCrmSettingReadPort = dmCrmSettingReadPort;
		this.dmCrmPointDetailListPort = dmCrmPointDetailListPort;
	}

	/**
	 * H5 单会员积分明细：按对接开关走 Hope 分页或本地 {@code point_member_log}；不补充会员摘要字段。
	 */
	public Map<String, Object> lists(long companyId, long userId, int pageNo, int pageSize, String outinType) {
		if (!dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
			return listsBranchLocalFront(companyId, userId, pageNo, pageSize, outinType);
		}
		return listsBranchHopeFront(companyId, userId, pageNo, pageSize, outinType);
	}

	public Map<String, Object> lists(
			long companyId,
			int page,
			int pageSize,
			long userIdParam,
			String mobile,
			String username,
			String name,
			Long dateBegin,
			Long dateEnd) {
		List<Long> filterUserIds = resolveFilterUserIds(companyId, userIdParam, mobile, username, name);
		LambdaQueryWrapper<PointMemberLog> baseLogFilter = buildLogWrapper(companyId, filterUserIds, dateBegin, dateEnd);

		if (dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
			return listsBranchHope(companyId, page, pageSize, filterUserIds, baseLogFilter);
		}
		return listsBranchLocal(companyId, page, pageSize, baseLogFilter);
	}

	private Map<String, Object> listsBranchLocalFront(
			long companyId, long userId, int pageNo, int pageSize, String outinType) {
		LambdaQueryWrapper<PointMemberLog> w = new LambdaQueryWrapper<>();
		w.eq(PointMemberLog::getCompanyId, companyId);
		w.eq(PointMemberLog::getUserId, userId);
		if (StringUtils.hasText(outinType)) {
			String ot = outinType.trim();
			if ("outcome".equals(ot)) {
				w.gt(PointMemberLog::getOutcome, 0);
			} else {
				w.gt(PointMemberLog::getIncome, 0);
			}
		}
		w.orderByDesc(PointMemberLog::getCreated);
		Page<PointMemberLog> p = new Page<>(pageNo, pageSize, false);
		pointMemberLogMapper.selectPage(p, w);
		long total = pointMemberLogMapper.selectCount(w);
		List<Map<String, Object>> list = new ArrayList<>();
		for (PointMemberLog log : p.getRecords()) {
			list.add(mapLocalLogRow(log));
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		return result;
	}

	private Map<String, Object> listsBranchHopeFront(
			long companyId, long userId, int pageNo, int pageSize, String outinType) {
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		Object mobileRaw = memberInfo != null ? memberInfo.get("mobile") : null;
		String mobileForApi = mobileRaw != null ? String.valueOf(mobileRaw) : "";
		DmCrmPointDetailListRequest req = DmCrmPointDetailListRequest.builder()
				.userId(userId)
				.mobile(mobileForApi)
				.currentPage(pageNo)
				.pageSize(pageSize)
				.cardNo(null)
				.build();
		DmCrmPointDetailListResult block = dmCrmPointDetailListPort.fetchDetailList(companyId, req);
		List<Map<String, Object>> list = new ArrayList<>(block.getItems() != null ? block.getItems() : List.of());
		long total = block.getTotalCount();

		if (!list.isEmpty() && StringUtils.hasText(outinType)) {
			String ot = outinType.trim();
			if ("outcome".equals(ot)) {
				List<Map<String, Object>> filtered = new ArrayList<>();
				for (Map<String, Object> row : list) {
					if (intFromMap(row, "outcome") > 0) {
						filtered.add(row);
					}
				}
				list = filtered;
			} else if ("income".equals(ot)) {
				List<Map<String, Object>> filtered = new ArrayList<>();
				for (Map<String, Object> row : list) {
					if (intFromMap(row, "income") > 0) {
						filtered.add(row);
					}
				}
				list = filtered;
			}
		}

		long now = Instant.now().getEpochSecond();
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> v : list) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>(v);
			Object etRaw = row.remove("effect_time");
			Long effectSec = null;
			if (etRaw instanceof Number n) {
				long s = n.longValue();
				if (s > 0) {
					effectSec = s;
					row.put("effectTime", s);
				}
			}
			if (effectSec != null) {
				if (now < effectSec) {
					continue;
				}
				row.put("created", effectSec);
			}
			row.put("updated", "");
			out.add(row);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", out);
		return result;
	}

	private static int intFromMap(Map<String, Object> row, String key) {
		Object o = row.get(key);
		if (o instanceof Number n) {
			return n.intValue();
		}
		return 0;
	}

	private Map<String, Object> listsBranchLocal(
			long companyId, int page, int pageSize, LambdaQueryWrapper<PointMemberLog> w) {
		Page<PointMemberLog> p = new Page<>(page, pageSize, false);
		pointMemberLogMapper.selectPage(p, w);
		long total = pointMemberLogMapper.selectCount(w);
		List<Map<String, Object>> list = new ArrayList<>();
		for (PointMemberLog log : p.getRecords()) {
			list.add(mapLocalLogRow(log));
		}
		enrichListWithMemberSummaries(list, companyId);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		return result;
	}

	private Map<String, Object> listsBranchHope(
			long companyId,
			int page,
			int pageSize,
			List<Long> filterUserIds,
			LambdaQueryWrapper<PointMemberLog> baseLogFilter) {
		List<Long> dmUserIds;
		if (filterUserIds != null) {
			dmUserIds = filterUserIds;
		} else {
			Page<PointMemberLog> sample = new Page<>(page, pageSize);
			sample.setOptimizeCountSql(true);
			pointMemberLogMapper.selectPage(sample, baseLogFilter);
			Set<Long> uniq = new LinkedHashSet<>();
			for (PointMemberLog r : sample.getRecords()) {
				if (r.getUserId() != null) {
					uniq.add(r.getUserId());
				}
			}
			dmUserIds = new ArrayList<>(uniq);
		}

		List<Map<String, Object>> mergedItems = new ArrayList<>();
		long pointTotal = 0L;
		int n = Math.max(1, dmUserIds.size());
		int perPageSize = (int) Math.ceil((double) pageSize / (double) n);

		for (Long userId : dmUserIds) {
			Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
			if (memberInfo == null || memberInfo.isEmpty()) {
				continue;
			}
			Object mobileRaw = memberInfo.get("mobile");
			String mobileStr = mobileRaw != null ? String.valueOf(mobileRaw) : "";
			DmCrmPointDetailListRequest req = DmCrmPointDetailListRequest.builder()
					.userId(userId)
					.mobile(mobileStr)
					.currentPage(page)
					.pageSize(perPageSize)
					.cardNo(null)
					.build();
			DmCrmPointDetailListResult block = dmCrmPointDetailListPort.fetchDetailList(companyId, req);
			pointTotal += block.getTotalCount();
			mergedItems.addAll(block.getItems());
		}

		enrichListWithMemberSummaries(mergedItems, companyId);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", pointTotal);
		result.put("list", mergedItems);
		return result;
	}

	private List<Long> resolveFilterUserIds(
			long companyId, long userIdParam, String mobile, String username, String name) {
		List<List<Long>> dimensionLists = new ArrayList<>();
		if (StringUtils.hasText(mobile)) {
			dimensionLists.add(memberAccountService.listUserIdsByCompanyAndMobile(companyId, mobile.trim()));
		}
		if (StringUtils.hasText(username)) {
			dimensionLists.add(memberAccountService.listUserIdsByUsername(companyId, username.trim()));
		}
		if (StringUtils.hasText(name)) {
			dimensionLists.add(memberAccountService.listUserIdsByNameContains(companyId, name.trim()));
		}
		if (!dimensionLists.isEmpty()) {
			Set<Long> inter = new LinkedHashSet<>(dimensionLists.get(0));
			for (int i = 1; i < dimensionLists.size(); i++) {
				inter.retainAll(new LinkedHashSet<>(dimensionLists.get(i)));
			}
			if (inter.isEmpty()) {
				return List.of(0L);
			}
			return new ArrayList<>(inter);
		}
		if (userIdParam > 0) {
			return List.of(userIdParam);
		}
		return null;
	}

	public long countLocalPointMemberLogs(long companyId, long userIdParam, String mobile, String username, String name,
			Long dateBegin, Long dateEnd) {
		List<Long> filterUserIds = resolveFilterUserIds(companyId, userIdParam, mobile, username, name);
		LambdaQueryWrapper<PointMemberLog> w = buildLogWrapperForExport(companyId, filterUserIds, dateBegin, dateEnd);
		return pointMemberLogMapper.selectCount(w);
	}

	public List<PointMemberLog> pageLocalPointMemberLogs(long companyId, long userIdParam, String mobile, String username,
			String name, Long dateBegin, Long dateEnd, int page, int pageSize) {
		List<Long> filterUserIds = resolveFilterUserIds(companyId, userIdParam, mobile, username, name);
		LambdaQueryWrapper<PointMemberLog> w = buildLogWrapperForExport(companyId, filterUserIds, dateBegin, dateEnd);
		Page<PointMemberLog> p = new Page<>(page, pageSize, false);
		pointMemberLogMapper.selectPage(p, w);
		return p.getRecords();
	}

	private LambdaQueryWrapper<PointMemberLog> buildLogWrapper(
			long companyId, List<Long> filterUserIds, Long dateBegin, Long dateEnd) {
		LambdaQueryWrapper<PointMemberLog> w = new LambdaQueryWrapper<>();
		w.eq(PointMemberLog::getCompanyId, companyId);
		if (filterUserIds != null) {
			w.in(PointMemberLog::getUserId, filterUserIds);
		}
		if (dateBegin != null && dateBegin != 0L) {
			w.ge(PointMemberLog::getCreated, dateBegin.intValue());
			if (dateEnd != null) {
				w.le(PointMemberLog::getCreated, dateEnd.intValue());
			} else {
				w.apply("1 = 0");
			}
		}
		w.orderByDesc(PointMemberLog::getCreated);
		return w;
	}

	private LambdaQueryWrapper<PointMemberLog> buildLogWrapperForExport(
			long companyId, List<Long> filterUserIds, Long dateBegin, Long dateEnd) {
		LambdaQueryWrapper<PointMemberLog> w = new LambdaQueryWrapper<>();
		w.eq(PointMemberLog::getCompanyId, companyId);
		if (filterUserIds != null) {
			w.in(PointMemberLog::getUserId, filterUserIds);
		}
		if (dateBegin != null && dateBegin != 0L) {
			w.ge(PointMemberLog::getCreated, dateBegin.intValue());
		}
		if (dateEnd != null && dateEnd != 0L) {
			w.le(PointMemberLog::getCreated, dateEnd.intValue());
		}
		w.orderByDesc(PointMemberLog::getCreated);
		return w;
	}

	private static Map<String, Object> mapLocalLogRow(PointMemberLog log) {
		int income = log.getIncome() != null ? log.getIncome() : 0;
		int outcome = log.getOutcome() != null ? log.getOutcome() : 0;
		String outinType;
		if (income == 0 && outcome == 0) {
			outinType = "in";
		} else if (income == 0) {
			outinType = "out";
		} else {
			outinType = "in";
		}
		int point = income == 0 ? outcome : income;

		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", log.getId());
		row.put("user_id", log.getUserId());
		row.put("company_id", log.getCompanyId());
		row.put("journal_type", log.getJournalType());
		row.put("point_desc", log.getPointDesc() != null ? log.getPointDesc() : "");
		row.put("income", income);
		row.put("outcome", outcome);
		row.put("order_id", log.getOrderId() != null ? log.getOrderId() : "");
		row.put("outin_type", outinType);
		row.put("point", point);
		row.put("created", log.getCreated() != null ? log.getCreated() : 0);
		row.put("updated", log.getUpdated() != null ? log.getUpdated() : 0);
		row.put("operater", log.getOperater() != null ? log.getOperater() : "");
		row.put("operater_remark", log.getOperaterRemark() != null ? log.getOperaterRemark() : "");
		row.put("external_id", log.getExternalId() != null ? log.getExternalId() : "");
		row.put("journal_type_desc", PointMemberJournalTypeDescriptions.forType(log.getJournalType()));
		row.put("s_point", parseSPointFromPointDesc(log.getPointDesc()));
		return row;
	}

	public static int parseSPointFromPointDesc(String pointDesc) {
		if (!StringUtils.hasText(pointDesc)) {
			return 0;
		}
		String[] parts = pointDesc.split("：", -1);
		String last = parts[parts.length - 1];
		try {
			return Integer.parseInt(last.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void enrichListWithMemberSummaries(List<Map<String, Object>> list, long companyId) {
		if (list == null || list.isEmpty()) {
			return;
		}
		List<Long> uids = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Object u = row.get("user_id");
			if (u instanceof Number n) {
				uids.add(n.longValue());
			}
		}
		List<Map<String, Object>> summaries = memberAccountService.listMemberSummariesByUserIds(companyId, uids);
		Map<Long, Map<String, Object>> byId = new HashMap<>();
		for (Map<String, Object> s : summaries) {
			Object id = s.get("user_id");
			if (id instanceof Number n) {
				byId.put(n.longValue(), s);
			}
		}
		for (Map<String, Object> row : list) {
			Object u = row.get("user_id");
			long uid = u instanceof Number n ? n.longValue() : 0L;
			Map<String, Object> s = byId.get(uid);
			row.put("username", s != null && s.get("username") != null ? String.valueOf(s.get("username")) : "");
			row.put("name", s != null && s.get("name") != null ? String.valueOf(s.get("name")) : "");
			row.put("mobile", s != null && s.get("mobile") != null ? String.valueOf(s.get("mobile")) : "");
			if (row.get("s_point") == null) {
				row.put("s_point", 0);
			}
		}
	}
}
