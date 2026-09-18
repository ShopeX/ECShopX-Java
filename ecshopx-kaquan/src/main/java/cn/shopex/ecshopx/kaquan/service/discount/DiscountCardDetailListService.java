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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountLogs;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountLogsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardDetailListService {

	private static final String CARD_DETAIL_ERROR_MSG = "获取卡券的详细信息出错.";
	private static final String NONE_DISPLAY = "无";
	private static final Pattern INTEGER_STRING = Pattern.compile("^-?\\d+$");

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final UserDiscountMapper userDiscountMapper;
	private final UserDiscountLogsMapper userDiscountLogsMapper;
	private final MemberAccountService memberAccountService;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public DiscountCardDetailListService(
			UserDiscountMapper userDiscountMapper,
			UserDiscountLogsMapper userDiscountLogsMapper,
			MemberAccountService memberAccountService,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.userDiscountMapper = userDiscountMapper;
		this.userDiscountLogsMapper = userDiscountLogsMapper;
		this.memberAccountService = memberAccountService;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	/** 与「获取卡券明细」接口相同的 card_id 校验规则，供 {@code DiscountCardController#getDiscountCardDetail} 复用。 */
	public void validateCardIdForAdminDetail(String cardIdRaw) {
		validateAndParseCardId(cardIdRaw);
	}

	public Map<String, Object> query(
			long companyId,
			String cardIdRaw,
			String isUseRaw,
			String mobile,
			String activityName,
			String statusRaw,
			String pageRaw,
			String pageSizeRaw) {
		validateAndParseCardId(cardIdRaw);
		long cardId = Long.parseLong(cardIdRaw.trim());

		int page = parseOptionalPage(pageRaw);
		int pageSize = parseOptionalPageSize(pageSizeRaw);

		if (isIsUseTrue(isUseRaw)) {
			return queryLogsBranch(companyId, cardId, mobile, page, pageSize);
		}
		return queryListBranch(companyId, cardId, mobile, activityName, statusRaw, page, pageSize);
	}

	private void validateAndParseCardId(String cardIdRaw) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		if (!StringUtils.hasText(cardIdRaw)) {
			errors.put("card_id", List.of("validation.required"));
			throw new BadRequestException(CARD_DETAIL_ERROR_MSG, errors);
		}
		String t = cardIdRaw.trim();
		if (!INTEGER_STRING.matcher(t).matches()) {
			errors.put("card_id", List.of("validation.integer"));
			throw new BadRequestException(CARD_DETAIL_ERROR_MSG, errors);
		}
		try {
			Long.parseLong(t);
		} catch (NumberFormatException e) {
			errors.put("card_id", List.of("validation.integer"));
			throw new BadRequestException(CARD_DETAIL_ERROR_MSG, errors);
		}
	}

	private int parseOptionalPage(String pageRaw) {
		if (!StringUtils.hasText(pageRaw)) {
			return 1;
		}
		Map<String, List<String>> errors = new LinkedHashMap<>();
		String s = pageRaw.trim();
		if (!INTEGER_STRING.matcher(s).matches()) {
			errors.put("page", List.of("validation.integer"));
			throw new BadRequestException(CARD_DETAIL_ERROR_MSG, errors);
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			errors.put("page", List.of("validation.integer"));
			throw new BadRequestException(CARD_DETAIL_ERROR_MSG, errors);
		}
	}

	private int parseOptionalPageSize(String pageSizeRaw) {
		if (!StringUtils.hasText(pageSizeRaw)) {
			return 10;
		}
		Map<String, List<String>> errors = new LinkedHashMap<>();
		String s = pageSizeRaw.trim();
		if (!INTEGER_STRING.matcher(s).matches()) {
			errors.put("pageSize", List.of("validation.integer"));
			throw new BadRequestException(CARD_DETAIL_ERROR_MSG, errors);
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			errors.put("pageSize", List.of("validation.integer"));
			throw new BadRequestException(CARD_DETAIL_ERROR_MSG, errors);
		}
	}

	private static boolean isIsUseTrue(String isUseRaw) {
		if (!StringUtils.hasText(isUseRaw)) {
			return false;
		}
		String t = isUseRaw.trim();
		return !t.isEmpty() && !"0".equals(t);
	}

	private Map<String, Object> queryLogsBranch(long companyId, long cardId, String mobile, int page, int pageSize) {
		LambdaQueryWrapper<UserDiscountLogs> wrapper = baseLogsWrapper(companyId, cardId, mobile);
		long total = userDiscountLogsMapper.selectCount(wrapper);
		List<Map<String, Object>> list = new ArrayList<>();
		if (total > 0) {
			int offset = pageSize * (page - 1);
			LambdaQueryWrapper<UserDiscountLogs> pageWrapper = baseLogsWrapper(companyId, cardId, mobile);
			pageWrapper.orderByDesc(UserDiscountLogs::getUsedTime);
			pageWrapper.last("LIMIT " + offset + "," + pageSize);
			List<UserDiscountLogs> rows = userDiscountLogsMapper.selectList(pageWrapper);
			for (UserDiscountLogs row : rows) {
				list.add(logsRowToMap(row));
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", (int) total);
		return out;
	}

	private LambdaQueryWrapper<UserDiscountLogs> baseLogsWrapper(long companyId, long cardId, String mobile) {
		LambdaQueryWrapper<UserDiscountLogs> w = new LambdaQueryWrapper<>();
		w.eq(UserDiscountLogs::getCompanyId, companyId).eq(UserDiscountLogs::getCardId, cardId);
		applyMobileUserFilterLogs(w, companyId, mobile);
		return w;
	}

	private void applyMobileUserFilterLogs(LambdaQueryWrapper<UserDiscountLogs> w, long companyId, String mobile) {
		if (!StringUtils.hasText(mobile)) {
			return;
		}
		Members m = memberAccountService.findMemberByCompanyAndMobile(companyId, mobile.trim());
		if (m != null) {
			w.eq(UserDiscountLogs::getUserId, m.getUserId());
		} else {
			w.isNull(UserDiscountLogs::getUserId);
		}
	}

	private Map<String, Object> logsRowToMap(UserDiscountLogs e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("user_id", e.getUserId());
		m.put("company_id", e.getCompanyId());
		m.put("mobile", e.getMobile());
		m.put("username", e.getUsername());
		m.put("card_id", e.getCardId());
		m.put("code", e.getCode());
		m.put("title", e.getTitle());
		m.put("card_type", e.getCardType());
		m.put("shop_name", e.getShopName());
		Integer ut = e.getUsedTime();
		if (ut == null) {
			m.put("used_time", null);
		} else {
			m.put("used_time", DATE_TIME_FMT.format(Instant.ofEpochSecond(ut.longValue())));
		}
		m.put("used_status", e.getUsedStatus());
		m.put("used_order", e.getUsedOrder());
		return m;
	}

	private Map<String, Object> queryListBranch(
			long companyId,
			long cardId,
			String mobile,
			String activityName,
			String statusRaw,
			int page,
			int pageSize) {
		int effPageSize = pageSize > 50 ? 50 : pageSize;
		effPageSize = effPageSize <= 0 ? 20 : effPageSize;
		long nowEpoch = System.currentTimeMillis() / 1000L;

		LambdaQueryWrapper<UserDiscount> wrapper = baseUserDiscountWrapper(companyId, cardId, mobile, activityName, statusRaw, nowEpoch);
		long cardTotal = userDiscountMapper.selectCount(wrapper);

		List<Map<String, Object>> list = new ArrayList<>();
		if (cardTotal > 0) {
			int offset = (page - 1) * effPageSize;
			LambdaQueryWrapper<UserDiscount> pageWrapper =
					baseUserDiscountWrapper(companyId, cardId, mobile, activityName, statusRaw, nowEpoch);
			pageWrapper
					.orderByAsc(UserDiscount::getEndDate)
					.orderByAsc(UserDiscount::getStatus)
					.orderByDesc(UserDiscount::getId)
					.last("LIMIT " + offset + "," + effPageSize);
			List<UserDiscount> rows = userDiscountMapper.selectList(pageWrapper);
			Set<Long> uids = new LinkedHashSet<>();
			for (UserDiscount ud : rows) {
				if (ud.getUserId() != null) {
					uids.add(ud.getUserId());
				}
			}
			Map<Long, MemberDisplay> memberByUserId = loadMemberDisplays(companyId, uids);
			for (UserDiscount ud : rows) {
				list.add(buildUserDiscountListRow(ud, memberByUserId));
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("count", (int) cardTotal);
		return out;
	}

	private LambdaQueryWrapper<UserDiscount> baseUserDiscountWrapper(
			long companyId,
			long cardId,
			String mobile,
			String activityName,
			String statusRaw,
			long nowEpoch) {
		LambdaQueryWrapper<UserDiscount> w = new LambdaQueryWrapper<>();
		w.eq(UserDiscount::getCompanyId, companyId).eq(UserDiscount::getCardId, cardId);
		if (StringUtils.hasText(activityName)) {
			w.like(UserDiscount::getActivityName, "%" + activityName.trim() + "%");
		}
		applyMobileUserFilterDiscount(w, companyId, mobile);
		applyUserDiscountStatusFilter(w, statusRaw, nowEpoch);
		return w;
	}

	private void applyMobileUserFilterDiscount(LambdaQueryWrapper<UserDiscount> w, long companyId, String mobile) {
		if (!StringUtils.hasText(mobile)) {
			return;
		}
		Members m = memberAccountService.findMemberByCompanyAndMobile(companyId, mobile.trim());
		if (m != null) {
			w.eq(UserDiscount::getUserId, m.getUserId());
		} else {
			w.isNull(UserDiscount::getUserId);
		}
	}

	private void applyUserDiscountStatusFilter(LambdaQueryWrapper<UserDiscount> w, String statusRaw, long nowEpoch) {
		if (!StringUtils.hasText(statusRaw)) {
			return;
		}
		String s = statusRaw.trim();
		int now = (int) nowEpoch;
		switch (s) {
			case "1" -> w.in(UserDiscount::getStatus, 1, 4, 10).gt(UserDiscount::getEndDate, now);
			case "2" -> w.eq(UserDiscount::getStatus, 2);
			case "3" -> w.in(UserDiscount::getStatus, 1, 5, 6).lt(UserDiscount::getEndDate, now);
			default -> {
				try {
					w.eq(UserDiscount::getStatus, Integer.parseInt(s));
				} catch (NumberFormatException e) {
					w.apply("1=0");
				}
			}
		}
	}

	private record MemberDisplay(String username, String mobile) {}

	private Map<Long, MemberDisplay> loadMemberDisplays(long companyId, Set<Long> userIds) {
		Map<Long, MemberDisplay> out = new LinkedHashMap<>();
		if (userIds.isEmpty()) {
			return out;
		}
		List<Members> members =
				membersMapper.selectList(new LambdaQueryWrapper<Members>()
						.eq(Members::getCompanyId, companyId)
						.in(Members::getUserId, userIds));
		Map<Long, Members> byUid = new LinkedHashMap<>();
		for (Members m : members) {
			byUid.put(m.getUserId(), m);
		}
		List<MembersInfo> infos =
				membersInfoMapper.selectList(new LambdaQueryWrapper<MembersInfo>()
						.eq(MembersInfo::getCompanyId, companyId)
						.in(MembersInfo::getUserId, userIds));
		Map<Long, String> usernameByUid = new LinkedHashMap<>();
		for (MembersInfo info : infos) {
			if (info.getUsername() != null) {
				usernameByUid.put(info.getUserId(), info.getUsername());
			}
		}
		for (Long uid : userIds) {
			Members m = byUid.get(uid);
			String username = usernameByUid.get(uid);
			if (!StringUtils.hasText(username)) {
				username = NONE_DISPLAY;
			}
			String mobilePlain = NONE_DISPLAY;
			if (m != null) {
				String decrypted = sensitiveFieldEncryptor.decrypt(m.getMobile());
				if (decrypted != null && !decrypted.isBlank()) {
					mobilePlain = decrypted;
				} else if (m.getRegionMobile() != null && !m.getRegionMobile().isBlank()) {
					mobilePlain = m.getRegionMobile();
				}
			}
			out.put(uid, new MemberDisplay(username, mobilePlain));
		}
		return out;
	}

	private Map<String, Object> buildUserDiscountListRow(UserDiscount ud, Map<Long, MemberDisplay> memberByUserId) {
		Map<String, Object> row = userDiscountToSnakeMap(ud);
		Integer bd = ud.getBeginDate();
		Integer ed = ud.getEndDate();
		if (bd != null) {
			row.put("begin_date_str", DATE_TIME_FMT.format(Instant.ofEpochSecond(bd.longValue())));
		} else {
			row.put("begin_date_str", null);
		}
		if (ed != null) {
			row.put("end_date_str", DATE_TIME_FMT.format(Instant.ofEpochSecond(ed.longValue())));
		} else {
			row.put("end_date_str", null);
		}

		String relShops = ud.getRelShopsIds();
		if (StringUtils.hasText(relShops) && !"all".equals(relShops)) {
			row.put("rel_shops_ids", splitCommaList(relShops));
		} else {
			row.put("rel_shops_ids", "all");
		}

		String relItems = ud.getRelItemIds();
		if (StringUtils.hasText(relItems) && !"all".equals(relItems)) {
			row.put("rel_item_ids", splitCommaList(relItems));
			row.put("use_all_items", false);
		} else {
			row.put("rel_item_ids", "all");
			row.put("use_all_items", true);
		}

		row.put("use_condition", parseUseCondition(ud.getUseCondition()));
		row.put("status_msg", statusMessage(ud.getStatus()));

		Long uid = ud.getUserId();
		MemberDisplay md = uid != null ? memberByUserId.get(uid) : null;
		row.put("username", md != null ? md.username() : NONE_DISPLAY);
		row.put("mobile", md != null ? md.mobile() : NONE_DISPLAY);
		return row;
	}

	private static List<String> splitCommaList(String raw) {
		String[] parts = raw.split(",");
		List<String> list = new ArrayList<>();
		for (String p : parts) {
			if (p != null && !p.isBlank()) {
				list.add(p.trim());
			}
		}
		return list;
	}

	private Object parseUseCondition(String raw) {
		if (!StringUtils.hasText(raw)) {
			return raw;
		}
		try {
			return objectMapper.readValue(raw.trim(), Object.class);
		} catch (JsonProcessingException e) {
			return raw;
		}
	}

	private static String statusMessage(Integer status) {
		if (status == null) {
			return "";
		}
		return switch (status) {
			case 1 -> "未核销";
			case 2 -> "已核销";
			case 3 -> "已转赠";
			case 5 -> "已过期";
			default -> "";
		};
	}

	private static Map<String, Object> userDiscountToSnakeMap(UserDiscount ud) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", ud.getId());
		m.put("user_id", ud.getUserId());
		m.put("company_id", ud.getCompanyId());
		m.put("card_id", ud.getCardId());
		m.put("code", ud.getCode());
		m.put("source_type", ud.getSourceType());
		m.put("status", ud.getStatus());
		m.put("card_type", ud.getCardType());
		m.put("use_platform", ud.getUsePlatform());
		m.put("begin_date", ud.getBeginDate());
		m.put("end_date", ud.getEndDate());
		m.put("get_date", ud.getGetDate());
		m.put("title", ud.getTitle());
		m.put("color", ud.getColor());
		m.put("discount", ud.getDiscount());
		m.put("least_cost", ud.getLeastCost());
		m.put("reduce_cost", ud.getReduceCost());
		m.put("rel_shops_ids", ud.getRelShopsIds());
		m.put("rel_item_ids", ud.getRelItemIds());
		m.put("rel_distributor_ids", ud.getRelDistributorIds());
		m.put("consume_source", ud.getConsumeSource());
		m.put("get_outer_str", ud.getGetOuterStr());
		m.put("location_name", ud.getLocationName());
		m.put("staff_open_id", ud.getStaffOpenId());
		m.put("verify_code", ud.getVerifyCode());
		m.put("remark_amount", ud.getRemarkAmount());
		m.put("consume_outer_str", ud.getConsumeOuterStr());
		m.put("trans_id", ud.getTransId());
		m.put("fee", ud.getFee());
		m.put("original_fee", ud.getOriginalFee());
		m.put("location_id", ud.getLocationId());
		m.put("use_scenes", ud.getUseScenes());
		m.put("most_cost", ud.getMostCost());
		m.put("use_condition", ud.getUseCondition());
		m.put("is_give_by_friend", ud.getIsGiveByFriend());
		m.put("old_code", ud.getOldCode());
		m.put("friend_open_id", ud.getFriendOpenId());
		m.put("is_return_back", ud.getIsReturnBack());
		m.put("is_chat_room", ud.getIsChatRoom());
		m.put("salesperson_id", ud.getSalespersonId());
		m.put("salesperson_code", ud.getSalespersonCode());
		m.put("use_limited", ud.getUseLimited());
		m.put("remain_times", ud.getRemainTimes());
		m.put("use_bound", ud.getUseBound());
		m.put("rel_category_ids", ud.getRelCategoryIds());
		m.put("apply_scope", ud.getApplyScope());
		m.put("used_time", ud.getUsedTime());
		m.put("expired_time", ud.getExpiredTime());
		m.put("activity_name", ud.getActivityName());
		m.put("dm_card_code", ud.getDmCardCode());
		return m;
	}
}
