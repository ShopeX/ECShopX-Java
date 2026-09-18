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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.orders.domain.OrderEpidemicRegister;
import cn.shopex.ecshopx.orders.repository.OrderEpidemicRegisterListFilter;
import cn.shopex.ecshopx.orders.repository.OrderEpidemicRegisterQueryRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EpidemicRegisterListService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CREATED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(CN);
	private static final ObjectMapper JSON = new ObjectMapper();

	private final OrderEpidemicRegisterQueryRepository orderEpidemicRegisterQueryRepository;
	private final DistributorMapper distributorMapper;

	public EpidemicRegisterListService(OrderEpidemicRegisterQueryRepository orderEpidemicRegisterQueryRepository,
			DistributorMapper distributorMapper) {
		this.orderEpidemicRegisterQueryRepository = orderEpidemicRegisterQueryRepository;
		this.distributorMapper = distributorMapper;
	}

	public OrderEpidemicRegisterListFilter buildListFilter(long companyId, String operatorType,
			List<Long> jwtDistributorIds, Long distributorIdParam, String orderTimeStart, String orderTimeEnd) {
		return buildListFilter(companyId, operatorType, jwtDistributorIds, distributorIdParam, orderTimeStart,
				orderTimeEnd, true);
	}

	/**
	 * @param strictOrderTimeParsing when both {@code orderTimeStart} and {@code orderTimeEnd} are non-blank, controls
	 *            parse failures: {@code true} rejects with {@link BadRequestException}; {@code false} omits the
	 *            order-time predicate so the query runs without that range (e.g. export paths that should not fail on
	 *            malformed bounds before counting).
	 */
	public OrderEpidemicRegisterListFilter buildListFilter(long companyId, String operatorType,
			List<Long> jwtDistributorIds, Long distributorIdParam, String orderTimeStart, String orderTimeEnd,
			boolean strictOrderTimeParsing) {
		OrderEpidemicRegisterListFilter filter = new OrderEpidemicRegisterListFilter();
		filter.setCompanyId(companyId);
		boolean staff = "staff".equals(operatorType);
		if (staff) {
			if (distributorIdParam != null) {
				filter.setDistributorIdEq(distributorIdParam);
			} else if (jwtDistributorIds != null && !jwtDistributorIds.isEmpty()) {
				filter.setDistributorIdIn(jwtDistributorIds);
			}
		} else {
			if (distributorIdParam != null) {
				filter.setDistributorIdEq(distributorIdParam);
			}
		}
		applyOrderTimeRange(filter, orderTimeStart, orderTimeEnd, strictOrderTimeParsing);
		return filter;
	}

	public Map<String, Object> buildAndList(long companyId, String operatorType, List<Long> jwtDistributorIds,
			Long distributorIdParam, String orderTimeStart, String orderTimeEnd, int page, int pageSize,
			boolean datapassBlock) {
		OrderEpidemicRegisterListFilter filter = buildListFilter(companyId, operatorType, jwtDistributorIds,
				distributorIdParam, orderTimeStart, orderTimeEnd);

		long total = orderEpidemicRegisterQueryRepository.countByFilter(filter);
		Map<String, Object> out = new LinkedHashMap<>();
		if (total == 0L) {
			out.put("total_count", 0L);
			out.put("list", List.of());
			return out;
		}

		List<OrderEpidemicRegister> rows = orderEpidemicRegisterQueryRepository.pageByFilter(filter, page, pageSize);
		Set<Long> distIds = new LinkedHashSet<>();
		for (OrderEpidemicRegister r : rows) {
			if (r.getDistributorId() != null && r.getDistributorId() > 0L) {
				distIds.add(r.getDistributorId());
			}
		}
		Map<Long, String> idToName = new LinkedHashMap<>();
		if (!distIds.isEmpty()) {
			LambdaQueryWrapper<Distributor> dw = new LambdaQueryWrapper<>();
			dw.eq(Distributor::getCompanyId, companyId).in(Distributor::getDistributorId, distIds)
					.select(Distributor::getDistributorId, Distributor::getName);
			List<Distributor> dists = distributorMapper.selectList(dw);
			for (Distributor d : dists) {
				if (d.getDistributorId() != null && StringUtils.hasText(d.getName())) {
					idToName.put(d.getDistributorId(), d.getName());
				}
			}
		}

		List<Map<String, Object>> list = new ArrayList<>(rows.size());
		for (OrderEpidemicRegister r : rows) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", r.getId());
			row.put("order_id", r.getOrderId());
			row.put("user_id", r.getUserId());
			row.put("company_id", r.getCompanyId());
			row.put("distributor_id", r.getDistributorId());
			row.put("name", r.getName());
			row.put("mobile", r.getMobile());
			Integer created = r.getCreated();
			if (created != null) {
				row.put("created", CREATED_FMT.format(Instant.ofEpochSecond(created.intValue())));
			} else {
				row.put("created", null);
			}
			Long did = r.getDistributorId();
			if (did != null) {
				String dn = idToName.get(did);
				if (StringUtils.hasText(dn)) {
					row.put("distributor_name", dn);
				}
			}
			list.add(row);
		}

		if (datapassBlock) {
			for (Map<String, Object> row : list) {
				Object n = row.get("name");
				Object m = row.get("mobile");
				if (n instanceof String ns) {
					row.put("name", DataMasking.maskTruename(ns));
				}
				if (m instanceof String ms) {
					row.put("mobile", DataMasking.maskMobile(ms));
				}
			}
		}

		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	private static void applyOrderTimeRange(OrderEpidemicRegisterListFilter filter, String orderTimeStart,
			String orderTimeEnd, boolean strictParsing) {
		if (!StringUtils.hasText(orderTimeStart) || !StringUtils.hasText(orderTimeEnd)) {
			return;
		}
		Integer gte = parseToEpochSecond(orderTimeStart.trim());
		Integer lte = parseToEpochSecond(orderTimeEnd.trim());
		if (gte == null || lte == null) {
			if (strictParsing) {
				throw new BadRequestException("下单时间参数格式错误");
			}
			return;
		}
		filter.setOrderTimeGte(gte);
		filter.setOrderTimeLte(lte);
	}

	private static Integer parseToEpochSecond(String s) {
		try {
			long v = Long.parseLong(s);
			if (v > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (v < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) v;
		} catch (NumberFormatException ignored) {
			// fall through
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			return (int) ldt.atZone(CN).toEpochSecond();
		} catch (DateTimeParseException e1) {
			try {
				LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
				return (int) ld.atStartOfDay(CN).toEpochSecond();
			} catch (DateTimeParseException e2) {
				return null;
			}
		}
	}

	public static List<Long> parseDistributorIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				if (o instanceof Map<?, ?> m) {
					Object did = m.get("distributor_id");
					Long v = toLong(did);
					if (v != null) {
						out.add(v);
					}
				} else {
					Long v = toLong(o);
					if (v != null) {
						out.add(v);
					}
				}
			}
			return out;
		}
		if (raw instanceof String str) {
			String s = str.trim();
			if (s.isEmpty()) {
				return List.of();
			}
			if (s.startsWith("[")) {
				try {
					JsonNode node = JSON.readTree(s);
					if (node.isArray()) {
						List<Long> out = new ArrayList<>();
						for (JsonNode n : node) {
							if (n.isObject() && n.hasNonNull("distributor_id")) {
								out.add(n.get("distributor_id").asLong());
							} else if (n.isNumber()) {
								out.add(n.asLong());
							} else if (n.isTextual()) {
								Long v = toLong(n.asText());
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
			String[] parts = s.split(",");
			List<Long> out = new ArrayList<>();
			for (String p : parts) {
				Long v = toLong(p.trim());
				if (v != null) {
					out.add(v);
				}
			}
			return out;
		}
		return List.of();
	}

	private static Long toLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
