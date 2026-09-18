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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.salesperson.domain.SalespersonNotice;
import cn.shopex.ecshopx.salesperson.domain.SalespersonNoticeLog;
import cn.shopex.ecshopx.salesperson.domain.SalespersonRelNotice;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonNoticeLogMapper;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonNoticeMapper;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonRelNoticeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SalespersonNoticeAddService {

	private final SalespersonNoticeMapper salespersonNoticeMapper;
	private final SalespersonNoticeLogMapper salespersonNoticeLogMapper;
	private final SalespersonRelNoticeMapper salespersonRelNoticeMapper;
	private final DistributorMapper distributorMapper;
	private final ObjectMapper objectMapper;

	public SalespersonNoticeAddService(
			SalespersonNoticeMapper salespersonNoticeMapper,
			SalespersonNoticeLogMapper salespersonNoticeLogMapper,
			SalespersonRelNoticeMapper salespersonRelNoticeMapper,
			DistributorMapper distributorMapper,
			ObjectMapper objectMapper) {
		this.salespersonNoticeMapper = salespersonNoticeMapper;
		this.salespersonNoticeLogMapper = salespersonNoticeLogMapper;
		this.salespersonRelNoticeMapper = salespersonRelNoticeMapper;
		this.distributorMapper = distributorMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> addNotice(long companyId, Map<String, Object> mergedInput, Map<String, Object> operatorJwt) {
		if (isShopContextForbidden(mergedInput, operatorJwt)) {
			throw new ResourceException("您没有此操作的权限");
		}

		String title = mergedInput.get("title") == null ? "" : mergedInput.get("title").toString().trim();
		if (title.isEmpty()) {
			throw new BadRequestException("标题不能为空");
		}

		String content = mergedInput.get("content") == null ? "" : mergedInput.get("content").toString().trim();
		if (content.isEmpty()) {
			throw new BadRequestException("内容不能为空");
		}

		SalespersonNotice entity = new SalespersonNotice();
		entity.setCompanyId(companyId);
		entity.setTitle(title);
		entity.setContent(content);
		entity.setStatus(1);
		entity.setNoticeType(2);
		int now = (int) Instant.now().getEpochSecond();
		entity.setCreated(now);
		entity.setUpdated(now);

		int rows = salespersonNoticeMapper.insert(entity);
		if (rows > 0) {
			return Map.of("result", true);
		}
		return Map.of("result", false);
	}

	public void validateUpdateNoticeFields(Map<String, Object> mergedInput) {
		String title = mergedInput.get("title") == null ? "" : mergedInput.get("title").toString().trim();
		if (title.isEmpty()) {
			throw new BadRequestException("标题不能为空", 422);
		}

		String content = mergedInput.get("content") == null ? "" : mergedInput.get("content").toString().trim();
		if (content.isEmpty()) {
			throw new BadRequestException("内容不能为空", 422);
		}
	}

	public Map<String, Object> updateNotice(long companyId, long noticeId, Map<String, Object> mergedInput) {
		validateUpdateNoticeFields(mergedInput);

		String title = mergedInput.get("title") == null ? "" : mergedInput.get("title").toString().trim();
		String content = mergedInput.get("content") == null ? "" : mergedInput.get("content").toString().trim();

		SalespersonNotice patch = new SalespersonNotice();
		patch.setTitle(title);
		patch.setContent(content);
		patch.setCompanyId(companyId);

		int rows = salespersonNoticeMapper.update(
				patch,
				new LambdaUpdateWrapper<SalespersonNotice>()
						.eq(SalespersonNotice::getNoticeId, noticeId)
						.eq(SalespersonNotice::getCompanyId, companyId));
		if (rows > 0) {
			return Map.of("success", true);
		}
		return Map.of("success", false);
	}

	public Map<String, Object> deleteNotice(long companyId, long noticeId) {
		int rows = salespersonNoticeMapper.update(
				null,
				new LambdaUpdateWrapper<SalespersonNotice>()
						.set(SalespersonNotice::getIsDelete, 1)
						.eq(SalespersonNotice::getNoticeId, noticeId)
						.eq(SalespersonNotice::getCompanyId, companyId));
		return rows > 0 ? Map.of("success", true) : Map.of("success", false);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> sendNotice(long companyId, long noticeId, Object distributorIdInput) {
		String normalizedToken;
		if (distributorIdInput instanceof String s && "all".equals(s)) {
			normalizedToken = "all";
		} else if (distributorIdInput instanceof Collection<?> c) {
			if (c.isEmpty()) {
				throw new BadRequestException("请选择店铺", 422);
			}
			List<Long> longList = new ArrayList<>();
			for (Object o : c) {
				Long id = longOrNull(o);
				if (id == null) {
					throw new BadRequestException("请选择店铺", 422);
				}
				longList.add(id);
			}
			try {
				normalizedToken = objectMapper.writeValueAsString(longList);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("请选择店铺", 422);
			}
		} else {
			throw new BadRequestException("请选择店铺", 422);
		}

		int now = (int) Instant.now().getEpochSecond();
		String sendId;
		String allDistributor;
		if ("all".equals(normalizedToken)) {
			LambdaQueryWrapper<Distributor> q = new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.select(Distributor::getDistributorId);
			List<Distributor> rows = distributorMapper.selectList(q);
			List<Long> ids = rows.stream().map(Distributor::getDistributorId).toList();
			try {
				sendId = objectMapper.writeValueAsString(ids);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("请选择店铺", 422);
			}
			allDistributor = "1";
		} else {
			sendId = normalizedToken;
			allDistributor = "0";
		}

		SalespersonNotice patch = new SalespersonNotice();
		patch.setAllDistributor(allDistributor);
		patch.setLastSentTime(now);
		patch.setStatus(2);
		patch.setDistributorId(sendId);
		salespersonNoticeMapper.update(
				patch,
				new LambdaUpdateWrapper<SalespersonNotice>()
						.eq(SalespersonNotice::getNoticeId, noticeId)
						.eq(SalespersonNotice::getCompanyId, companyId));

		JsonNode arr;
		try {
			arr = objectMapper.readTree(sendId);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("请选择店铺", 422);
		}
		if (arr == null || !arr.isArray()) {
			throw new BadRequestException("请选择店铺", 422);
		}

		salespersonNoticeLogMapper.delete(
				new LambdaQueryWrapper<SalespersonNoticeLog>()
						.eq(SalespersonNoticeLog::getNoticeId, noticeId)
						.eq(SalespersonNoticeLog::getCompanyId, companyId));
		salespersonRelNoticeMapper.delete(
				new LambdaQueryWrapper<SalespersonRelNotice>()
						.eq(SalespersonRelNotice::getNoticeId, noticeId)
						.eq(SalespersonRelNotice::getCompanyId, companyId));

		for (JsonNode el : arr) {
			long id;
			if (el.isNumber()) {
				id = el.longValue();
			} else if (el.isTextual()) {
				try {
					id = Long.parseLong(el.asText().trim());
				} catch (NumberFormatException e) {
					throw new BadRequestException("请选择店铺", 422);
				}
			} else {
				throw new BadRequestException("请选择店铺", 422);
			}
			if (id > Integer.MAX_VALUE || id < Integer.MIN_VALUE) {
				throw new BadRequestException("请选择店铺", 422);
			}
			SalespersonNoticeLog row = new SalespersonNoticeLog();
			row.setNoticeId(noticeId);
			row.setDistributorId((int) id);
			row.setCompanyId(companyId);
			row.setCreated(now);
			row.setUpdated(now);
			salespersonNoticeLogMapper.insert(row);
		}

		return Map.of("status", true);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> withdrawNotice(long companyId, long noticeId) {
		SalespersonNotice patch = new SalespersonNotice();
		patch.setDistributorId(",");
		patch.setStatus(3);
		salespersonNoticeMapper.update(
				patch,
				new LambdaUpdateWrapper<SalespersonNotice>()
						.eq(SalespersonNotice::getNoticeId, noticeId)
						.eq(SalespersonNotice::getCompanyId, companyId));
		salespersonNoticeLogMapper.delete(
				new LambdaQueryWrapper<SalespersonNoticeLog>()
						.eq(SalespersonNoticeLog::getNoticeId, noticeId)
						.eq(SalespersonNoticeLog::getCompanyId, companyId));
		salespersonRelNoticeMapper.delete(
				new LambdaQueryWrapper<SalespersonRelNotice>()
						.eq(SalespersonRelNotice::getNoticeId, noticeId)
						.eq(SalespersonRelNotice::getCompanyId, companyId));
		return Map.of("stauts", Boolean.TRUE);
	}

	/**
	 * Lists notices for the company with optional title filter and status equality when {@code statusFilter != 0}.
	 * Results are ordered by {@code notice_id} ascending for stable pagination.
	 */
	public Map<String, Object> getNoticeList(long companyId, String title, int statusFilter, int page, int pageSize) {
		LambdaQueryWrapper<SalespersonNotice> w = new LambdaQueryWrapper<SalespersonNotice>()
				.eq(SalespersonNotice::getCompanyId, companyId)
				.eq(SalespersonNotice::getIsDelete, 0);
		if (StringUtils.hasText(title)) {
			w.like(SalespersonNotice::getTitle, title.trim());
		}
		if (statusFilter != 0) {
			w.eq(SalespersonNotice::getStatus, statusFilter);
		}
		w.orderByAsc(SalespersonNotice::getNoticeId);

		Page<SalespersonNotice> mpPage = new Page<>(page, pageSize);
		Page<SalespersonNotice> result = salespersonNoticeMapper.selectPage(mpPage, w);
		long total = result.getTotal();
		List<SalespersonNotice> rows = result.getRecords();

		List<LinkedHashMap<String, Object>> listMaps = new ArrayList<>();
		for (SalespersonNotice row : rows) {
			listMaps.add(toNoticeListItemMap(row));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", listMaps);
		return out;
	}

	private static LinkedHashMap<String, Object> toNoticeListItemMap(SalespersonNotice row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("notice_id", row.getNoticeId());
		m.put("company_id", row.getCompanyId());
		m.put("title", row.getTitle());
		m.put("content", row.getContent());
		String rawDist = row.getDistributorId();
		if (rawDist == null || rawDist.trim().isEmpty()) {
			m.put("distributor_id", "");
		} else {
			m.put("distributor_id", rawDist.trim());
		}
		m.put("all_distributor", row.getAllDistributor());
		m.put("notice_type", row.getNoticeType());
		m.put("sent_times", row.getSentTimes());
		m.put("is_delete", row.getIsDelete());
		m.put("withdraw", row.getWithdraw());
		m.put("last_sent_time", row.getLastSentTime());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		m.put("status", row.getStatus());
		return m;
	}

	public Object getNoticeDetail(long companyId, long noticeId, int withLog) {
		SalespersonNotice row = salespersonNoticeMapper.selectOne(
				new LambdaQueryWrapper<SalespersonNotice>()
						.eq(SalespersonNotice::getCompanyId, companyId)
						.eq(SalespersonNotice::getNoticeId, noticeId));
		if (row == null) {
			if (withLog == 0) {
				return List.of();
			}
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("distributors", List.of());
			return empty;
		}

		LinkedHashMap<String, Object> detail = new LinkedHashMap<>();
		detail.put("notice_id", row.getNoticeId());
		detail.put("company_id", row.getCompanyId());
		detail.put("title", row.getTitle());
		detail.put("content", row.getContent());
		detail.put("notice_type", row.getNoticeType());
		detail.put("sent_times", row.getSentTimes());
		detail.put("is_delete", row.getIsDelete());
		detail.put("withdraw", row.getWithdraw());
		detail.put("status", row.getStatus());

		String rawDist = row.getDistributorId();
		if (rawDist == null || rawDist.trim().isEmpty()) {
			detail.put("distributor_id", "");
		} else {
			detail.put("distributor_id", rawDist.trim());
		}

		detail.put("all_distributor", row.getAllDistributor());
		detail.put("last_sent_time", row.getLastSentTime());
		detail.put("created", row.getCreated());
		detail.put("updated", row.getUpdated());

		if (withLog == 0) {
			return detail;
		}

		String ad = row.getAllDistributor();
		boolean isAll = false;
		if (ad != null && !ad.isBlank()) {
			try {
				isAll = Integer.parseInt(ad.trim()) == 1;
			} catch (NumberFormatException e) {
				isAll = false;
			}
		}
		if (isAll) {
			detail.put("distributors", "all");
			return detail;
		}

		if (distributorIdsExplicitPresent(row.getDistributorId())) {
			JsonNode node;
			try {
				node = objectMapper.readTree(row.getDistributorId());
			} catch (JsonProcessingException e) {
				throw new ResourceException("店铺数据无效");
			}
			if (node == null || !node.isArray()) {
				throw new ResourceException("店铺数据无效");
			}
			List<Long> ids = new ArrayList<>();
			for (JsonNode el : node) {
				if (el.isNumber()) {
					ids.add(el.longValue());
				} else if (el.isTextual()) {
					Long v = longOrNull(el.asText());
					if (v != null) {
						ids.add(v);
					}
				}
			}
			if (ids.isEmpty()) {
				detail.put("distributors", List.of());
			} else {
				LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<Distributor>()
						.eq(Distributor::getCompanyId, companyId)
						.in(Distributor::getDistributorId, ids)
						.select(Distributor::getDistributorId, Distributor::getName);
				List<Distributor> list = distributorMapper.selectList(w);
				Map<Long, String> nameById = new HashMap<>();
				for (Distributor d : list) {
					nameById.put(d.getDistributorId(), d.getName());
				}
				List<Map<String, Object>> mappedList = new ArrayList<>();
				for (Long id : ids) {
					LinkedHashMap<String, Object> m = new LinkedHashMap<>();
					m.put("name", nameById.get(id));
					mappedList.add(m);
				}
				detail.put("distributors", mappedList);
			}
		} else {
			detail.put("distributors", List.of());
		}
		return detail;
	}

	private boolean isShopContextForbidden(Map<String, Object> mergedInput, Map<String, Object> operatorJwt) {
		List<Long> jwtIds = parseDistributorIdsFromJwt(operatorJwt.get("distributor_ids"));
		if (!jwtIds.isEmpty()) {
			return true;
		}
		if (distributorIdsExplicitPresent(mergedInput.get("distributorIds"))) {
			return true;
		}
		return distributorIdsExplicitPresent(mergedInput.get("distributor_ids"));
	}

	public static boolean distributorIdsExplicitPresent(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"[]".equals(t);
		}
		if (v instanceof Number n) {
			return n.longValue() != 0;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		return true;
	}

	private List<Long> parseDistributorIdsFromJwt(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				if (o instanceof Map<?, ?> mm) {
					Long v = longOrNull(mm.get("distributor_id"));
					if (v != null) {
						out.add(v);
					}
				} else {
					Long v = longOrNull(o);
					if (v != null) {
						out.add(v);
					}
				}
			}
			return out;
		}
		if (raw instanceof String s && s.trim().startsWith("[")) {
			try {
				JsonNode node = objectMapper.readTree(s);
				if (node.isArray()) {
					List<Long> out = new ArrayList<>();
					for (JsonNode n : node) {
						if (n.isObject()) {
							JsonNode idNode = n.get("distributor_id");
							if (idNode != null && idNode.isNumber()) {
								out.add(idNode.longValue());
							} else if (idNode != null && idNode.isTextual()) {
								Long v = longOrNull(idNode.asText());
								if (v != null) {
									out.add(v);
								}
							}
						} else if (n.isNumber()) {
							out.add(n.longValue());
						} else if (n.isTextual()) {
							Long v = longOrNull(n.asText());
							if (v != null) {
								out.add(v);
							}
						}
					}
					return out;
				}
			} catch (Exception ignored) {
				return List.of();
			}
		}
		Long single = longOrNull(raw);
		if (single != null) {
			return List.of(single);
		}
		return List.of();
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
