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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShopSalespersonAdminUpdateService {

	private static final Logger log = LoggerFactory.getLogger(ShopSalespersonAdminUpdateService.class);

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public ShopSalespersonAdminUpdateService(ShopSalespersonMapper shopSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updatesalesperson(long companyId, Map<String, Object> mergedParams) {
		long salespersonId = parseSalespersonId(mergedParams);
		requireH5UpdateSalespersonInputKeys(mergedParams);
		String isValidStored = normalizeIsValidH5Updatesalesperson(mergedParams);
		boolean updateName = shouldUpdateNameColumn(mergedParams);
		String nameRaw = null;
		if (updateName) {
			nameRaw = String.valueOf(mergedParams.get("name")).trim();
		}

		if (log.isDebugEnabled()) {
			log.debug("updatesalesperson mergedParams={}, isValidStored={}, updateName={}", mergedParams, isValidStored,
					updateName);
		}

		ShopSalesperson current = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.last("LIMIT 1"));
		if (current == null) {
			throw new ResourceException("更新的人员不存在");
		}

		long now = Instant.now().getEpochSecond();
		LambdaUpdateWrapper<ShopSalesperson> uw = new LambdaUpdateWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.set(ShopSalesperson::getUpdated, now)
				.set(ShopSalesperson::getIsValid, isValidStored);
		if (updateName) {
			uw.set(ShopSalesperson::getName, sensitiveFieldEncryptor.encrypt(nameRaw));
		}

		int rows = shopSalespersonMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		ShopSalesperson fresh = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.last("LIMIT 1"));
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}
		Map<String, Object> data = buildH5UpdateSalespersonDataMap(fresh);

		LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();
		envelope.put("status", 1);
		envelope.put("code", 0);
		envelope.put("data", data);
		envelope.put("inoutData", new LinkedHashMap<>(mergedParams));
		return envelope;
	}

	private Map<String, Object> buildH5UpdateSalespersonDataMap(ShopSalesperson row) {
		LinkedHashMap<String, Object> values = new LinkedHashMap<>();
		values.put("salesperson_id", String.valueOf(row.getSalespersonId()));
		values.put("name", row.getName() != null ? sensitiveFieldEncryptor.decrypt(row.getName()) : "");
		values.put("mobile", row.getMobile() != null ? sensitiveFieldEncryptor.decrypt(row.getMobile()) : "");
		String ct = row.getCreatedTime();
		if (ct != null && !ct.isEmpty()) {
			values.put("created_time", ct);
		} else {
			values.put("created_time", null);
		}
		values.put("salesperson_type", row.getSalespersonType() != null ? row.getSalespersonType() : "");
		values.put("company_id", String.valueOf(row.getCompanyId()));
		values.put("user_id", row.getUserId() != null ? row.getUserId() : 0);
		values.put("child_count", row.getChildCount() != null ? row.getChildCount() : 0);
		values.put("is_valid", row.getIsValid() != null ? row.getIsValid() : "");
		values.put("shop_id", normalizeShopIdForH5Response(row.getShopId()));
		values.put("shop_name", row.getShopName());
		values.put("number", emptyStringToNull(row.getNumber()));
		values.put("friend_count", row.getFriendCount() != null ? row.getFriendCount() : 0);
		values.put("avatar", row.getAvatar());
		values.put("work_userid", row.getWorkUserid());
		values.put("work_configid", row.getWorkConfigid());
		values.put("work_qrcode_configid", row.getWorkQrcodeConfigid());
		values.put("role", emptyStringToNull(row.getRole()));
		values.put("salesperson_job", row.getSalespersonJob() != null ? row.getSalespersonJob() : "");
		values.put("employee_status", row.getEmployeeStatus() != null ? row.getEmployeeStatus() : 0);
		values.put("created", row.getCreated());
		values.put("updated", row.getUpdated());
		values.put("work_clear_userid", row.getWorkClearUserid());
		return values;
	}

	private static Object normalizeShopIdForH5Response(String shopId) {
		if (shopId == null) {
			return null;
		}
		String t = shopId.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return null;
		}
		return shopId;
	}

	private static String emptyStringToNull(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	private static void requireH5UpdateSalespersonInputKeys(Map<String, Object> mergedParams) {
		if (!mergedParams.containsKey("is_valid")) {
			throw new ResourceException("参数错误");
		}
		if (!mergedParams.containsKey("name")) {
			throw new ResourceException("参数错误");
		}
	}

	private static boolean shouldUpdateNameColumn(Map<String, Object> mergedParams) {
		if (!mergedParams.containsKey("name")) {
			return false;
		}
		return mergedParams.get("name") != null;
	}

	private static long parseSalespersonId(Map<String, Object> mergedParams) {
		Object rawId = mergedParams.get("salesperson_id");
		if (rawId == null) {
			throw new ResourceException("参数错误");
		}
		if (rawId instanceof Boolean) {
			throw new ResourceException("参数错误");
		}
		long salespersonId;
		if (rawId instanceof Number n) {
			salespersonId = n.longValue();
		} else if (rawId instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new ResourceException("参数错误");
			}
			salespersonId = LeadingNumberParser.parseAsLong(t);
		} else {
			throw new ResourceException("参数错误");
		}
		if (salespersonId <= 0L) {
			throw new ResourceException("参数错误");
		}
		return salespersonId;
	}

	/** Normalizes {@code is_valid} using the same loose rules as the H5 controller (incl. JSON boolean true → disabled). */
	private static String normalizeIsValidH5Updatesalesperson(Map<String, Object> mergedParams) {
		Object v = mergedParams.get("is_valid");
		if (h5UpdatesalespersonIsValidLooseFalse(v)) {
			return "false";
		}
		return "true";
	}

	/** True when the controller would treat {@code is_valid} as disabling the salesperson (loose false / literal false). */
	private static boolean h5UpdatesalespersonIsValidLooseFalse(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean) {
			return true;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		if (v instanceof String s) {
			if (s.isEmpty()) {
				return true;
			}
			if ("0".equals(s)) {
				return true;
			}
			return "false".equals(s);
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}
}
