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

package cn.shopex.ecshopx.orders.service.rights.export.consume;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.RightsLog;
import cn.shopex.ecshopx.orders.domain.RightsLogsShopSalesperson;
import cn.shopex.ecshopx.orders.mapper.RightsLogMapper;
import cn.shopex.ecshopx.orders.mapper.RightsLogsShopSalespersonMapper;
import cn.shopex.ecshopx.orders.service.admin.RightsLogsListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RightsConsumeLogCsvExportService {

	private static final int PAGE_SIZE = 1000;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final RightsLogMapper rightsLogMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final WxShopsMapper wxShopsMapper;
	private final RightsLogsShopSalespersonMapper rightsLogsShopSalespersonMapper;
	private final MemberAccountService memberAccountService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final RightsLogsListService rightsLogsListService;

	public RightsConsumeLogCsvExportService(
			RightsLogMapper rightsLogMapper,
			ExportCsvFileService exportCsvFileService,
			WxShopsMapper wxShopsMapper,
			RightsLogsShopSalespersonMapper rightsLogsShopSalespersonMapper,
			MemberAccountService memberAccountService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			RightsLogsListService rightsLogsListService) {
		this.rightsLogMapper = rightsLogMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.wxShopsMapper = wxShopsMapper;
		this.rightsLogsShopSalespersonMapper = rightsLogsShopSalespersonMapper;
		this.memberAccountService = memberAccountService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.rightsLogsListService = rightsLogsListService;
	}

	public Optional<Map<String, String>> runExport(
			long companyId, LinkedHashMap<String, Object> workFilter, boolean datapassBlock) {
		LinkedHashMap<String, String> title = buildTitleRow();
		List<Map<String, String>> rows = new ArrayList<>();

		int pageNum = 1;
		while (true) {
			LambdaQueryWrapper<RightsLog> w = new LambdaQueryWrapper<>();
			rightsLogsListService.applyRightsLogFiltersFromJobMap(companyId, workFilter, w);
			w.orderByDesc(RightsLog::getCreated);
			Page<RightsLog> page = new Page<>(pageNum, PAGE_SIZE);
			rightsLogMapper.selectPage(page, w);
			List<RightsLog> records = page.getRecords();
			if (records == null || records.isEmpty()) {
				break;
			}
			for (RightsLog e : records) {
				rows.add(buildRow(companyId, e, datapassBlock));
			}
			if (records.size() < PAGE_SIZE) {
				break;
			}
			pageNum++;
		}

		if (rows.isEmpty()) {
			return Optional.empty();
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + companyId + "right_consume";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private LinkedHashMap<String, String> buildTitleRow() {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("shop_name", "门店名称");
		m.put("salesperson_name", "核销员");
		m.put("attendant", "服务员");
		m.put("rights_name", "权益名称");
		m.put("rights_num", "核销数量");
		m.put("user_name", "会员名称");
		m.put("user_sex", "会员性别");
		m.put("user_mobile", "会员手机");
		m.put("end_time", "核销时间");
		return m;
	}

	private Map<String, String> buildRow(long companyId, RightsLog e, boolean datapassBlock) {
		Map<String, String> row = new LinkedHashMap<>();

		Long shopPk = parseShopIdToLong(e.getShopId());
		WxShops shop = shopPk != null ? wxShopsMapper.selectById(shopPk) : null;
		row.put("shop_name", shop != null && shop.getStoreName() != null ? shop.getStoreName() : "未知");

		String plainMob = displaySalespersonMobile(e.getSalespersonMobile());
		String salespersonName = resolveSalespersonName(companyId, plainMob);
		row.put("salesperson_name", salespersonName);

		row.put("attendant", e.getAttendant() == null ? "" : e.getAttendant());
		row.put("rights_name", e.getRightsName() == null ? "" : e.getRightsName());
		row.put("rights_num", e.getConsumNum() == null ? "" : String.valueOf(e.getConsumNum()));

		long uid = e.getUserId() == null ? 0L : e.getUserId();
		Map<String, Object> mem = memberAccountService.getMemberInfo(uid, companyId);
		Object u = mem.get("username");
		String un = u == null ? "" : String.valueOf(u).trim();
		row.put("user_name", StringUtils.hasText(un) ? un : "未知");
		row.put("user_sex", sexToText(normalizeMemberSex(mem.get("sex"))));

		String mob =
				memberAccountService.resolveMemberMobileForH5Context(
						e.getUserId() == null ? 0L : e.getUserId(), companyId);
		row.put("user_mobile", !StringUtils.hasText(mob) ? "未知" : mob);

		row.put("end_time", formatEndTimeForCsv(e.getEndTime()));

		if (datapassBlock) {
			String userName = row.get("user_name");
			if (userName != null && !"未知".equals(userName)) {
				row.put("user_name", DataMasking.maskTruename(userName));
				row.put("user_mobile", DataMasking.maskMobile(row.get("user_mobile")));
			}
		}

		return row;
	}

	private String resolveSalespersonName(long companyId, String plainMobile) {
		if (!StringUtils.hasText(plainMobile)) {
			return "未知";
		}
		LambdaQueryWrapper<RightsLogsShopSalesperson> spw = new LambdaQueryWrapper<>();
		spw.eq(RightsLogsShopSalesperson::getCompanyId, companyId)
				.eq(RightsLogsShopSalesperson::getMobile, LegacyFixedMobileEncrypt.fixedEncryptMobile(plainMobile))
				.in(RightsLogsShopSalesperson::getSalespersonType, "admin", "verification_clerk")
				.last("LIMIT 1");
		RightsLogsShopSalesperson sp = rightsLogsShopSalespersonMapper.selectOne(spw);
		if (sp != null) {
			String nm = sp.getName();
			if (nm != null) {
				nm = sensitiveFieldEncryptor.decrypt(nm);
			}
			return StringUtils.hasText(nm) ? nm : "未知";
		}
		return "未知";
	}

	private static String displaySalespersonMobile(String stored) {
		if (stored == null) {
			return "";
		}
		try {
			String d = LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(stored);
			return d == null ? "" : d;
		} catch (Exception ex) {
			return stored;
		}
	}

	private static Long parseShopIdToLong(String shopId) {
		if (shopId == null) {
			return null;
		}
		try {
			return Long.parseLong(shopId.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer normalizeMemberSex(Object sx) {
		if (sx == null) {
			return null;
		}
		if (sx instanceof Number n) {
			return n.intValue();
		}
		try {
			String s = String.valueOf(sx).trim();
			if (s.isEmpty()) {
				return null;
			}
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String sexToText(Integer sex) {
		if (sex == null) {
			return "未知";
		}
		return switch (sex) {
			case 1 -> "男";
			case 2 -> "女";
			default -> "未知";
		};
	}

	private static String formatEndTimeForCsv(String endTimeRaw) {
		if (!StringUtils.hasText(endTimeRaw)) {
			return "";
		}
		String t = endTimeRaw.trim();
		try {
			long sec = new BigDecimal(t).longValue();
			ZonedDateTime zdt = Instant.ofEpochSecond(sec).atZone(SHANGHAI);
			return CSV_TIME.format(zdt);
		} catch (NumberFormatException | ArithmeticException e) {
			return "";
		}
	}
}
