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

package cn.shopex.ecshopx.members.service.export;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.members.mapper.AdminMemberBatchOperatingMapper;
import cn.shopex.ecshopx.members.service.admin.AdminMemberRegisterSettingService;
import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import cn.shopex.ecshopx.members.service.admin.dto.AdminMemberBatchOperatingMemberQueryFilter;
import cn.shopex.ecshopx.members.service.export.dto.AdminMemberExportRow;
import cn.shopex.ecshopx.members.service.export.port.AdminMemberExportCsvFilePort;
import cn.shopex.ecshopx.members.service.export.port.AdminMemberExportFinishLogPort;
import cn.shopex.ecshopx.members.service.export.port.AdminMemberExportMemberCardGradesPort;
import cn.shopex.ecshopx.members.service.export.port.AdminMemberExportSalesmanLookupPort;
import cn.shopex.ecshopx.members.service.export.port.AdminMemberExportUserVipGradePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberCsvExportRunService {

	private static final Logger log = LoggerFactory.getLogger(AdminMemberCsvExportRunService.class);
	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);
	private static final int PAGE_LIMIT = 500;

	private final AdminMemberBatchOperatingMapper adminMemberBatchOperatingMapper;
	private final AdminMemberExportCsvFilePort csvFilePort;
	private final AdminMemberExportFinishLogPort finishLogPort;
	private final AdminMemberRegisterSettingService adminMemberRegisterSettingService;
	private final AdminMemberExportMemberCardGradesPort memberCardGradesPort;
	private final AdminMemberExportUserVipGradePort userVipGradePort;
	private final MembersContactByUserIdsLookupService membersContactByUserIdsLookupService;
	private final AdminMemberExportSalesmanLookupPort salesmanLookupPort;
	private final ObjectMapper objectMapper;

	public AdminMemberCsvExportRunService(
			AdminMemberBatchOperatingMapper adminMemberBatchOperatingMapper,
			AdminMemberExportCsvFilePort csvFilePort,
			AdminMemberExportFinishLogPort finishLogPort,
			AdminMemberRegisterSettingService adminMemberRegisterSettingService,
			AdminMemberExportMemberCardGradesPort memberCardGradesPort,
			AdminMemberExportUserVipGradePort userVipGradePort,
			MembersContactByUserIdsLookupService membersContactByUserIdsLookupService,
			AdminMemberExportSalesmanLookupPort salesmanLookupPort,
			ObjectMapper objectMapper) {
		this.adminMemberBatchOperatingMapper = adminMemberBatchOperatingMapper;
		this.csvFilePort = csvFilePort;
		this.finishLogPort = finishLogPort;
		this.adminMemberRegisterSettingService = adminMemberRegisterSettingService;
		this.memberCardGradesPort = memberCardGradesPort;
		this.userVipGradePort = userVipGradePort;
		this.membersContactByUserIdsLookupService = membersContactByUserIdsLookupService;
		this.salesmanLookupPort = salesmanLookupPort;
		this.objectMapper = objectMapper;
	}

	public void runExport(AdminMemberExportJobContext ctx) {
		if (ctx.queryFilter() == null) {
			log.debug("member export skipped: empty filter");
			return;
		}
		AdminMemberBatchOperatingMemberQueryFilter filter = ctx.queryFilter();
		long count = adminMemberBatchOperatingMapper.countMembers(filter);
		if (count == 0L) {
			return;
		}
		long companyId = ctx.companyId();
		LinkedHashMap<String, String> titles = buildTitleRow();

		Map<String, Object> settingRoot =
				adminMemberRegisterSettingService
						.readMemberRegSettingRoot(companyId)
						.map(this::extractSettingNode)
						.orElse(null);

		Map<Long, String> gradeIdToName = new LinkedHashMap<>();
		for (Map<String, Object> g : memberCardGradesPort.getGradeListByCompanyId(companyId, false)) {
			Object gid = g.get("grade_id");
			Object name = g.get("grade_name");
			if (gid instanceof Number n && name != null) {
				gradeIdToName.put(n.longValue(), String.valueOf(name));
			}
		}

		int pages = (int) Math.ceil(count / (double) PAGE_LIMIT);
		List<Map<String, String>> allRows = new ArrayList<>();
		for (int page = 1; page <= pages; page++) {
			long offset = (page - 1L) * PAGE_LIMIT;
			List<AdminMemberExportRow> pageRows =
					adminMemberBatchOperatingMapper.listMemberExportRowsPage(filter, offset, PAGE_LIMIT);

			LinkedHashSet<Long> contactUserIds = new LinkedHashSet<>();
			LinkedHashSet<Long> pageMemberUserIds = new LinkedHashSet<>();
			for (AdminMemberExportRow r : pageRows) {
				if (r.getUserId() != null && r.getUserId() > 0L) {
					pageMemberUserIds.add(r.getUserId());
					contactUserIds.add(r.getUserId());
				}
				if (r.getInviterId() != null && r.getInviterId() > 0L) {
					contactUserIds.add(r.getInviterId());
				}
			}
			List<Long> contactIdList = new ArrayList<>(contactUserIds);
			int contactLimit = Math.max(1, contactIdList.size());
			Map<Long, Map<String, String>> contacts =
					membersContactByUserIdsLookupService.loadDecryptedContactsByUserIds(
							contactIdList, contactLimit);

			Map<Long, Map<String, Object>> vipByUserId = new LinkedHashMap<>();
			for (Long uid : pageMemberUserIds) {
				if (uid == null || uid <= 0L) {
					continue;
				}
				vipByUserId.put(uid, userVipGradePort.userVipGradeGet(companyId, uid, false));
			}

			for (AdminMemberExportRow r : pageRows) {
				allRows.add(
						buildOneCsvRow(
								ctx,
								r,
								settingRoot,
								gradeIdToName,
								contacts,
								vipByUserId,
								companyId));
			}
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + companyId + "member";
		Map<String, String> uploaded = csvFilePort.exportCsv(fileBaseName, titles, allRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("member export: csv upload empty");
			return;
		}
		finishLogPort.createFinishLog(
				companyId,
				ctx.operatorId(),
				0L,
				ctx.supplierId(),
				"member",
				uploaded.getOrDefault("filename", fileBaseName + ".csv"),
				uploaded.get("url"),
				Instant.now().getEpochSecond());
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> extractSettingNode(Map<String, Object> root) {
		Object s = root.get("setting");
		if (s instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return null;
	}

	private LinkedHashMap<String, String> buildTitleRow() {
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("user_card_code", "会员卡编号");
		titles.put("mobile", "会员手机号");
		titles.put("name", "用户名");
		titles.put("sex", "性别");
		titles.put("username", "姓名");
		titles.put("created_date", "注册时间");
		titles.put("shop_name", "所属店铺");
		titles.put("store_name", "所属门店");
		titles.put("vip_grade", "vip付费会员等级");
		titles.put("vip_day", "vip付费会员剩余天数");
		titles.put("svip_grade", "svip付费会员等级");
		titles.put("svip_day", "svip付费会员剩余天数");
		titles.put("grade_id", "普通会员等级");
		titles.put("inviter_id", "推荐人手机号");
		titles.put("birthday", "出生日期");
		titles.put("address", "家庭住址");
		titles.put("email", "常用邮箱");
		titles.put("industry", "从事行业");
		titles.put("income", "年收入");
		titles.put("edu_background", "学历");
		titles.put("habbit", "爱好");
		titles.put("salesman", "导购员");
		titles.put("unionid", "unionid");
		titles.put("open_id", "openid");
		return titles;
	}

	private Map<String, String> buildOneCsvRow(
			AdminMemberExportJobContext ctx,
			AdminMemberExportRow r,
			Map<String, Object> regSetting,
			Map<Long, String> gradeIdToName,
			Map<Long, Map<String, String>> contacts,
			Map<Long, Map<String, Object>> vipByUserId,
			long companyId) {
		boolean mask = ctx.datapassBlock();
		long userId = r.getUserId() == null ? 0L : r.getUserId();

		Map<String, String> selfContact = contacts.getOrDefault(userId, Map.of());
		String mobilePlain = selfContact.getOrDefault("mobile", "");
		String usernamePlain = selfContact.getOrDefault("username", "");
		String birthdayPlain = blankToDash(r.getBirthday());
		String addressPlain = blankToDash(r.getAddress());

		if (mask) {
			mobilePlain = DataMasking.maskUname(mobilePlain);
			usernamePlain = DataMasking.maskTruename(usernamePlain);
			birthdayPlain = DataMasking.maskBirthday(birthdayPlain);
			addressPlain = DataMasking.maskDetailedAddress(addressPlain);
		}

		String createdDate = formatCreatedDate(r);

		String industryKey = blankToEmpty(r.getIndustry());
		String industryVal = regItemLabel(regSetting, "industry", industryKey);
		String incomeVal = regItemLabel(regSetting, "income", industryKey);
		String eduVal = regItemLabel(regSetting, "edu_background", industryKey);

		String habbitVal = resolveHabbitLabel(r.getHabbit());

		Map<String, Object> vipBundle = vipByUserId.getOrDefault(userId, Map.of());

		String inviterPhone = "-";
		if (r.getInviterId() != null && r.getInviterId() > 0L) {
			Map<String, String> c = contacts.get(r.getInviterId());
			if (c != null && StringUtils.hasText(c.get("mobile"))) {
				inviterPhone = c.get("mobile");
				if (mask && !"-".equals(inviterPhone)) {
					inviterPhone = DataMasking.maskUname(inviterPhone);
				}
			}
		}

		String gradeName = "-";
		if (r.getGradeId() != null) {
			gradeName = gradeIdToName.getOrDefault(r.getGradeId(), "-");
		}

		Map<String, Object> memberRowForSales = new LinkedHashMap<>();
		memberRowForSales.put("user_id", userId);
		memberRowForSales.put("company_id", companyId);
		memberRowForSales.put("inviter_id", r.getInviterId() == null ? 0L : r.getInviterId());
		memberRowForSales.put("mobile", mobilePlain);
		Map<String, Object> salesmanInfo = salesmanLookupPort.getSalesmanInfo(companyId, memberRowForSales);
		String salesmanCell = "-";
		if (salesmanInfo != null && !salesmanInfo.isEmpty()) {
			Object sm = salesmanInfo.get("mobile");
			if (sm != null && StringUtils.hasText(String.valueOf(sm))) {
				salesmanCell = String.valueOf(sm);
			}
		}

		String unionOut = r.getUnionId() == null ? "" : r.getUnionId().trim();
		String openOut = r.getOpenId() == null ? "" : r.getOpenId().trim();

		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		row.put("user_card_code", dashIfBlank(r.getUserCardCode()));
		row.put("mobile", dashIfBlank(mobilePlain));
		row.put("name", dashIfBlank(r.getName()));
		row.put("sex", formatSex(r.getSex()));
		row.put("username", dashIfBlank(usernamePlain));
		row.put("created_date", dashIfBlank(createdDate));
		row.put("shop_name", dashIfBlank(r.getShopName()));
		row.put("store_name", dashIfBlank(r.getStoreName()));
		row.put("vip_grade", vipSub(vipBundle, "vip", "grade_name"));
		row.put("vip_day", vipSub(vipBundle, "vip", "day"));
		row.put("svip_grade", vipSub(vipBundle, "svip", "grade_name"));
		row.put("svip_day", vipSub(vipBundle, "svip", "day"));
		row.put("grade_id", dashIfBlank(gradeName));
		row.put("inviter_id", dashIfBlank(inviterPhone));
		row.put("birthday", dashIfBlank(birthdayPlain));
		row.put("address", dashIfBlank(addressPlain));
		row.put("email", dashIfBlank(r.getEmail()));
		row.put("industry", dashIfBlank(industryVal));
		row.put("income", dashIfBlank(incomeVal));
		row.put("edu_background", dashIfBlank(eduVal));
		row.put("habbit", dashIfBlank(habbitVal));
		row.put("salesman", dashIfBlank(salesmanCell));
		row.put("unionid", dashIfBlank(unionOut));
		row.put("open_id", dashIfBlank(openOut));
		return row;
	}

	private static String blankToDash(String s) {
		return StringUtils.hasText(s) ? s : "-";
	}

	private static String blankToEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	private static String dashIfBlank(String s) {
		return StringUtils.hasText(s) ? s : "-";
	}

	private static String formatSex(Integer sex) {
		if (sex == null) {
			return "未知";
		}
		if (sex == 2) {
			return "女";
		}
		if (sex == 1) {
			return "男";
		}
		return "未知";
	}

	private static String formatCreatedDate(AdminMemberExportRow r) {
		Integer y = r.getCreatedYear();
		Integer m = r.getCreatedMonth();
		Integer d = r.getCreatedDay();
		if (y == null || m == null || d == null || y == 0) {
			return "";
		}
		return y + "-" + m + "-" + d;
	}

	private String resolveHabbitLabel(String habbitJson) {
		if (!StringUtils.hasText(habbitJson) || "-".equals(habbitJson.trim())) {
			return "-";
		}
		try {
			JsonNode root = objectMapper.readTree(habbitJson);
			List<String> parts = new ArrayList<>();
			if (root.isArray()) {
				for (JsonNode n : root) {
					if (n.isObject()) {
						JsonNode chk = n.get("ischecked");
						if ("true".equals(String.valueOf(chk))) {
							JsonNode name = n.get("name");
							if (name != null && StringUtils.hasText(name.asText())) {
								parts.add(name.asText());
							}
						}
					} else if (n.isTextual()) {
						parts.add(n.asText());
					}
				}
			}
			return parts.isEmpty() ? "-" : String.join(",", parts);
		} catch (Exception e) {
			return "-";
		}
	}

	private static String vipSub(Map<String, Object> bundle, String lvType, String field) {
		if (bundle == null || bundle.isEmpty()) {
			return "-";
		}
		Object lv = bundle.get(lvType);
		if (!(lv instanceof Map<?, ?> m)) {
			return "-";
		}
		Object v = m.get(field);
		return v != null && StringUtils.hasText(String.valueOf(v)) ? String.valueOf(v) : "-";
	}

	private static String regItemLabel(Map<String, Object> setting, String section, String key) {
		if (setting == null || !StringUtils.hasText(key)) {
			return "-";
		}
		Object sec = setting.get(section);
		if (!(sec instanceof Map<?, ?> secMap)) {
			return "-";
		}
		Object itemsObj = secMap.get("items");
		if (itemsObj instanceof Map<?, ?> im) {
			Object cell = im.get(key);
			return labelFromItemCell(cell);
		}
		if (itemsObj instanceof List<?> list) {
			for (Object elem : list) {
				if (elem instanceof Map<?, ?> row) {
					Object id = row.get("id");
					if (key.equals(String.valueOf(id))) {
						Object name = row.get("name");
						return name != null && StringUtils.hasText(String.valueOf(name)) ? String.valueOf(name) : "-";
					}
				}
			}
		}
		return "-";
	}

	private static String labelFromItemCell(Object cell) {
		if (cell == null) {
			return "-";
		}
		if (cell instanceof Map<?, ?> row) {
			Object name = row.get("name");
			return name != null && StringUtils.hasText(String.valueOf(name)) ? String.valueOf(name) : "-";
		}
		String t = String.valueOf(cell).trim();
		return t.isEmpty() ? "-" : t;
	}
}
