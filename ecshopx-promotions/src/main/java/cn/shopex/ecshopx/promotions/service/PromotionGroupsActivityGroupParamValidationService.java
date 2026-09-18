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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionGroupsActivityGroupParamValidationService {

	private static final ZoneId DATETIME_PARSE_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter LOCAL_DATETIME_SECONDS =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final MessageSource messageSource;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final JdbcTemplate jdbcTemplate;

	public PromotionGroupsActivityGroupParamValidationService(
			MessageSource messageSource,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			JdbcTemplate jdbcTemplate) {
		this.messageSource = messageSource;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.jdbcTemplate = jdbcTemplate;
	}

	public void assertActivityNotYetStarted(PromotionGroupsActivity existing, Locale locale) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		Long bt = existing.getBeginTime();
		if (bt == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}
		if (now >= bt) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.activity_started_cannot_edit", null, locale));
		}
	}

	public void assertDuplicateNameForUpdate(
			long companyId, String actNameTrimmed, long excludeGroupsActivityId, Locale locale) {
		long dup =
				promotionGroupsActivityMapper.selectCount(
						new LambdaQueryWrapper<PromotionGroupsActivity>()
								.eq(PromotionGroupsActivity::getCompanyId, companyId)
								.eq(PromotionGroupsActivity::getActName, actNameTrimmed)
								.eq(PromotionGroupsActivity::getDisabled, false)
								.ne(PromotionGroupsActivity::getGroupsActivityId, excludeGroupsActivityId));
		if (dup > 0L) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.activity_name_cannot_same", null, locale));
		}
	}

	public void validateRequestParams(Map<String, Object> params, Locale locale) {
		String actName = Objects.toString(params.get("act_name"), "").trim();
		if (!StringUtils.hasText(actName) || actName.length() > 10) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.groups.activity_name_required_max_10", null, locale));
		}
		if (params.get("limit_buy_num") == null) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.limit_buy_num_invalid", null, locale));
		}
		if (params.get("person_num") == null) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.person_num_invalid", null, locale));
		}
		if (params.get("goods_id") == null) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.goods_id_required", null, locale));
		}
		String pics = Objects.toString(params.get("pics"), "").trim();
		if (!StringUtils.hasText(pics)) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.pics_required", null, locale));
		}
		String shareDesc = Objects.toString(params.get("share_desc"), "").trim();
		if (!StringUtils.hasText(shareDesc)) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.share_desc_required", null, locale));
		}
		if (params.get("store") == null) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.store_invalid", null, locale));
		}
		requireActPriceYuan(params.get("act_price"), locale);
		if (params.get("limit_time") == null) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.limit_time_invalid", null, locale));
		}
	}

	public long requireFlexibleEpochSeconds(Object v, Locale locale) {
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.date_required", null, locale));
		}
		if (v instanceof Boolean || v instanceof Map<?, ?> || v instanceof Iterable<?>) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.date_invalid", null, locale));
		}
		if (v instanceof Number n) {
			long raw = n.longValue();
			if (raw >= 1_000_000_000_000L) {
				raw = raw / 1000;
			}
			return raw;
		}
		if (v instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new BadRequestException(messageSource.getMessage("promotions.groups.date_required", null, locale));
			}
			String t = s.trim();
			if (t.matches("^-?\\d+$")) {
				try {
					long raw = Long.parseLong(t);
					if (raw >= 1_000_000_000_000L) {
						raw = raw / 1000;
					}
					return raw;
				} catch (NumberFormatException e) {
					throw new BadRequestException(messageSource.getMessage("promotions.groups.date_invalid", null, locale));
				}
			}
			try {
				LocalDate localDate = LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
				return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();
			} catch (DateTimeParseException ignored) {
				try {
					LocalDateTime ldt = LocalDateTime.parse(t, LOCAL_DATETIME_SECONDS);
					return ldt.atZone(DATETIME_PARSE_ZONE).toEpochSecond();
				} catch (DateTimeParseException e) {
					throw new BadRequestException(messageSource.getMessage("promotions.groups.date_invalid", null, locale));
				}
			}
		}
		throw new BadRequestException(messageSource.getMessage("promotions.groups.date_invalid", null, locale));
	}

	public BigDecimal requireActPriceYuan(Object v, Locale locale) {
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.act_price_invalid", null, locale));
		}
		try {
			BigDecimal bd = new BigDecimal(String.valueOf(v).trim());
			if (bd.compareTo(new BigDecimal("0.01")) < 0) {
				throw new BadRequestException(messageSource.getMessage("promotions.groups.act_price_invalid", null, locale));
			}
			return bd;
		} catch (NumberFormatException | ArithmeticException e) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.act_price_invalid", null, locale));
		}
	}

	public boolean readTriStateBoolean(Map<String, Object> params, String key, Locale locale) {
		if (!params.containsKey(key)) {
			return true;
		}
		Object v = params.get(key);
		if (v == null) {
			return false;
		}
		if (v instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				return false;
			}
			String t = s.trim();
			if ("true".equalsIgnoreCase(t) || "1".equals(t)) {
				return true;
			}
			if ("false".equalsIgnoreCase(t) || "0".equals(t)) {
				return false;
			}
			throw new BadRequestException(
					messageSource.getMessage("promotions.groups.invalid_boolean_param", new Object[] {key}, locale));
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			long lv = n.longValue();
			if (lv == 1L) {
				return true;
			}
			if (lv == 0L) {
				return false;
			}
		}
		throw new BadRequestException(
				messageSource.getMessage("promotions.groups.invalid_boolean_param", new Object[] {key}, locale));
	}

	public static long readLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	public long readPositiveLongParam(Object v, String messageKey, Locale locale) {
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
		try {
			long n = v instanceof Number num ? num.longValue() : Long.parseLong(String.valueOf(v).trim());
			if (n <= 0L) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
			}
			return n;
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
	}

	public int readIntInRange(Object v, int min, int max, String messageKey, Locale locale) {
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
		try {
			int n = v instanceof Number num ? num.intValue() : Integer.parseInt(String.valueOf(v).trim());
			if (n < min || n > max) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
			}
			return n;
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
	}

	public long readNonNegativeLong(Object v, String messageKey, Locale locale) {
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
		try {
			long n = v instanceof Number num ? num.longValue() : Long.parseLong(String.valueOf(v).trim());
			if (n < 0L) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
			}
			return n;
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
	}

	public List<Long> resolveItemIdsForGroupConflictCheck(long companyId, long goodsIdAsItemOrGoods) {
		List<Long> byGoodsColumn = marketingActivityCatalogAccess.listItemIdsByGoodsId(companyId, goodsIdAsItemOrGoods);
		if (byGoodsColumn == null || byGoodsColumn.isEmpty()) {
			return List.of(goodsIdAsItemOrGoods);
		}
		return byGoodsColumn.stream().filter(Objects::nonNull).distinct().toList();
	}

	public boolean hasAnyNonDeletedDistributor(long companyId) {
		Long c =
				jdbcTemplate.queryForObject(
						"SELECT COUNT(1) FROM distribution_distributor WHERE company_id = ? "
								+ "AND (is_valid IS NULL OR LOWER(TRIM(is_valid)) <> ?)",
						Long.class,
						companyId,
						"delete");
		return c != null && c > 0L;
	}

	public Long findLowPricedDistributorItem(long companyId, long itemId, int actPriceFen) {
		return jdbcTemplate.query(
				"SELECT di.distributor_id FROM distribution_distributor_items di "
						+ "INNER JOIN distribution_distributor d ON d.distributor_id = di.distributor_id AND d.company_id = di.company_id "
						+ "WHERE di.company_id = ? AND di.item_id = ? AND di.price < ? "
						+ "AND IFNULL(di.is_total_store, 1) = 0 "
						+ "AND (d.is_valid IS NULL OR LOWER(TRIM(d.is_valid)) <> ?) "
						+ "ORDER BY di.created DESC LIMIT 1",
				(ResultSetExtractor<Long>) rs -> {
					if (rs.next()) {
						return rs.getLong(1);
					}
					return null;
				},
				companyId,
				itemId,
				(long) actPriceFen,
				"delete");
	}
}
