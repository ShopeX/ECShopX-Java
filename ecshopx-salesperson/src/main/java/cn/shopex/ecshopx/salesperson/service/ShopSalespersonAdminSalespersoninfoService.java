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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopSalespersonAdminSalespersoninfoService {

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public ShopSalespersonAdminSalespersoninfoService(ShopSalespersonMapper shopSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> salespersoninfo(long companyId, Map<String, Object> mergedInput,
			HttpServletRequest request) {
		LambdaQueryWrapper<ShopSalesperson> w = new LambdaQueryWrapper<>();
		w.eq(ShopSalesperson::getCompanyId, companyId);
		applyShopIdFilter(w, mergedInput, request);
		applyIdFilter(w, mergedInput);
		applyUserIdFilter(w, mergedInput);
		applyEncryptedNameMobileFilters(w, mergedInput);

		ShopSalesperson sp = shopSalespersonMapper.selectOne(w);

		LinkedHashMap<String, Object> inputData = new LinkedHashMap<>(mergedInput);

		Object data;
		if (sp == null) {
			data = Collections.emptyList();
		} else {
			String namePlain = sp.getName() == null ? null : sensitiveFieldEncryptor.decrypt(sp.getName());
			String mobilePlain = sp.getMobile() == null ? null : sensitiveFieldEncryptor.decrypt(sp.getMobile());
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			putRowFields(row, sp, namePlain, mobilePlain);
			normalizeIsValid(row);
			data = row;
		}

		LinkedHashMap<String, Object> legacy = new LinkedHashMap<>();
		legacy.put("status", 1);
		legacy.put("code", 0);
		legacy.put("data", data);
		legacy.put("inputData", inputData);
		return legacy;
	}

	private void applyShopIdFilter(LambdaQueryWrapper<ShopSalesperson> w, Map<String, Object> mergedInput,
			HttpServletRequest request) {
		boolean distributorKeyFromMerged = mergedInput.containsKey("distributor_id");
		boolean distributorKeyFromQuery = request.getParameterMap().containsKey("distributor_id");
		if (!distributorKeyFromMerged && !distributorKeyFromQuery) {
			w.isNull(ShopSalesperson::getShopId);
			return;
		}
		Object raw = distributorKeyFromMerged ? mergedInput.get("distributor_id") : request.getParameter("distributor_id");
		if (raw == null) {
			w.isNull(ShopSalesperson::getShopId);
		} else {
			String v = raw instanceof String s ? s : String.valueOf(raw);
			w.eq(ShopSalesperson::getShopId, v);
		}
	}

	private void applyIdFilter(LambdaQueryWrapper<ShopSalesperson> w, Map<String, Object> mergedInput) {
		if (!mergedInput.containsKey("id")) {
			return;
		}
		Object v = mergedInput.get("id");
		String s = v == null ? "" : String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		long idLong;
		try {
			idLong = Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
		if (idLong > 0L) {
			w.eq(ShopSalesperson::getSalespersonId, idLong);
		}
	}

	private void applyUserIdFilter(LambdaQueryWrapper<ShopSalesperson> w, Map<String, Object> mergedInput) {
		if (!mergedInput.containsKey("user_id")) {
			return;
		}
		Object v = mergedInput.get("user_id");
		String s = v == null ? "" : String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		int uid;
		try {
			uid = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
		if (uid > 0) {
			w.eq(ShopSalesperson::getUserId, uid);
		}
	}

	private void applyEncryptedNameMobileFilters(LambdaQueryWrapper<ShopSalesperson> w,
			Map<String, Object> mergedInput) {
		if (mergedInput.containsKey("name")) {
			Object v = mergedInput.get("name");
			String s = v == null ? "" : String.valueOf(v).trim();
			if (StringUtils.hasText(s)) {
				w.eq(ShopSalesperson::getName, sensitiveFieldEncryptor.encrypt(s));
			}
		}
		if (mergedInput.containsKey("mobile")) {
			Object v = mergedInput.get("mobile");
			String s = v == null ? "" : String.valueOf(v).trim();
			if (StringUtils.hasText(s)) {
				w.eq(ShopSalesperson::getMobile, sensitiveFieldEncryptor.encrypt(s));
			}
		}
	}

	private static void putRowFields(LinkedHashMap<String, Object> row, ShopSalesperson sp, String namePlain,
			String mobilePlain) {
		row.put("salesperson_id", sp.getSalespersonId());
		row.put("name", namePlain);
		row.put("mobile", mobilePlain);
		row.put("created_time", sp.getCreatedTime());
		row.put("salesperson_type", sp.getSalespersonType());
		row.put("company_id", sp.getCompanyId());
		row.put("user_id", sp.getUserId() == null ? 0 : sp.getUserId());
		row.put("child_count", sp.getChildCount());
		row.put("is_valid", sp.getIsValid());
		row.put("shop_id", sp.getShopId());
		row.put("shop_name", sp.getShopName());
		row.put("number", sp.getNumber());
		row.put("friend_count", sp.getFriendCount());
		row.put("avatar", sp.getAvatar());
		row.put("work_userid", sp.getWorkUserid());
		row.put("work_configid", sp.getWorkConfigid());
		row.put("work_qrcode_configid", sp.getWorkQrcodeConfigid());
		row.put("role", sp.getRole());
		row.put("salesperson_job", sp.getSalespersonJob());
		row.put("employee_status", sp.getEmployeeStatus());
		row.put("created", sp.getCreated());
		row.put("updated", sp.getUpdated());
		row.put("work_clear_userid", sp.getWorkClearUserid());
	}

	private static void normalizeIsValid(LinkedHashMap<String, Object> row) {
		if (!row.containsKey("is_valid")) {
			return;
		}
		String s = String.valueOf(row.get("is_valid"));
		if ("true".equals(s)) {
			row.put("is_valid", Boolean.TRUE);
		} else if ("false".equals(s)) {
			row.put("is_valid", Boolean.FALSE);
		}
	}
}
