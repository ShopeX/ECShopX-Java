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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.RightsLog;
import cn.shopex.ecshopx.orders.domain.RightsLogsShopSalesperson;
import cn.shopex.ecshopx.orders.mapper.RightsLogMapper;
import cn.shopex.ecshopx.orders.mapper.RightsLogsShopSalespersonMapper;
import cn.shopex.ecshopx.orders.service.admin.support.RightsLogsQueryParseSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RightsLogsListService {

	private final RightsLogMapper rightsLogMapper;
	private final WxShopsMapper wxShopsMapper;
	private final RightsLogsShopSalespersonMapper rightsLogsShopSalespersonMapper;
	private final MemberAccountService memberAccountService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public RightsLogsListService(
			RightsLogMapper rightsLogMapper,
			WxShopsMapper wxShopsMapper,
			RightsLogsShopSalespersonMapper rightsLogsShopSalespersonMapper,
			MemberAccountService memberAccountService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.rightsLogMapper = rightsLogMapper;
		this.wxShopsMapper = wxShopsMapper;
		this.rightsLogsShopSalespersonMapper = rightsLogsShopSalespersonMapper;
		this.memberAccountService = memberAccountService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public void applyCommonRightsLogFilters(
			long companyId,
			String mobileParam,
			String nameParam,
			String shopIdParam,
			String tsb,
			String tse,
			LambdaQueryWrapper<RightsLog> w) {
		w.eq(RightsLog::getCompanyId, companyId);

		int mobileInt = RightsLogsQueryParseSupport.parseIntLooseAsInt(mobileParam);
		if (mobileInt != 0) {
			String enc = LegacyFixedMobileEncrypt.fixedEncryptMobile(String.valueOf(mobileInt));
			w.eq(RightsLog::getSalespersonMobile, enc);
		}

		int nameInt = RightsLogsQueryParseSupport.parseIntLooseAsInt(nameParam);
		if (nameInt != 0) {
			w.eq(RightsLog::getAttendant, String.valueOf(nameInt));
		}

		int shopIdInt = RightsLogsQueryParseSupport.parseIntLooseAsInt(shopIdParam);
		if (shopIdInt != 0) {
			w.eq(RightsLog::getShopId, String.valueOf(shopIdInt));
		}

		if (RightsLogsQueryParseSupport.hasTimeToken(tsb) && !RightsLogsQueryParseSupport.isNumericUnixSecondsToken(tsb)) {
			throw new ResourceException("导出有误，日期时间参数有误");
		}
		if (RightsLogsQueryParseSupport.hasTimeToken(tse) && !RightsLogsQueryParseSupport.isNumericUnixSecondsToken(tse)) {
			throw new ResourceException("导出有误，日期时间参数有误");
		}
		if (RightsLogsQueryParseSupport.hasTimeToken(tsb)) {
			w.apply("end_time >= {0}", tsb.trim());
		}
		if (RightsLogsQueryParseSupport.hasTimeToken(tse)) {
			w.apply("end_time < {0}", tse.trim());
		}
	}

	public void applyRightsLogFiltersFromJobMap(
			long companyId, LinkedHashMap<String, Object> work, LambdaQueryWrapper<RightsLog> w) {
		w.eq(RightsLog::getCompanyId, companyId);

		if (work.containsKey("salesperson_mobile")) {
			String sm = String.valueOf(work.get("salesperson_mobile"));
			if (StringUtils.hasText(sm)) {
				w.eq(RightsLog::getSalespersonMobile, sm.trim());
			}
		}
		if (work.containsKey("attendant")) {
			w.eq(RightsLog::getAttendant, String.valueOf(work.get("attendant")));
		}
		if (work.containsKey("shop_id")) {
			w.eq(RightsLog::getShopId, String.valueOf(work.get("shop_id")));
		}

		Object tsbObj = work.get("time_start_begin");
		Object tseObj = work.get("time_start_end");
		String tsb = tsbObj == null ? "" : String.valueOf(tsbObj).trim();
		String tse = tseObj == null ? "" : String.valueOf(tseObj).trim();

		if (RightsLogsQueryParseSupport.hasTimeToken(tsb) && RightsLogsQueryParseSupport.isNumericUnixSecondsToken(tsb)) {
			w.apply("end_time >= {0}", tsb);
		}
		if (RightsLogsQueryParseSupport.hasTimeToken(tse) && RightsLogsQueryParseSupport.isNumericUnixSecondsToken(tse)) {
			w.apply("end_time < {0}", tse);
		}
	}

	public Map<String, Object> getLogsList(long companyId, HttpServletRequest request) {
		int pageNo = parsePageNo(request.getParameter("page"));
		int pageSize = parsePageSize(request.getParameter("pageSize"));

		String mobileParam = request.getParameter("mobile");
		String nameParam = request.getParameter("name");
		String userIdParam = request.getParameter("user_id");
		String shopIdParam = request.getParameter("shop_id");
		String tsb = request.getParameter("time_start_begin");
		String tse = request.getParameter("time_start_end");

		LambdaQueryWrapper<RightsLog> w = new LambdaQueryWrapper<>();
		applyCommonRightsLogFilters(companyId, mobileParam, nameParam, shopIdParam, tsb, tse, w);

		int userIdInt = RightsLogsQueryParseSupport.parseIntLooseAsInt(userIdParam);
		if (userIdInt != 0) {
			w.eq(RightsLog::getUserId, (long) userIdInt);
		}

		w.orderByDesc(RightsLog::getCreated);

		long totalCount = rightsLogMapper.selectCount(w);
		Page<RightsLog> page = new Page<>(pageNo, pageSize, false);
		rightsLogMapper.selectPage(page, w);

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (RightsLog e : page.getRecords()) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("rights_log_id", e.getRightsLogId());
			row.put("rights_id", e.getRightsId());
			row.put("company_id", e.getCompanyId());
			row.put("user_id", e.getUserId());
			row.put("shop_id", e.getShopId());
			row.put("rights_name", e.getRightsName());
			row.put("rights_subname", e.getRightsSubname());
			row.put("consum_num", e.getConsumNum());
			row.put("attendant", e.getAttendant());
			row.put("salesperson_mobile", displaySalespersonMobile(e.getSalespersonMobile()));
			row.put("end_time", e.getEndTime());
			row.put("created", e.getCreated());

			Long shopPk = parseShopIdToLong(e.getShopId());
			WxShops shop = shopPk != null ? wxShopsMapper.selectById(shopPk) : null;
			row.put("shop_name", shop != null && shop.getStoreName() != null ? shop.getStoreName() : "未知");

			String plainMob = (String) row.get("salesperson_mobile");
			LambdaQueryWrapper<RightsLogsShopSalesperson> spw = new LambdaQueryWrapper<>();
			spw.eq(RightsLogsShopSalesperson::getCompanyId, companyId)
					.eq(RightsLogsShopSalesperson::getMobile, LegacyFixedMobileEncrypt.fixedEncryptMobile(plainMob))
					.in(RightsLogsShopSalesperson::getSalespersonType, "admin", "verification_clerk")
					.last("LIMIT 1");
			RightsLogsShopSalesperson sp = rightsLogsShopSalespersonMapper.selectOne(spw);
			if (sp != null) {
				String nm = sp.getName();
				if (nm != null) {
					nm = sensitiveFieldEncryptor.decrypt(nm);
				}
				row.put("name", StringUtils.hasText(nm) ? nm : "未知");
			} else {
				row.put("name", "未知");
			}

			long uid = e.getUserId() == null ? 0L : e.getUserId();
			Map<String, Object> mem = memberAccountService.getMemberInfo(uid, companyId);

			Object u = mem.get("username");
			String un = u == null ? "" : String.valueOf(u).trim();
			row.put("user_name", StringUtils.hasText(un) ? un : "未知");

			if (!mem.containsKey("sex")) {
				row.put("user_sex", "未知");
			} else {
				Object sx = mem.get("sex");
				row.put("user_sex", sx == null ? "未知" : String.valueOf(sx));
			}

			String mob =
					memberAccountService.resolveMemberMobileForH5Context(
							e.getUserId() == null ? 0L : e.getUserId(), companyId);
			row.put("user_mobile", !StringUtils.hasText(mob) ? "未知" : mob);

			listMaps.add(row);
		}

		if (!listMaps.isEmpty() && isDatapassBlocked(request)) {
			for (Map<String, Object> row : listMaps) {
				String originalUserName = String.valueOf(row.get("user_name"));
				row.put("salesperson_mobile", DataMasking.maskMobile(String.valueOf(row.get("salesperson_mobile"))));
				if (!"未知".equals(originalUserName)) {
					row.put("user_name", DataMasking.maskTruename(String.valueOf(row.get("user_name"))));
					row.put("user_mobile", DataMasking.maskMobile(String.valueOf(row.get("user_mobile"))));
				}
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", listMaps);
		data.put("total_count", totalCount);
		return data;
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

	private static int parsePageNo(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return 1;
		}
		try {
			int p = Integer.parseInt(raw.trim());
			return p < 1 ? 1 : p;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parsePageSize(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return 20;
		}
		try {
			int ps = Integer.parseInt(raw.trim());
			if (ps <= 0) {
				return 20;
			}
			if (ps > 1000) {
				return 1000;
			}
			return ps;
		} catch (NumberFormatException e) {
			return 20;
		}
	}

	private static boolean isDatapassBlocked(HttpServletRequest request) {
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Number n && n.intValue() != 0) {
			return true;
		}
		if (Boolean.TRUE.equals(attr)) {
			return true;
		}
		if (attr != null) {
			String t = attr.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return true;
			}
		}
		String p = request.getParameter("x-datapass-block");
		if (p == null || p.trim().isEmpty() || "0".equals(p.trim()) || "false".equalsIgnoreCase(p.trim())) {
			return false;
		}
		return true;
	}
}
