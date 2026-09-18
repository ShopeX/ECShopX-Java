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

package cn.shopex.ecshopx.companys.service.setting;

import cn.shopex.ecshopx.companys.domain.Setting;
import cn.shopex.ecshopx.companys.mapper.SettingMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyBaseSettingService {

	private final SettingMapper settingMapper;
	private final ObjectMapper objectMapper;

	public CompanyBaseSettingService(SettingMapper settingMapper, ObjectMapper objectMapper) {
		this.settingMapper = settingMapper;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> upsertCompanySetting(long companyId, Map<String, Object> presentKeysOnly) {
		Setting row = settingMapper.selectById(Long.valueOf(companyId));
		if (row != null) {
			applyPatchToEntity(row, presentKeysOnly);
			row.setUpdated(Integer.valueOf((int) (System.currentTimeMillis() / 1000L)));
			int affected = settingMapper.updateById(row);
			if (affected == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			Setting fresh = settingMapper.selectById(Long.valueOf(companyId));
			if (fresh == null) {
				throw new ResourceException("未查询到更新数据");
			}
			return toApiMap(fresh);
		}
		Setting ins = new Setting();
		ins.setCompanyId(Long.valueOf(companyId));
		applyPatchToEntity(ins, presentKeysOnly);
		int now = (int) (System.currentTimeMillis() / 1000L);
		ins.setCreated(Integer.valueOf(now));
		ins.setUpdated(Integer.valueOf(now));
		settingMapper.insert(ins);
		Setting fresh = settingMapper.selectById(Long.valueOf(companyId));
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toApiMap(fresh);
	}

	public Object getFapiaoset(long companyId) {
		Setting row = settingMapper.selectById(Long.valueOf(companyId));
		if (row == null) {
			return Collections.emptyList();
		}
		String raw = row.getFapiaoConfig();
		if (raw == null || raw.isBlank()) {
			return Collections.emptyList();
		}
		return decodeFapiaoConfigForGet(raw);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> saveFapiaoset(long companyId, Map<String, Object> dataIn) {
		Setting row = settingMapper.selectById(Long.valueOf(companyId));
		if (row != null) {
			applyFapiaoPatchToEntity(row, dataIn);
			row.setUpdated(Integer.valueOf((int) (System.currentTimeMillis() / 1000L)));
			int affected = settingMapper.updateById(row);
			if (affected == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			Setting fresh = settingMapper.selectById(Long.valueOf(companyId));
			if (fresh == null) {
				throw new ResourceException("未查询到更新数据");
			}
			return toApiMap(fresh);
		}
		Setting ins = new Setting();
		ins.setCompanyId(Long.valueOf(companyId));
		applyFapiaoPatchToEntity(ins, dataIn);
		int now = (int) (System.currentTimeMillis() / 1000L);
		ins.setCreated(Integer.valueOf(now));
		ins.setUpdated(Integer.valueOf(now));
		settingMapper.insert(ins);
		Setting fresh = settingMapper.selectById(Long.valueOf(companyId));
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toApiMap(fresh);
	}

	public Map<String, Object> getSetting(
			long companyId,
			String communityConfigQuery,
			String withdrawBankQuery,
			String consumerHotlineQuery,
			String customerSwitchQuery) {
		Setting row = settingMapper.selectById(Long.valueOf(companyId));

		final Map<String, Object> setting;
		if (row == null) {
			setting = Collections.emptyMap();
		} else {
			setting = toApiMap(row);
		}

		boolean qCommunity = isTruthyForJsonColumn(communityConfigQuery);
		boolean qWithdraw = isTruthyForJsonColumn(withdrawBankQuery);
		boolean qHotline = isTruthyForJsonColumn(consumerHotlineQuery);
		boolean qCustomer = isTruthyForJsonColumn(customerSwitchQuery);

		if (!qCommunity && !qWithdraw && !qHotline && !qCustomer) {
			if (row == null) {
				return new LinkedHashMap<>();
			}
			return new LinkedHashMap<>(setting);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (qCommunity) {
			boolean isset = setting.containsKey("community_config") && setting.get("community_config") != null;
			out.put("community_config", isset ? Boolean.TRUE : Collections.emptyList());
		}
		if (qWithdraw) {
			boolean isset = setting.containsKey("withdraw_bank") && setting.get("withdraw_bank") != null;
			out.put("withdraw_bank", isset ? Boolean.TRUE : Collections.emptyList());
		}
		if (qHotline) {
			boolean isset = setting.containsKey("consumer_hotline") && setting.get("consumer_hotline") != null;
			out.put("consumer_hotline", isset ? Boolean.TRUE : "");
		}
		if (qCustomer) {
			if (row == null) {
				out.put("customer_switch", Integer.valueOf(0));
			} else {
				out.put("customer_switch", setting.get("customer_switch"));
			}
		}
		return out;
	}

	private void applyPatchToEntity(Setting entity, Map<String, Object> presentKeysOnly) {
		if (presentKeysOnly.containsKey("community_config")) {
			Object v = presentKeysOnly.get("community_config");
			if (isTruthyForJsonColumn(v)) {
				entity.setCommunityConfig(jsonColumnToDbString(v, "community_config"));
			}
		}
		if (presentKeysOnly.containsKey("withdraw_bank")) {
			Object v = presentKeysOnly.get("withdraw_bank");
			if (isTruthyForJsonColumn(v)) {
				entity.setWithdrawBank(jsonColumnToDbString(v, "withdraw_bank"));
			}
		}
		if (presentKeysOnly.containsKey("consumer_hotline")) {
			Object v = presentKeysOnly.get("consumer_hotline");
			if (isTruthyScalar(v)) {
				entity.setConsumerHotline(String.valueOf(v).trim());
			}
		}
		if (presentKeysOnly.containsKey("customer_switch")) {
			entity.setCustomerSwitch(Integer.valueOf(parseIntegerLoose(presentKeysOnly.get("customer_switch"))));
		}
	}

	private void applyFapiaoPatchToEntity(Setting entity, Map<String, Object> dataIn) {
		if (dataIn != null && isTruthyForJsonColumn(dataIn)) {
			try {
				entity.setFapiaoConfig(objectMapper.writeValueAsString(dataIn));
			} catch (JsonProcessingException e) {
				throw new BadRequestException("fapiao_config 格式错误");
			}
		}
		if (dataIn != null && dataIn.containsKey("fapiao_switch")
				&& dataIn.get("fapiao_switch") != null) {
			entity.setFapiaoSwitch(Integer.valueOf(parseFapiaoSwitchLoose(dataIn.get("fapiao_switch"))));
		}
	}

	private static int parseFapiaoSwitchLoose(Object v) {
		if (v == null) {
			throw new BadRequestException("fapiao_switch 格式错误");
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof Boolean) {
			throw new BadRequestException("fapiao_switch 格式错误");
		}
		if (v instanceof String s) {
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("fapiao_switch 格式错误");
			}
		}
		throw new BadRequestException("fapiao_switch 格式错误");
	}

	private static boolean isTruthyForJsonColumn(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Number n && n.doubleValue() == 0.0d) {
			return false;
		}
		if (v instanceof CharSequence cs) {
			String s = cs.toString().trim();
			return !s.isEmpty() && !"0".equals(s);
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		return true;
	}

	private static boolean isTruthyScalar(Object v) {
		return isTruthyForJsonColumn(v);
	}

	private String jsonColumnToDbString(Object v, String fieldKey) {
		String msg = fieldKey + " 格式错误";
		try {
			if (v instanceof Map<?, ?> || v instanceof List<?>) {
				return objectMapper.writeValueAsString(v);
			}
			if (v instanceof String s) {
				JsonNode node = objectMapper.readTree(s);
				if (node.isObject() || node.isArray()) {
					return objectMapper.writeValueAsString(node);
				}
				throw new BadRequestException(msg);
			}
			throw new BadRequestException(msg);
		} catch (JsonProcessingException e) {
			throw new BadRequestException(msg);
		}
	}

	private static int parseIntegerLoose(Object v) {
		if (v == null) {
			throw new BadRequestException("customer_switch 格式错误");
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof Boolean) {
			throw new BadRequestException("customer_switch 格式错误");
		}
		if (v instanceof String s) {
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("customer_switch 格式错误");
			}
		}
		throw new BadRequestException("customer_switch 格式错误");
	}

	private Map<String, Object> toApiMap(Setting entity) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("company_id", entity.getCompanyId());
		out.put("community_config", parseJsonColumnForApi(entity.getCommunityConfig()));
		out.put("withdraw_bank", parseWithdrawBankForApi(entity.getWithdrawBank()));
		out.put("consumer_hotline", entity.getConsumerHotline());
		out.put("customer_switch", entity.getCustomerSwitch() != null ? entity.getCustomerSwitch() : 0);
		out.put("fapiao_config", parseJsonColumnForApi(entity.getFapiaoConfig()));
		out.put("fapiao_switch", entity.getFapiaoSwitch() != null ? entity.getFapiaoSwitch() : 0);
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		return out;
	}

	private Object decodeFapiaoConfigForGet(String raw) {
		try {
			JsonNode node = objectMapper.readTree(raw);
			if (node == null || node.isNull() || node.isMissingNode()) {
				return Collections.emptyList();
			}
			return objectMapper.convertValue(node, Object.class);
		} catch (JsonProcessingException e) {
			return Collections.emptyList();
		}
	}

	private Map<String, Object> parseJsonColumnForApi(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private Map<String, Object> parseWithdrawBankForApi(String raw) {
		Map<String, Object> parsed = parseJsonColumnForApi(raw);
		if (parsed == null) {
			return null;
		}
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>(parsed);
		copy.remove("groupskf");
		copy.remove("pintuan");
		return copy;
	}
}
