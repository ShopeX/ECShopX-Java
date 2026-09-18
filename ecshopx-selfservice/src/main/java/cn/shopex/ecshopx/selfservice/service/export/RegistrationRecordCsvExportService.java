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

package cn.shopex.ecshopx.selfservice.service.export;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.selfservice.domain.FormTemplate;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.FormTemplateMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationRecordCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(RegistrationRecordCsvExportService.class);
	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);
	private static final DateTimeFormatter ROW_DT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(CN);
	private static final int BATCH = 500;
	private static final String EXPORT_TYPE = "selform_registration_record";

	private static final String MSG_STATUS_PENDING = "selfservice.registration_record.status_pending";
	private static final String MSG_STATUS_PASSED = "selfservice.registration_record.status_passed";
	private static final String MSG_STATUS_REJECTED = "selfservice.registration_record.status_rejected";
	private static final String MSG_STATUS_VERIFIED = "selfservice.registration_record.status_verified";
	private static final String MSG_STATUS_CANCELED = "selfservice.registration_record.status_canceled";

	private final RegistrationRecordExportFilterSupport registrationRecordExportFilterSupport;
	private final RegistrationRecordMapper registrationRecordMapper;
	private final RegistrationActivityMapper registrationActivityMapper;
	private final FormTemplateMapper formTemplateMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;

	public RegistrationRecordCsvExportService(
			RegistrationRecordExportFilterSupport registrationRecordExportFilterSupport,
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationActivityMapper registrationActivityMapper,
			FormTemplateMapper formTemplateMapper,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			MessageSource messageSource,
			ObjectMapper objectMapper) {
		this.registrationRecordExportFilterSupport = registrationRecordExportFilterSupport;
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationActivityMapper = registrationActivityMapper;
		this.formTemplateMapper = formTemplateMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	public void runExport(RegistrationRecordExportJobContext ctx) {
		long total = registrationRecordExportFilterSupport.countForExport(
				ctx.companyId(),
				ctx.activityId(),
				ctx.mobilePlain(),
				ctx.startCreatedInclusive(),
				ctx.endCreatedInclusive(),
				sensitiveFieldEncryptor);
		if (total == 0L) {
			return;
		}

		RegistrationActivity activity =
				registrationActivityMapper.selectOne(
						new LambdaQueryWrapper<RegistrationActivity>()
								.eq(RegistrationActivity::getActivityId, ctx.activityId())
								.eq(RegistrationActivity::getCompanyId, ctx.companyId()));
		String activityName = activity != null && StringUtils.hasText(activity.getActivityName())
				? activity.getActivityName()
				: "";

		LinkedHashMap<String, String> titles = buildBaseTitles();
		appendDynamicTitles(activity, titles);

		boolean maskSensitive = StringUtils.hasText(ctx.datapassBlock());
		int pages = (int) Math.ceil(total / (double) BATCH);
		List<Map<String, String>> allRows = new ArrayList<>();
		for (int pageNo = 1; pageNo <= pages; pageNo++) {
			LambdaQueryWrapper<RegistrationRecord> w =
					registrationRecordExportFilterSupport.buildExportWrapper(
							ctx.companyId(),
							ctx.activityId(),
							ctx.mobilePlain(),
							ctx.startCreatedInclusive(),
							ctx.endCreatedInclusive(),
							sensitiveFieldEncryptor);
			w.orderByDesc(RegistrationRecord::getCreated);
			Page<RegistrationRecord> p = new Page<>(pageNo, BATCH, false);
			Page<RegistrationRecord> result = registrationRecordMapper.selectPage(p, w);
			Map<Long, String> formIdToTemName =
					loadTemNamesByFormIds(ctx.companyId(), collectDistinctFormIds(result.getRecords()));
			for (RegistrationRecord rec : result.getRecords()) {
				allRows.add(
						buildRow(rec, activityName, titles, maskSensitive, ctx.locale(), formIdToTemName));
			}
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + "_registration_record";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBaseName, titles, allRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		exportLogCreateService.createFinishLog(
				ctx.companyId(),
				ctx.operatorId(),
				0L,
				ctx.supplierId(),
				EXPORT_TYPE,
				uploaded.getOrDefault("filename", fileBaseName + ".csv"),
				uploaded.get("url"),
				Instant.now().getEpochSecond());
	}

	private LinkedHashMap<String, String> buildBaseTitles() {
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("record_no", "报名编号");
		titles.put("mobile", "会员手机号");
		titles.put("activity_name", "活动名称");
		titles.put("group_no", "活动分组编码");
		titles.put("get_points", "获取积分");
		titles.put("is_white_list", "进白名单");
		titles.put("tem_name", "来源表单");
		titles.put("status_name", "状态");
		titles.put("reason", "拒绝原因");
		titles.put("created", "申请时间");
		return titles;
	}

	private void appendDynamicTitles(RegistrationActivity activity, LinkedHashMap<String, String> titles) {
		if (activity == null || activity.getTempId() == null || activity.getTempId() <= 0L) {
			return;
		}
		FormTemplate tpl = formTemplateMapper.selectById(activity.getTempId());
		if (tpl == null || !StringUtils.hasText(tpl.getContent())) {
			return;
		}
		try {
			JsonNode root = objectMapper.readTree(tpl.getContent());
			if (!root.isArray()) {
				return;
			}
			for (JsonNode v : root) {
				JsonNode formdata = v.get("formdata");
				if (formdata == null || !formdata.isArray()) {
					continue;
				}
				for (JsonNode vv : formdata) {
					JsonNode idNode = vv.get("id");
					if (idNode == null || idNode.isNull()) {
						continue;
					}
					String colKey = "row" + idNode.asText();
					JsonNode ft = vv.get("field_title");
					String header = ft == null || ft.isNull() ? "" : replaceSpecialChar(ft.asText());
					titles.put(colKey, header);
				}
			}
		} catch (Exception e) {
			log.debug("registration export: skip dynamic titles, content parse failed", e);
		}
	}

	private static Set<Long> collectDistinctFormIds(List<RegistrationRecord> records) {
		Set<Long> ids = new HashSet<>();
		for (RegistrationRecord rec : records) {
			Long fid = rec.getFormId();
			if (fid != null && fid > 0L) {
				ids.add(fid);
			}
		}
		return ids;
	}

	private Map<Long, String> loadTemNamesByFormIds(long companyId, Set<Long> formIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (formIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<FormTemplate> ftW = new LambdaQueryWrapper<>();
		ftW.in(FormTemplate::getId, formIds);
		ftW.eq(FormTemplate::getCompanyId, companyId);
		ftW.select(FormTemplate::getId, FormTemplate::getTemName);
		for (FormTemplate t : formTemplateMapper.selectList(ftW)) {
			if (t.getId() == null) {
				continue;
			}
			String name = t.getTemName();
			out.put(t.getId(), name != null ? name : "");
		}
		return out;
	}

	private Map<String, String> buildRow(
			RegistrationRecord rec,
			String activityName,
			LinkedHashMap<String, String> titles,
			boolean maskSensitive,
			Locale locale,
			Map<Long, String> formIdToTemName) {
		Map<String, String> dynamicAnswers = extractDynamicAnswers(rec.getContent(), maskSensitive);

		Map<String, String> row = new LinkedHashMap<>();
		for (String colKey : titles.keySet()) {
			String cell;
			switch (colKey) {
				case "record_no" -> cell = replaceSpecialChar(formatRecordNo(rec.getRecordNo()));
				case "mobile" -> {
					String plain =
							objectToString(sensitiveFieldEncryptor.decrypt(rec.getMobile() == null ? "" : rec.getMobile()));
					if (maskSensitive) {
						plain = DataMasking.maskMobile(plain);
					}
					cell = replaceSpecialChar(plain);
				}
				case "activity_name" -> cell = replaceSpecialChar(activityName);
				case "group_no" -> cell = replaceSpecialChar(rec.getGroupNo() == null ? "" : rec.getGroupNo());
				case "get_points" -> {
					Long gp = rec.getGetPoints();
					cell = replaceSpecialChar(gp == null ? "" : gp.toString());
				}
				case "is_white_list" -> {
					Long iw = rec.getIsWhiteList();
					cell = replaceSpecialChar(iw == null ? "" : iw.toString());
				}
				case "tem_name" -> {
					Long fid = rec.getFormId();
					String temDisplay =
							fid == null || fid <= 0L
									? ""
									: formIdToTemName.getOrDefault(fid, String.valueOf(fid));
					cell = replaceSpecialChar(temDisplay);
				}
				case "status_name" -> cell = replaceSpecialChar(statusName(rec.getStatus(), locale));
				case "reason" -> cell = replaceSpecialChar(rec.getReason() == null ? "" : rec.getReason());
				case "created" -> {
					int sec = rec.getCreated() != null ? rec.getCreated() : 0;
					cell = ROW_DT.format(Instant.ofEpochSecond(sec));
				}
				default -> {
					if (colKey.startsWith("row")) {
						cell = replaceSpecialChar(dynamicAnswers.getOrDefault(colKey, ""));
					} else {
						cell = replaceSpecialChar("");
					}
				}
			}
			row.put(colKey, cell);
		}
		return row;
	}

	private String statusName(String status, Locale locale) {
		if (!StringUtils.hasText(status)) {
			return "";
		}
		String key =
				switch (status.trim()) {
					case "pending" -> MSG_STATUS_PENDING;
					case "passed" -> MSG_STATUS_PASSED;
					case "rejected" -> MSG_STATUS_REJECTED;
					case "verified" -> MSG_STATUS_VERIFIED;
					case "canceled" -> MSG_STATUS_CANCELED;
					default -> null;
				};
		if (key == null) {
			return status;
		}
		return messageSource.getMessage(key, null, locale);
	}

	private Map<String, String> extractDynamicAnswers(String contentJson, boolean maskSensitive) {
		Map<String, String> out = new LinkedHashMap<>();
		JsonNode root;
		try {
			if (!StringUtils.hasText(contentJson)) {
				root = objectMapper.createArrayNode();
			} else {
				root = objectMapper.readTree(contentJson);
			}
		} catch (Exception e) {
			root = objectMapper.createArrayNode();
		}
		if (!root.isArray()) {
			return out;
		}
		for (JsonNode card : root) {
			JsonNode formdata = card.get("formdata");
			if (formdata == null || !formdata.isArray()) {
				continue;
			}
			for (JsonNode line : formdata) {
				JsonNode idNode = line.get("id");
				if (idNode == null || idNode.isNull()) {
					continue;
				}
				String rowKey = "row" + idNode.asText();
				String fieldName = "";
				JsonNode fn = line.get("field_name");
				if (fn != null && !fn.isNull()) {
					fieldName = fn.asText("");
				}
				String answer = formatAnswerCell(line.get("answer"));
				if (maskSensitive) {
					answer = maskFormAnswer(fieldName, answer);
				}
				out.put(rowKey, answer);
			}
		}
		return out;
	}

	private static String formatAnswerCell(JsonNode answerNode) {
		if (answerNode == null || answerNode.isNull()) {
			return "无";
		}
		if (answerNode.isArray()) {
			List<String> parts = new ArrayList<>();
			for (JsonNode n : answerNode) {
				parts.add(textualAnswerPart(n));
			}
			return String.join(";", parts);
		}
		String s = textualAnswerPart(answerNode);
		return StringUtils.hasText(s) ? s : "无";
	}

	private static String textualAnswerPart(JsonNode n) {
		if (n == null || n.isNull()) {
			return "";
		}
		if (n.isTextual()) {
			return n.asText();
		}
		if (n.isNumber()) {
			return n.asText();
		}
		if (n.isBoolean()) {
			return n.asBoolean() ? "1" : "0";
		}
		return n.toString();
	}

	private static String maskFormAnswer(String fieldName, String answer) {
		if (answer == null) {
			return "";
		}
		return switch (fieldName) {
			case "username" -> DataMasking.maskTruename(answer);
			case "mobile" -> DataMasking.maskMobile(answer);
			case "birthday" -> DataMasking.maskBirthday(answer);
			case "bankcard" -> DataMasking.maskBankcard(answer);
			case "idcard" -> DataMasking.maskIdcard(answer);
			case "address" -> DataMasking.maskAddress(answer);
			case "detailedaddress" -> DataMasking.maskDetailedAddress(answer);
			default -> answer;
		};
	}

	public String maskMobileLoose(String s) {
		return DataMasking.maskMobile(s);
	}

	private static String formatRecordNo(Long recordNo) {
		long n = recordNo != null ? recordNo : 0L;
		String s = Long.toString(n);
		if (s.length() >= 4) {
			return s;
		}
		return "0".repeat(4 - s.length()) + s;
	}

	private static String replaceSpecialChar(String raw) {
		if (raw == null || raw.isEmpty()) {
			return raw;
		}
		String str = raw;
		if (isNumericIntvalStyle(str)) {
			str = str + "\t";
		}
		return str.replace("\r", "；")
				.replace("\n", "；")
				.replace(",", "；")
				.replace("\"", "；");
	}

	private static boolean isNumericIntvalStyle(String s) {
		try {
			new BigDecimal(s.trim());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static String objectToString(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
