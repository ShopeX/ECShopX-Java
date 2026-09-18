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

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import cn.shopex.ecshopx.promotions.domain.SpecificCrowdDiscount;
import cn.shopex.ecshopx.promotions.domain.SpecificCrowdDiscountRelUser;
import cn.shopex.ecshopx.promotions.mapper.SpecificCrowdDiscountMapper;
import cn.shopex.ecshopx.promotions.mapper.SpecificCrowdDiscountRelUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(rollbackFor = Exception.class)
public class SpecificCrowdDiscountCreateService {

	private final SpecificCrowdDiscountMapper specificCrowdDiscountMapper;
	private final SpecificCrowdDiscountRelUserMapper specificCrowdDiscountRelUserMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final MemberAccountService memberAccountService;
	private final MembersContactByUserIdsLookupService membersContactByUserIdsLookupService;
	private final MessageSource messageSource;

	public SpecificCrowdDiscountCreateService(
			SpecificCrowdDiscountMapper specificCrowdDiscountMapper,
			SpecificCrowdDiscountRelUserMapper specificCrowdDiscountRelUserMapper,
			MemberTagsMapper memberTagsMapper,
			MemberAccountService memberAccountService,
			MembersContactByUserIdsLookupService membersContactByUserIdsLookupService,
			MessageSource messageSource) {
		this.specificCrowdDiscountMapper = specificCrowdDiscountMapper;
		this.specificCrowdDiscountRelUserMapper = specificCrowdDiscountRelUserMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.memberAccountService = memberAccountService;
		this.membersContactByUserIdsLookupService = membersContactByUserIdsLookupService;
		this.messageSource = messageSource;
	}

	private String msg(String code, String zhCnFallback, Locale locale) {
		return messageSource.getMessage(code, null, zhCnFallback, locale);
	}

