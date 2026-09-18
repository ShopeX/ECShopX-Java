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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.selfservice.domain.UserDailyRecord;
import cn.shopex.ecshopx.selfservice.mapper.UserDailyRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserDailyRecordAllUserListService {

	private static final int PAGE = 1;
	private static final int PAGE_SIZE = 20;

	private final UserDailyRecordPhysicalSettingService userDailyRecordPhysicalSettingService;
	private final UserDailyRecordMapper userDailyRecordMapper;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public UserDailyRecordAllUserListService(
			UserDailyRecordPhysicalSettingService userDailyRecordPhysicalSettingService,
			UserDailyRecordMapper userDailyRecordMapper,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.userDailyRecordPhysicalSettingService = userDailyRecordPhysicalSettingService;
		this.userDailyRecordMapper = userDailyRecordMapper;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getAllUserList(long companyId, String formType, String mobile, String username) {
		Map<String, Object> result = new LinkedHashMap<>();
		List<Map<String, Object>> listdata = new ArrayList<>();
		List<Map<String, String>> colstitlePlaceholder = new ArrayList<>();
		result.put("list", listdata);
		result.put("total_count", 0);
		result.put("colstitle", colstitlePlaceholder);

		long tempId = userDailyRecordPhysicalSettingService.getTempIdForRead(companyId, formType);
		if (companyId <= 0L || tempId == 0L) {
			return result;
		}

		List<Long> filterUserIds = null;
		List<Members> mobileFilterMembers = null;
		List<MembersInfo> usernameFilterInfos = null;

		if (StringUtils.hasText(mobile)) {
			String encMobile = sensitiveFieldEncryptor.encrypt(mobile.trim());
			List<Members> mobileRows =
					membersMapper.selectList(new LambdaQueryWrapper<Members>()
							.eq(Members::getCompanyId, companyId)
							.eq(Members::getMobile, encMobile));
			if (mobileRows.isEmpty()) {
				return result;
			}
			filterUserIds = mobileRows.stream().map(Members::getUserId).toList();
			mobileFilterMembers = mobileRows;
		}

		if (StringUtils.hasText(username)) {
			String encUsername = sensitiveFieldEncryptor.encrypt(username.trim());
			List<MembersInfo> usernameRows =
					membersInfoMapper.selectList(new LambdaQueryWrapper<MembersInfo>()
							.eq(MembersInfo::getCompanyId, companyId)
							.eq(MembersInfo::getUsername, encUsername));
			if (usernameRows.isEmpty()) {
				return result;
			}
			filterUserIds = usernameRows.stream().map(MembersInfo::getUserId).toList();
			usernameFilterInfos = usernameRows;
			mobileFilterMembers = null;
		}

		long count = userDailyRecordMapper.countDistinctUserIds(companyId, tempId, filterUserIds);
		if (count == 0L) {
			return result;
		}

		int offset = PAGE_SIZE * (PAGE - 1);
		List<UserDailyRecord> records =
				userDailyRecordMapper.selectGroupByUserIdPage(companyId, tempId, filterUserIds, offset, PAGE_SIZE);
		if (records.isEmpty()) {
			return result;
		}

		List<Long> distinctRecordUserIds = records.stream().map(UserDailyRecord::getUserId).distinct().toList();

		Map<Long, String> mobileByUser = new LinkedHashMap<>();
		Map<Long, String> usernameByUser = new LinkedHashMap<>();

		if (mobileFilterMembers != null) {
			for (Members m : mobileFilterMembers) {
				mobileByUser.put(m.getUserId(), m.getMobile() != null ? m.getMobile() : "");
			}
		} else {
			List<Members> mBatch =
					membersMapper.selectList(new LambdaQueryWrapper<Members>()
							.eq(Members::getCompanyId, companyId)
							.in(Members::getUserId, distinctRecordUserIds)
							.select(Members::getUserId, Members::getMobile));
			for (Members m : mBatch) {
				mobileByUser.put(m.getUserId(), m.getMobile() != null ? m.getMobile() : "");
			}
		}

		if (usernameFilterInfos != null) {
			for (MembersInfo mi : usernameFilterInfos) {
				usernameByUser.put(mi.getUserId(), mi.getUsername() != null ? mi.getUsername() : "");
			}
		} else {
			List<MembersInfo> iBatch =
					membersInfoMapper.selectList(new LambdaQueryWrapper<MembersInfo>()
							.eq(MembersInfo::getCompanyId, companyId)
							.in(MembersInfo::getUserId, distinctRecordUserIds)
							.select(MembersInfo::getUserId, MembersInfo::getUsername));
			for (MembersInfo mi : iBatch) {
				usernameByUser.put(mi.getUserId(), mi.getUsername() != null ? mi.getUsername() : "");
			}
		}

		LinkedHashMap<Integer, Map<String, String>> colTitleMerged = new LinkedHashMap<>();

		for (UserDailyRecord rec : records) {
			Long userId = rec.getUserId();
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", rec.getId());
			row.put("company_id", rec.getCompanyId());
			row.put("user_id", userId);
			row.put("record_date", rec.getRecordDate());
			row.put("shop_id", rec.getShopId());
			row.put("created", rec.getCreated());
			row.put("updated", rec.getUpdated());
			row.put("operator_id", rec.getOperatorId());
			row.put("operator", rec.getOperator());
			row.put("temp_id", rec.getTempId());

			JsonNode arr = readFormDataArray(rec.getFormData());
			for (int i = 0; i < arr.size(); i++) {
				JsonNode elem = arr.get(i);
				if (!elem.isObject()) {
					continue;
				}
				if (!keyIndexMatchesLooseTrueString(elem)) {
					continue;
				}
				String fieldName = textField(elem, "field_name");
				String fieldTitle = textField(elem, "field_title");
				String fieldValue = valueFieldAsString(elem, "field_value");
				LinkedHashMap<String, String> titleEntry = new LinkedHashMap<>();
				titleEntry.put("prop", fieldName);
				titleEntry.put("label", fieldTitle);
				colTitleMerged.put(i, titleEntry);
				row.put(fieldName, fieldValue);
			}

			row.put("mobile", mobileByUser.getOrDefault(userId, ""));
			row.put("username", usernameByUser.getOrDefault(userId, ""));
			listdata.add(row);
		}

		result.put("total_count", count);
		result.put("list", listdata);
		result.put("colstitle", new ArrayList<>(colTitleMerged.values()));
		return result;
	}

	private JsonNode readFormDataArray(String formData) {
		try {
			if (formData == null || formData.isBlank()) {
				return objectMapper.createArrayNode();
			}
			JsonNode parsed = objectMapper.readTree(formData);
			if (parsed.isArray()) {
				return parsed;
			}
			return objectMapper.createArrayNode();
		} catch (JsonProcessingException ex) {
			return objectMapper.createArrayNode();
		}
	}

	private static String textField(JsonNode elem, String name) {
		JsonNode n = elem.get(name);
		if (n == null || n.isNull()) {
			return "";
		}
		if (n.isValueNode()) {
			return n.asText("");
		}
		return "";
	}

	private static String valueFieldAsString(JsonNode elem, String name) {
		JsonNode n = elem.get(name);
		if (n == null || n.isNull()) {
			return "";
		}
		if (n.isObject() || n.isArray()) {
			return n.toString();
		}
		return n.asText("");
	}

	private static boolean keyIndexMatchesLooseTrueString(JsonNode elem) {
		JsonNode raw = elem.get("key_index");
		Object left = coalesceKeyIndexLeft(raw);
		return leftOperandLooseEqualsTrueString(left);
	}

	private static Object coalesceKeyIndexLeft(JsonNode raw) {
		if (raw == null || raw.isNull() || raw.isMissingNode()) {
			return Boolean.FALSE;
		}
		if (raw.isBoolean()) {
			return raw.booleanValue();
		}
		if (raw.isTextual()) {
			return raw.asText();
		}
		if (raw.isNumber()) {
			if (raw.isIntegralNumber()) {
				return raw.longValue();
			}
			BigDecimal dec = raw.decimalValue();
			return dec;
		}
		return new Object();
	}

	private static boolean leftOperandLooseEqualsTrueString(Object left) {
		if (Boolean.FALSE.equals(left)) {
			return false;
		}
		if (Boolean.TRUE.equals(left)) {
			return true;
		}
		if (left instanceof String s) {
			return "true".equals(s);
		}
		if (left instanceof Long l) {
			return l == 0L;
		}
		if (left instanceof Integer i) {
			return i == 0;
		}
		if (left instanceof Short s) {
			return s == 0;
		}
		if (left instanceof Byte b) {
			return b == 0;
		}
		if (left instanceof Double d) {
			return d == 0.0;
		}
		if (left instanceof Float f) {
			return f == 0.0f;
		}
		if (left instanceof BigDecimal bd) {
			return bd.compareTo(BigDecimal.ZERO) == 0;
		}
		if (left instanceof BigInteger bi) {
			return BigInteger.ZERO.equals(bi);
		}
		if (left instanceof Number n) {
			return n.longValue() == 0L;
		}
		return false;
	}
}
