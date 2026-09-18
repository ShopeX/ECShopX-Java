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

package cn.shopex.ecshopx.orders.service.rights.export;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RightsExportCsvExportService {

	private static final int PAGE_SIZE = 500;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final RightsMapper rightsMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public RightsExportCsvExportService(
			RightsMapper rightsMapper,
			MembersInfoMapper membersInfoMapper,
			ExportCsvFileService exportCsvFileService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.rightsMapper = rightsMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Optional<Map<String, String>> runExport(
			long companyId, LinkedHashMap<String, Object> queryFilter, boolean datapassBlock) {
		LinkedHashMap<String, String> title = buildTitleRow();
		List<Map<String, String>> rows = new ArrayList<>();

		int pageNum = 1;
		while (true) {
			Page<Rights> page = new Page<>(pageNum, PAGE_SIZE);
			IPage<Rights> result =
					rightsMapper.selectPage(page, RightsExportQuerySupport.toPageWrapper(companyId, queryFilter));
			List<Rights> records = result.getRecords();
			if (records == null || records.isEmpty()) {
				break;
			}
			List<Long> userIds =
					records.stream()
							.map(Rights::getUserId)
							.filter(id -> id != null && id > 0L)
							.distinct()
							.toList();
			Map<Long, MembersInfo> infoByUser = loadMembersInfoByUserIds(companyId, userIds);

			for (Rights r : records) {
				MembersInfo mi = r.getUserId() == null ? null : infoByUser.get(r.getUserId());
				rows.add(buildRow(r, mi, datapassBlock));
			}

			if (records.size() < PAGE_SIZE) {
				break;
			}
			pageNum++;
		}

		if (rows.isEmpty()) {
			return Optional.empty();
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + companyId + "rights";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private Map<Long, MembersInfo> loadMembersInfoByUserIds(long companyId, List<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<MembersInfo> w = new LambdaQueryWrapper<>();
		w.eq(MembersInfo::getCompanyId, companyId);
		w.in(MembersInfo::getUserId, userIds);
		List<MembersInfo> list = membersInfoMapper.selectList(w);
		Map<Long, MembersInfo> out = new HashMap<>();
		for (MembersInfo mi : list) {
			out.put(mi.getUserId(), mi);
		}
		return out;
	}

	private LinkedHashMap<String, String> buildTitleRow() {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("rights_id", "权益ID");
		m.put("user_id", "用户ID");
		m.put("user_nickname", "昵称");
		m.put("sex", "性别");
		m.put("mobile", "手机号");
		m.put("rights_name", "权益名称");
		m.put("rights_subname", "权益副标题");
		m.put("rights_from", "权益来源");
		m.put("operator_desc", "操作员信息");
		m.put("status", "状态");
		m.put("order_id", "订单号");
		m.put("total_num", "总次数");
		m.put("total_consum_num", "已消耗次数");
		m.put("start_time", "开始时间");
		m.put("end_time", "结束时间");
		m.put("can_reservation", "可预约");
		m.put("is_not_limit_num", "核销限制");
		m.put("label_infos", "物料信息");
		return m;
	}

	private Map<String, String> buildRow(Rights r, MembersInfo mi, boolean datapassBlock) {
		Map<String, String> row = new LinkedHashMap<>();
		row.put("rights_id", r.getRightsId() == null ? "" : String.valueOf(r.getRightsId()));
		row.put("user_id", r.getUserId() == null ? "" : String.valueOf(r.getUserId()));

		String nickname = "";
		String sexText = "未知";
		if (mi != null) {
			String rawNick = firstNonBlank(mi.getName(), mi.getUsername());
			nickname = decryptMaybe(rawNick);
			if (datapassBlock) {
				nickname = DataMasking.maskTruename(nickname);
			}
			sexText = sexToText(mi.getSex());
		}
		row.put("user_nickname", nickname == null ? "" : nickname);
		row.put("sex", sexText);

		String mobilePlain = decryptMaybe(r.getMobile());
		if (datapassBlock) {
			mobilePlain = DataMasking.maskUname(mobilePlain);
		}
		row.put("mobile", mobilePlain == null ? "" : mobilePlain);

		row.put("rights_name", nullToEmpty(r.getRightsName()));
		row.put("rights_subname", nullToEmpty(r.getRightsSubname()));
		row.put("rights_from", nullToEmpty(r.getRightsFrom()));

		String opDesc = nullToEmpty(r.getOperatorDesc());
		if (datapassBlock && StringUtils.hasText(opDesc)) {
			String masked = DataMasking.maskTruename(opDesc);
			opDesc = masked != null ? masked : "";
		}
		row.put("operator_desc", opDesc);

		row.put("status", nullToEmpty(r.getStatus()));
		row.put("order_id", r.getOrderId() == null ? "" : String.valueOf(r.getOrderId()));
		row.put("total_num", r.getTotalNum() == null ? "" : String.valueOf(r.getTotalNum()));
		row.put("total_consum_num", r.getTotalConsumNum() == null ? "" : String.valueOf(r.getTotalConsumNum()));
		row.put("start_time", formatEpochSeconds(r.getStartTime()));
		row.put("end_time", formatEpochSeconds(r.getEndTime()));
		row.put("can_reservation", boolYesNo(r.getCanReservation()));

		row.put("is_not_limit_num", formatIsNotLimitNum(r.getIsNotLimitNum()));
		row.put("label_infos", nullToEmpty(r.getLabelInfos()));
		return row;
	}

	private static String formatIsNotLimitNum(Integer v) {
		if (v == null) {
			return "";
		}
		if (v == 1) {
			return "不限制";
		}
		if (v == 2) {
			return "限制";
		}
		return String.valueOf(v);
	}

	private static String boolYesNo(Boolean b) {
		if (b == null) {
			return "";
		}
		return Boolean.TRUE.equals(b) ? "是" : "否";
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

	private String formatEpochSeconds(Integer epoch) {
		if (epoch == null) {
			return "";
		}
		ZonedDateTime zdt = Instant.ofEpochSecond(epoch.longValue()).atZone(SHANGHAI);
		return CSV_TIME.format(zdt);
	}

	private String decryptMaybe(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		try {
			return sensitiveFieldEncryptor.decrypt(raw);
		} catch (Exception e) {
			return raw;
		}
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a.trim();
		}
		if (StringUtils.hasText(b)) {
			return b.trim();
		}
		return "";
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