	public Map<String, Object> createSpecificCrowdDiscount(Map<String, Object> merged, Locale locale) {
		long companyId = readCompanyId(merged);

		int cycleType = parseIntDefault(merged.get("cycle_type"), 1, locale);

		Object rawStart = merged.get("start_time");
		boolean startMissing =
				(rawStart == null)
						|| !StringUtils.hasText(String.valueOf(rawStart).trim())
						|| "0".equals(String.valueOf(rawStart).trim());
		if (cycleType == 2 && startMissing) {
			throw new ResourceException(
					msg(
							"promotions.specific_crowd.cycle_start_end_time_required",
							"指定周期时，开始时间和结束时间必填",
							locale));
		}

		Object sidRaw = merged.get("specific_id");
		if (sidRaw == null || !StringUtils.hasText(String.valueOf(sidRaw).trim())) {
			throw new BadRequestException(
					msg("promotions.specific_crowd.please_select_target_crowd", "请选择针对人群", locale));
		}
		long specificIdLong;
		try {
			specificIdLong =
					sidRaw instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(sidRaw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.specific_crowd.please_select_target_crowd", "请选择针对人群", locale));
		}
		if (specificIdLong <= 0L) {
			throw new BadRequestException(
					msg("promotions.specific_crowd.please_select_target_crowd", "请选择针对人群", locale));
		}

		for (String timeKey : new String[] {"start_time", "end_time"}) {
			if (!merged.containsKey(timeKey)) {
				continue;
			}
			Object tv = merged.get(timeKey);
			if (tv == null || !StringUtils.hasText(String.valueOf(tv).trim())) {
				continue;
			}
			normalizeEpochMaybeMillis(timeKey, tv, merged, locale);
		}

		String specificType = Objects.toString(merged.get("specific_type"), "member_tag").trim();
		if (!StringUtils.hasText(specificType)) {
			specificType = "member_tag";
		}
		merged.put("specific_type", specificType);

		switch (specificType) {
			case "member_tag": {
				LambdaQueryWrapper<MemberTags> w = new LambdaQueryWrapper<>();
				w.eq(MemberTags::getCompanyId, companyId).eq(MemberTags::getTagId, specificIdLong);
				if (memberTagsMapper.selectCount(w) == 0) {
					throw new ResourceException("请选择正确的针对人群");
				}
				break;
			}
			default:
				break;
		}

		Object discountRaw = merged.get("discount");
		if (discountRaw == null || !StringUtils.hasText(String.valueOf(discountRaw).trim())) {
			throw new ResourceException(
					msg("promotions.specific_crowd.discount_invalid", "折扣参数格式不正确", locale));
		}
		long discountLong = parseLongFlexible(discountRaw, locale);
		if (discountLong < 1L || discountLong > 100L) {
			throw new BadRequestException(
					msg(
							"promotions.specific_crowd.discount_percentage_range_error",
							"周期内优惠折扣必须1-100的整数",
							locale));
		}
		int discount = (int) discountLong;

		Object ltm = merged.get("limit_total_money");
		if (ltm == null || !StringUtils.hasText(String.valueOf(ltm).trim())) {
			throw new BadRequestException(
					msg(
							"promotions.specific_crowd.limit_total_money_must_be_numeric",
							"周期内优惠限额必须为数字",
							locale));
		}
		BigDecimal yuan;
		try {
			yuan = new BigDecimal(String.valueOf(ltm).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.specific_crowd.invalid_limit_total_money", "周期内优惠限额格式不正确", locale));
		}
		BigDecimal centsBd = yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.DOWN);
		if (centsBd.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
			throw new ResourceException(
					msg("promotions.specific_crowd.discount_limit_exceeds_max", "优惠限额超出最大值", locale));
		}
		long centsLong = centsBd.longValue();
		merged.put("limit_total_money", (int) centsLong);

		Object idRaw = merged.get("id");
		boolean doUpdate =
				idRaw != null
						&& StringUtils.hasText(String.valueOf(idRaw).trim())
						&& !"0".equals(String.valueOf(idRaw).trim());
		if (doUpdate) {
			long id;
			try {
				id = Long.parseLong(String.valueOf(idRaw).trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("记录ID格式不正确");
			}
			SpecificCrowdDiscount row =
					specificCrowdDiscountMapper.selectOne(
							new LambdaQueryWrapper<SpecificCrowdDiscount>()
									.eq(SpecificCrowdDiscount::getCompanyId, companyId)
									.eq(SpecificCrowdDiscount::getId, id));
			if (row == null) {
				throw new ResourceException(
						msg("promotions.specific_crowd.no_update_data_found", "未查询到更新数据", locale));
			}
			if (merged.containsKey("specific_type")) {
				row.setSpecificType(specificType);
			}
			row.setSpecificId(specificIdLong);
			if (merged.containsKey("cycle_type")) {
				row.setCycleType(cycleType);
			}
			if (merged.containsKey("start_time")) {
				row.setStartTime(toNullableLong(merged.get("start_time")));
			}
			if (merged.containsKey("end_time")) {
				row.setEndTime(toNullableLong(merged.get("end_time")));
			}
			row.setDiscount((long) discount);
			row.setLimitTotalMoney((Integer) merged.get("limit_total_money"));
			if (merged.containsKey("status")) {
				row.setStatus(readStatusLong(merged.get("status")));
			}
			row.setUpdated((int) Instant.now().getEpochSecond());
			specificCrowdDiscountMapper.updateById(row);
			return toColumnMap(row);
		}

		SpecificCrowdDiscount row = new SpecificCrowdDiscount();
		row.setCompanyId(companyId);
		row.setSpecificType(specificType);
		row.setSpecificId(specificIdLong);
		row.setCycleType(cycleType);
		row.setStartTime(toNullableLong(merged.get("start_time")));
		row.setEndTime(toNullableLong(merged.get("end_time")));
		row.setDiscount((long) discount);
		row.setLimitTotalMoney((Integer) merged.get("limit_total_money"));
		if (merged.containsKey("status")) {
			row.setStatus(readStatusLong(merged.get("status")));
		}
		row.setCreated((int) Instant.now().getEpochSecond());
		specificCrowdDiscountMapper.insert(row);
		return toColumnMap(row);
	}

	public Map<String, Object> updateSpecificCrowdDiscount(Map<String, Object> merged, Locale locale) {
		long companyId = readCompanyId(merged);

		int cycleType = parseIntDefault(merged.get("cycle_type"), 1, locale);

		Object rawStart = merged.get("start_time");
		boolean startMissing =
				(rawStart == null)
						|| !StringUtils.hasText(String.valueOf(rawStart).trim())
						|| "0".equals(String.valueOf(rawStart).trim());
		if (cycleType == 2 && startMissing) {
			throw new ResourceException(
					msg(
							"promotions.specific_crowd.cycle_start_end_time_required",
							"指定周期时，开始时间和结束时间必填",
							locale));
		}

		for (String timeKey : new String[] {"start_time", "end_time"}) {
			if (!merged.containsKey(timeKey)) {
				continue;
			}
			Object tv = merged.get(timeKey);
			if (tv == null || !StringUtils.hasText(String.valueOf(tv).trim())) {
				continue;
			}
			normalizeEpochMaybeMillis(timeKey, tv, merged, locale);
		}

		multiplyLimitYuanToCentsForUpdate(merged, locale);

		String specificType = Objects.toString(merged.get("specific_type"), "member_tag").trim();
		if (!StringUtils.hasText(specificType)) {
			specificType = "member_tag";
		}
		merged.put("specific_type", specificType);

		applySpecificCrowdPostValidation(merged, companyId, locale);

		Object idRaw = merged.get("id");
		if (idRaw == null
				|| !StringUtils.hasText(String.valueOf(idRaw).trim())
				|| "0".equals(String.valueOf(idRaw).trim())) {
			throw new BadRequestException("记录ID格式不正确");
		}
		long id;
		try {
			id = Long.parseLong(String.valueOf(idRaw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("记录ID格式不正确");
		}

		SpecificCrowdDiscount row =
				specificCrowdDiscountMapper.selectOne(
						new LambdaQueryWrapper<SpecificCrowdDiscount>()
								.eq(SpecificCrowdDiscount::getCompanyId, companyId)
								.eq(SpecificCrowdDiscount::getId, id));
		if (row == null) {
			throw new ResourceException(
					msg("promotions.specific_crowd.no_update_data_found", "未查询到更新数据", locale));
		}

		if (merged.get("specific_type") != null) {
			row.setSpecificType(specificType);
		}
		if (merged.get("specific_id") != null) {
			Object sidRaw = merged.get("specific_id");
			try {
				long sid =
						sidRaw instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(sidRaw).trim());
				row.setSpecificId(sid);
			} catch (NumberFormatException e) {
				throw new ResourceException(
						msg(
								"promotions.specific_crowd.please_select_correct_crowd",
								"请选择正确的针对人群",
								locale));
			}
		}
		if (merged.get("cycle_type") != null) {
			row.setCycleType(parseIntDefault(merged.get("cycle_type"), 1, locale));
		}
		if (merged.get("start_time") != null) {
			row.setStartTime(toNullableLong(merged.get("start_time")));
		}
		if (merged.get("end_time") != null) {
			row.setEndTime(toNullableLong(merged.get("end_time")));
		}
		if (merged.get("discount") != null) {
			long discountLong = parseLongFlexible(merged.get("discount"), locale);
			row.setDiscount(discountLong);
		}
		if (merged.get("limit_total_money") != null) {
			Object ltm = merged.get("limit_total_money");
			int cents =
					ltm instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(ltm).trim());
			row.setLimitTotalMoney(cents);
		}
		if (merged.get("status") != null) {
			row.setStatus(readStatusLong(merged.get("status")));
		}
		if (merged.containsKey("company_id")) {
			row.setCompanyId(readCompanyId(merged));
		}

		row.setUpdated((int) Instant.now().getEpochSecond());
		specificCrowdDiscountMapper.updateById(row);
		return toColumnMap(row);
	}

	/**
	 * 详情接口：限额展示为「元」整数字符串（scale 0、向零截断），与列表接口两位小数策略不同。
	 */
	public Object getSpecificCrowdDiscountInfo(long companyId, String idRaw) {
		if (idRaw == null || !StringUtils.hasText(idRaw.trim())) {
			return Collections.emptyList();
		}
		long parsedId;
		try {
			parsedId = Long.parseLong(idRaw.trim());
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}

		SpecificCrowdDiscount row =
				specificCrowdDiscountMapper.selectOne(
						new LambdaQueryWrapper<SpecificCrowdDiscount>()
								.eq(SpecificCrowdDiscount::getCompanyId, companyId)
								.eq(SpecificCrowdDiscount::getId, parsedId));
		if (row == null) {
			return Collections.emptyList();
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>(toColumnMap(row));
		result.remove("limit_total_money");
		result.put("start_time", row.getStartTime());
		result.put("end_time", row.getEndTime());

		LambdaQueryWrapper<MemberTags> tw = new LambdaQueryWrapper<>();
		tw.eq(MemberTags::getCompanyId, companyId).eq(MemberTags::getTagId, row.getSpecificId());
		MemberTags tag = memberTagsMapper.selectOne(tw);
		if (tag == null) {
			result.put("specific_name", null);
		} else {
			result.put("specific_name", tag.getTagName());
		}

		ZoneId zone = ZoneId.systemDefault();
		DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("uuuu-MM-dd");
		if (row.getStartTime() == null) {
			result.put("start_date", null);
		} else {
			result.put(
					"start_date",
					Instant.ofEpochSecond(row.getStartTime()).atZone(zone).toLocalDate().format(dayFmt));
		}
		if (row.getEndTime() == null) {
			result.put("end_date", null);
		} else {
			result.put(
					"end_date",
					Instant.ofEpochSecond(row.getEndTime()).atZone(zone).toLocalDate().format(dayFmt));
		}

		Integer limitTotalMoney = row.getLimitTotalMoney();
		if (limitTotalMoney == null) {
			result.put("limit_total_money", null);
		} else {
			BigDecimal cents = BigDecimal.valueOf(limitTotalMoney.intValue());
			BigDecimal yuan = cents.divide(new BigDecimal("100"), 0, RoundingMode.DOWN);
			result.put("limit_total_money", yuan.toPlainString());
		}

		return result;
	}

	public Map<String, Object> getSpecificCrowdDiscountList(
			long companyId,
			String statusRaw,
			String specificIdRaw,
			String pageRaw,
			String pageSizeRaw) {
		int page = parseListIntParam(pageRaw, "页码格式不正确");
		if (page < 1) {
			page = 1;
		}
		int pageSize = parseListIntParam(pageSizeRaw, "每页条数格式不正确");
		if (pageSize < 1) {
			pageSize = 1;
		}

		Page<SpecificCrowdDiscount> mpPage = new Page<>(page, pageSize);
		LambdaQueryWrapper<SpecificCrowdDiscount> w = new LambdaQueryWrapper<>();
		w.eq(SpecificCrowdDiscount::getCompanyId, companyId);
		if (isTruthyRequestQueryString(statusRaw)) {
			w.eq(SpecificCrowdDiscount::getStatus, readStatusLong(statusRaw));
		}
		if (isTruthyRequestQueryString(specificIdRaw)) {
			long sid;
			try {
				sid = Long.parseLong(specificIdRaw.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("定向人群ID格式不正确");
			}
			w.eq(SpecificCrowdDiscount::getSpecificId, sid);
		}
		w.orderByDesc(SpecificCrowdDiscount::getCreated).orderByAsc(SpecificCrowdDiscount::getId);

		IPage<SpecificCrowdDiscount> pageResult = specificCrowdDiscountMapper.selectPage(mpPage, w);
		List<SpecificCrowdDiscount> records = pageResult.getRecords();
		long total = pageResult.getTotal();

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (records.isEmpty()) {
			out.put("list", Collections.emptyList());
			return out;
		}

		List<Long> tagIds =
				records.stream()
						.map(SpecificCrowdDiscount::getSpecificId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();
		Map<Long, String> tagIdToName = Collections.emptyMap();
		if (!tagIds.isEmpty()) {
			LambdaQueryWrapper<MemberTags> tw = new LambdaQueryWrapper<>();
			tw.eq(MemberTags::getCompanyId, companyId).in(MemberTags::getTagId, tagIds);
			List<MemberTags> tags = memberTagsMapper.selectList(tw);
			tagIdToName =
					tags.stream()
							.collect(
									Collectors.toMap(
											MemberTags::getTagId,
											t -> t.getTagName() == null ? "" : t.getTagName(),
											(a, b) -> a));
		}

		ZoneId zone = ZoneId.systemDefault();
		DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("uuuu-MM-dd");
		String nullEpochDay = Instant.EPOCH.atZone(zone).toLocalDate().format(dayFmt);
		List<Map<String, Object>> outList = new ArrayList<>(records.size());
		for (SpecificCrowdDiscount e : records) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>(toColumnMap(e));
			Long specId = e.getSpecificId();
			row.put("specific_name", tagIdToName.getOrDefault(specId, ""));
			if (e.getStartTime() == null) {
				row.put("start_date", nullEpochDay);
			} else {
				row.put(
						"start_date",
						Instant.ofEpochSecond(e.getStartTime()).atZone(zone).toLocalDate().format(dayFmt));
			}
			if (e.getEndTime() == null) {
				row.put("end_date", nullEpochDay);
			} else {
				row.put(
						"end_date",
						Instant.ofEpochSecond(e.getEndTime()).atZone(zone).toLocalDate().format(dayFmt));
			}
			Integer limitTotalMoney = e.getLimitTotalMoney();
			if (limitTotalMoney == null) {
				row.put("limit_total_money", null);
			} else {
				row.put("limit_total_money", formatLimitTotalMoneyForListCents(limitTotalMoney));
			}
			outList.add(row);
		}
		out.put("list", outList);
		return out;
	}

	public Map<String, Object> getSpecificcrowddiscountLogList(
			long companyId,
			String activityIdRaw,
			String mobileRaw,
			String orderIdRaw,
			String timeStartBeginRaw,
			String timeStartEndRaw,
			String pageRaw,
			String pageSizeRaw,
			boolean datapassBlocked) {
		LambdaQueryWrapper<SpecificCrowdDiscountRelUser> w = new LambdaQueryWrapper<>();
		w.eq(SpecificCrowdDiscountRelUser::getCompanyId, companyId);

		if (activityIdRaw == null) {
			w.isNull(SpecificCrowdDiscountRelUser::getActivityId);
		} else {
			String v = activityIdRaw.trim();
			if (v.isEmpty()) {
				w.apply("activity_id = {0}", "");
			} else {
				try {
					long parsedLong = Long.parseLong(v);
					w.eq(SpecificCrowdDiscountRelUser::getActivityId, parsedLong);
				} catch (NumberFormatException e) {
					w.apply("activity_id = {0}", v);
				}
			}
		}

		if (isTruthyRequestQueryString(orderIdRaw)) {
			w.eq(SpecificCrowdDiscountRelUser::getOrderId, orderIdRaw.trim());
		}
		if (isTruthyRequestQueryString(timeStartBeginRaw)) {
			String t = timeStartBeginRaw.trim();
			long ts;
			try {
				ts = Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("时间筛选格式不正确");
			}
			w.ge(SpecificCrowdDiscountRelUser::getCreated, ts);
		}
		if (isTruthyRequestQueryString(timeStartEndRaw)) {
			String t = timeStartEndRaw.trim();
			long ts;
			try {
				ts = Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("时间筛选格式不正确");
			}
			w.lt(SpecificCrowdDiscountRelUser::getCreated, ts);
		}

		boolean mobileMemberBranch = false;
		String branchMobilePlain = "";
		if (StringUtils.hasText(mobileRaw == null ? "" : mobileRaw.trim())) {
			Members m = memberAccountService.findMemberByCompanyAndMobile(companyId, mobileRaw.trim());
			if (m == null) {
				LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
				empty.put("total_count", 0L);
				empty.put("list", Collections.emptyList());
				return empty;
			}
			mobileMemberBranch = true;
			long filterUserId = m.getUserId();
			w.eq(SpecificCrowdDiscountRelUser::getUserId, filterUserId);
			branchMobilePlain = memberAccountService.resolveMemberMobileForH5Context(filterUserId, companyId);
		}

		int page = parseListIntParam(pageRaw, "页码格式不正确");
		if (page < 1) {
			page = 1;
		}

		String ps = pageSizeRaw == null ? "-1" : pageSizeRaw.trim();
		if (ps.isEmpty()) {
			ps = "-1";
		}
		int pageSize;
		try {
			pageSize = Integer.parseInt(ps);
		} catch (NumberFormatException e) {
			throw new BadRequestException("每页条数格式不正确");
		}

		long total = specificCrowdDiscountRelUserMapper.selectCount(w);
		List<SpecificCrowdDiscountRelUser> records;
		if (total == 0L) {
			records = Collections.emptyList();
		} else if (pageSize <= 0) {
			records = specificCrowdDiscountRelUserMapper.selectList(w);
		} else {
			Page<SpecificCrowdDiscountRelUser> mpPage = new Page<>(page, pageSize, false);
			records = specificCrowdDiscountRelUserMapper.selectPage(mpPage, w).getRecords();
		}

		List<Map<String, Object>> outList = new ArrayList<>();
		if (!records.isEmpty()) {
			Map<Long, Map<String, String>> contacts = Collections.emptyMap();
			if (!mobileMemberBranch) {
				List<Long> userIds =
						records.stream()
								.map(SpecificCrowdDiscountRelUser::getUserId)
								.filter(Objects::nonNull)
								.distinct()
								.toList();
				contacts =
						membersContactByUserIdsLookupService.loadDecryptedContactsByUserIds(
								userIds, Math.max(userIds.size(), 1));
			}
			for (SpecificCrowdDiscountRelUser entity : records) {
				LinkedHashMap<String, Object> row = new LinkedHashMap<>();
				row.put("id", entity.getId());
				row.put("company_id", entity.getCompanyId());
				row.put("user_id", entity.getUserId());
				row.put("order_id", entity.getOrderId());
				row.put("discount_fee", entity.getDiscountFee());
				row.put("activity_id", entity.getActivityId());
				row.put("specific_id", entity.getSpecificId());
				row.put("specific_name", entity.getSpecificName());
				row.put("activity_month", entity.getActivityMonth());
				row.put("action_type", entity.getActionType());
				row.put("created", entity.getCreated());

				String userMobile;
				if (mobileMemberBranch) {
					userMobile = branchMobilePlain == null ? "" : branchMobilePlain;
					if (datapassBlocked && StringUtils.hasText(userMobile)) {
						userMobile = DataMasking.maskMobile(userMobile);
					}
				} else {
					Long userId = entity.getUserId();
					String mob =
							Optional.ofNullable(userId == null ? null : contacts.get(userId))
									.map(cm -> cm.getOrDefault("mobile", ""))
									.orElse("");
					if (mob.isEmpty()) {
						userMobile = "-";
					} else {
						userMobile = mob;
					}
					if (datapassBlocked && !"-".equals(userMobile)) {
						userMobile = DataMasking.maskMobile(userMobile);
					}
				}
				row.put("user_mobile", userMobile);
				outList.add(row);
			}
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", outList);
		return out;
	}

	private static int parseListIntParam(String raw, String badFormatMessage) {
		String s = raw == null ? "" : raw.trim();
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(badFormatMessage);
		}
	}

	private static boolean isTruthyRequestQueryString(String s) {
		if (s == null) {
			return false;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		return true;
	}

	private static String formatLimitTotalMoneyForListCents(Integer cents) {
		return BigDecimal.valueOf(cents.intValue())
				.divide(new BigDecimal("100"), 2, RoundingMode.DOWN)
				.toPlainString();
	}

	private void multiplyLimitYuanToCentsForUpdate(Map<String, Object> merged, Locale locale) {
		Object ltm = merged.get("limit_total_money");
		if (ltm == null || !StringUtils.hasText(String.valueOf(ltm).trim())) {
			throw new BadRequestException(
					msg(
							"promotions.specific_crowd.limit_total_money_must_be_numeric",
							"周期内优惠限额必须为数字",
							locale));
		}
		String trim = String.valueOf(ltm).trim();
		try {
			BigDecimal yuan = new BigDecimal(trim);
			BigDecimal centsBd = yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.DOWN);
			merged.put("limit_total_money", (int) centsBd.longValue());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.specific_crowd.invalid_limit_total_money", "周期内优惠限额格式不正确", locale));
		}
	}

	private void applySpecificCrowdPostValidation(Map<String, Object> merged, long companyId, Locale locale) {
		String specificType = Objects.toString(merged.get("specific_type"), "member_tag").trim();
		if (!StringUtils.hasText(specificType)) {
			specificType = "member_tag";
		}
		switch (specificType) {
			case "member_tag": {
				Object sidRaw = merged.get("specific_id");
				if (sidRaw == null || !StringUtils.hasText(String.valueOf(sidRaw).trim())) {
					throw new ResourceException(
							msg(
									"promotions.specific_crowd.please_select_correct_crowd",
									"请选择正确的针对人群",
									locale));
				}
				long specificIdLong;
				try {
					specificIdLong =
							sidRaw instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(sidRaw).trim());
				} catch (NumberFormatException e) {
					throw new ResourceException(
							msg(
									"promotions.specific_crowd.please_select_correct_crowd",
									"请选择正确的针对人群",
									locale));
				}
				if (specificIdLong <= 0L) {
					throw new ResourceException(
							msg(
									"promotions.specific_crowd.please_select_correct_crowd",
									"请选择正确的针对人群",
									locale));
				}
				LambdaQueryWrapper<MemberTags> w = new LambdaQueryWrapper<>();
				w.eq(MemberTags::getCompanyId, companyId).eq(MemberTags::getTagId, specificIdLong);
				if (memberTagsMapper.selectCount(w) == 0) {
					throw new ResourceException(
							msg(
									"promotions.specific_crowd.please_select_correct_crowd",
									"请选择正确的针对人群",
									locale));
				}
				break;
			}
			default:
				break;
		}

		Object discountRaw = merged.get("discount");
		if (discountRaw == null || !StringUtils.hasText(String.valueOf(discountRaw).trim())) {
			throw new ResourceException(
					msg("promotions.specific_crowd.discount_invalid", "折扣参数格式不正确", locale));
		}
		long discountLong = parseLongFlexible(discountRaw, locale);
		if (discountLong < 1L || discountLong > 100L) {
			throw new BadRequestException(
					msg(
							"promotions.specific_crowd.discount_percentage_range_error",
							"周期内优惠折扣必须1-100的整数",
							locale));
		}

		Object ltmAfter = merged.get("limit_total_money");
		if (ltmAfter == null || !isNumericLimitTotalMoneyCents(ltmAfter)) {
			throw new BadRequestException(
					msg(
							"promotions.specific_crowd.limit_total_money_must_be_numeric",
							"周期内优惠限额必须为数字",
							locale));
		}
	}

	private boolean isNumericLimitTotalMoneyCents(Object v) {
		if (v instanceof Number) {
			return true;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return false;
		}
		try {
			new BigDecimal(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private long readCompanyId(Map<String, Object> merged) {
		Object v = merged.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	private int parseIntDefault(Object value, int defaultWhenNullOrBlank, Locale locale) {
		if (value == null) {
			return defaultWhenNullOrBlank;
		}
		String s = String.valueOf(value).trim();
		if (!StringUtils.hasText(s)) {
			return defaultWhenNullOrBlank;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.specific_crowd.invalid_cycle_type", "周期类型参数格式不正确", locale));
		}
	}

	private long parseLongFlexible(Object value, Locale locale) {
		String s = String.valueOf(value).trim();
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException(
					msg("promotions.specific_crowd.discount_invalid", "折扣参数格式不正确", locale));
		}
	}

	private long normalizeEpochMaybeMillis(String mapKey, Object rawValue, Map<String, Object> merged, Locale locale) {
		Object v = rawValue;
		BigDecimal bd;
		try {
			bd = new BigDecimal(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.specific_crowd.invalid_timestamp", "时间戳格式不正确", locale));
		}
		BigInteger bi = bd.toBigInteger();
		int len = bi.abs().toString().length();
		long normalizedSeconds = len > 10 ? bi.longValue() / 1000L : bi.longValue();
		merged.put(mapKey, normalizedSeconds);
		return normalizedSeconds;
	}

	private static Long toNullableLong(Object v) {
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
		return Long.parseLong(s);
	}

	private static Long readStatusLong(Object v) {
		if (v == null) {
			return 1L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return 1L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("状态参数格式不正确");
		}
	}

	private static Map<String, Object> toColumnMap(SpecificCrowdDiscount e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("specific_type", e.getSpecificType());
		m.put("specific_id", e.getSpecificId());
		m.put("cycle_type", e.getCycleType());
		m.put("start_time", e.getStartTime());
		m.put("end_time", e.getEndTime());
		m.put("discount", e.getDiscount());
		m.put("limit_total_money", e.getLimitTotalMoney());
		m.put("status", e.getStatus());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}
}
