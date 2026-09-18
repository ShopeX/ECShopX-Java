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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.service.SalesmanBrokerageCountListQueryService;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopSalespersonBrokerageStaticListService {

	private static final Logger log = LoggerFactory.getLogger(ShopSalespersonBrokerageStaticListService.class);

	private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

	private final SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;
	private final MemberAccountService memberAccountService;

	public ShopSalespersonBrokerageStaticListService(
			SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService,
			ShopSalespersonMapper shopSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper,
			MemberAccountService memberAccountService) {
		this.salesmanBrokerageCountListQueryService = salesmanBrokerageCountListQueryService;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> brokagestaticlist(long companyId, Map<String, Object> authClaims,
			Map<String, Object> mergedInput) {
		LinkedHashMap<String, Object> inputData = new LinkedHashMap<>(mergedInput);
		inputData.put("company_id", companyId);

		Object un = inputData.get("username");
		boolean hasUsername = un != null && StringUtils.hasText(un.toString().trim());
		Object mob = inputData.get("mobile");
		boolean hasMobile = mob != null && StringUtils.hasText(mob.toString().trim());

		Map<String, Object> filterSalesperson = null;
		if (hasUsername) {
			filterSalesperson = new LinkedHashMap<>();
			filterSalesperson.put("company_id", companyId);
			String shopEq = resolveShopIdScalarForFilter(inputData.get("distributor_id"));
			if (shopEq != null) {
				filterSalesperson.put("shop_id", shopEq);
			}
			filterSalesperson.put("name", un.toString().trim());
		}
		if (hasMobile) {
			filterSalesperson = new LinkedHashMap<>();
			filterSalesperson.put("company_id", companyId);
			String shopEq = resolveShopIdScalarForFilter(inputData.get("distributor_id"));
			if (shopEq != null) {
				filterSalesperson.put("shop_id", shopEq);
			}
			filterSalesperson.put("mobile", mob.toString().trim());
			inputData.remove("mobile");
		}

		if (filterSalesperson != null) {
			log.info(":salespersonSearch:brokagestaticlist:filter_salesperson:{}", writeJsonSafe(filterSalesperson));
			LambdaQueryWrapper<ShopSalesperson> w = new LambdaQueryWrapper<>();
			w.eq(ShopSalesperson::getCompanyId, companyId);
			Object shopId = filterSalesperson.get("shop_id");
			if (shopId != null) {
				w.eq(ShopSalesperson::getShopId, shopId.toString());
			}
			if (filterSalesperson.containsKey("name")) {
				String plain = Objects.requireNonNull(filterSalesperson.get("name")).toString();
				w.eq(ShopSalesperson::getName, sensitiveFieldEncryptor.encrypt(plain));
			} else if (filterSalesperson.containsKey("mobile")) {
				String plain = Objects.requireNonNull(filterSalesperson.get("mobile")).toString();
				w.eq(ShopSalesperson::getMobile, sensitiveFieldEncryptor.encrypt(plain));
			}
			w.last("LIMIT 1");
			ShopSalesperson entity = shopSalespersonMapper.selectOne(w);
			log.info(":salespersonSearch:brokagestaticlist:salespersonSearch:{}",
					entity == null ? "null" : writeJsonSafe(Map.of("salesperson_id", entity.getSalespersonId(),
							"user_id", entity.getUserId())));
			if (entity == null || entity.getUserId() == null || entity.getUserId() == 0) {
				throw new ResourceException("查询数据不存在");
			}
			inputData.put("user_id", entity.getUserId());
		}

		log.info(":salespersonSearch:brokagestaticlist:inputData:{}", writeJsonSafe(inputData));

		int pageSize = parsePositiveIntDefault(inputData.get("pageSize"), 1000);
		int page = parsePositiveIntDefault(inputData.get("page"), 1);

		List<Map<String, Object>> rows = salesmanBrokerageCountListQueryService.getSalesmanBrokerageCountList(inputData,
				pageSize, page);

		if (!rows.isEmpty()) {
			LinkedHashSet<Long> idSet = new LinkedHashSet<>();
			for (Map<String, Object> row : rows) {
				Long sid = extractSalespersonId(row.get("salesperson_id"));
				if (sid != null) {
					idSet.add(sid);
				}
			}
			if (!idSet.isEmpty()) {
				List<ShopSalesperson> staff = shopSalespersonMapper.selectList(
						new LambdaQueryWrapper<ShopSalesperson>().in(ShopSalesperson::getSalespersonId, idSet));
				Map<Long, ShopSalesperson> byId = new LinkedHashMap<>();
				for (ShopSalesperson sp : staff) {
					byId.put(sp.getSalespersonId(), sp);
				}
				for (Map<String, Object> v : rows) {
					Long sid = extractSalespersonId(v.get("salesperson_id"));
					ShopSalesperson sp = sid == null ? null : byId.get(sid);
					String mobileOut = "-";
					String nameOut = "-";
					if (sp != null) {
						String encM = sp.getMobile();
						if (StringUtils.hasText(encM)) {
							try {
								mobileOut = sensitiveFieldEncryptor.decrypt(encM);
							} catch (RuntimeException e) {
								mobileOut = "-";
							}
						}
						String encN = sp.getName();
						if (StringUtils.hasText(encN)) {
							try {
								nameOut = sensitiveFieldEncryptor.decrypt(encN);
							} catch (RuntimeException e) {
								nameOut = "-";
							}
						}
					}
					v.put("mobile", mobileOut);
					v.put("username", nameOut);
				}
			}
		}

		long memberUserId = parsePositiveUserIdFromClaims(authClaims);
		Map<String, Object> authInfo = memberAccountService.buildH5AuthInfoForResponse(memberUserId, companyId);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", 1);
		result.put("code", 0);
		result.put("data", rows);
		result.put("inputData", inputData);
		result.put("authInfo", authInfo);
		return result;
	}

	private static long parsePositiveUserIdFromClaims(Map<String, Object> claims) {
		Object v = claims.get("user_id");
		if (v == null) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		if (v instanceof Number n) {
			long x = n.longValue();
			if (x <= 0L) {
				throw new BadRequestException("导购更新用户信息错误");
			}
			return x;
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		try {
			long x = Long.parseLong(s);
			if (x <= 0L) {
				throw new BadRequestException("导购更新用户信息错误");
			}
			return x;
		} catch (NumberFormatException e) {
			throw new BadRequestException("导购更新用户信息错误");
		}
	}

	private String writeJsonSafe(Object o) {
		try {
			return objectMapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			return String.valueOf(o);
		}
	}

	private static Long extractSalespersonId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int parsePositiveIntDefault(Object raw, int def) {
		if (raw == null) {
			return def;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			return v > 0 ? v : def;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return def;
		}
		try {
			int v = Integer.parseInt(s);
			return v > 0 ? v : def;
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/**
	 * Scalar {@code distributor_id} for salesperson {@code shop_id} filter (single store only).
	 */
	private static String resolveShopIdScalarForFilter(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new BadRequestException("distributor_id 格式错误");
			}
			return String.valueOf(v);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return null;
			}
			if (t.contains(",")) {
				throw new BadRequestException("distributor_id 格式错误");
			}
			if (!DIGITS_ONLY.matcher(t).matches()) {
				throw new BadRequestException("distributor_id 格式错误");
			}
			try {
				long v = Long.parseLong(t);
				if (v <= 0L) {
					throw new BadRequestException("distributor_id 格式错误");
				}
				return String.valueOf(v);
			} catch (NumberFormatException e) {
				throw new BadRequestException("distributor_id 格式错误");
			}
		}
		throw new BadRequestException("distributor_id 格式错误");
	}
}
