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
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SalespersonBaInfoUpdateService {

	private final ShopSalespersonMapper shopSalespersonMapper;

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public SalespersonBaInfoUpdateService(
			ShopSalespersonMapper shopSalespersonMapper, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> updateBaInfo(long salespersonId, Map<String, Object> mergedInput) {
		String avatar = trimIfKeyPresent(mergedInput, "avatar");
		String qrcode = trimIfKeyPresent(mergedInput, "qrcode");
		String mobile = trimIfKeyPresent(mergedInput, "mobile");

		boolean any = Stream.of(avatar, qrcode, mobile).filter(Objects::nonNull).anyMatch(StringUtils::hasText);
		if (!any) {
			throw new ResourceException("没有要更新的信息");
		}

		ShopSalesperson current = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.last("LIMIT 1"));
		if (current == null) {
			throw new ResourceException("未查询到更新数据");
		}

		boolean hasWritableColumn = mergedInput.containsKey("avatar") || mergedInput.containsKey("mobile");
		if (!hasWritableColumn) {
			return toColumnNamesData(current);
		}

		LambdaUpdateWrapper<ShopSalesperson> uw = new LambdaUpdateWrapper<>();
		uw.eq(ShopSalesperson::getSalespersonId, salespersonId);
		if (mergedInput.containsKey("avatar")) {
			uw.set(ShopSalesperson::getAvatar, avatar != null ? avatar : "");
		}
		if (mergedInput.containsKey("mobile")) {
			String m = mobile != null ? mobile : "";
			uw.set(ShopSalesperson::getMobile, sensitiveFieldEncryptor.encrypt(m));
		}
		long now = Instant.now().getEpochSecond();
		uw.set(ShopSalesperson::getUpdated, now);
		shopSalespersonMapper.update(null, uw);

		ShopSalesperson fresh = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.last("LIMIT 1"));
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toColumnNamesData(fresh);
	}

	private static String trimIfKeyPresent(Map<String, Object> mergedInput, String key) {
		if (!mergedInput.containsKey(key)) {
			return null;
		}
		Object v = mergedInput.get(key);
		return v == null ? "" : v.toString().trim();
	}

	private Map<String, Object> toColumnNamesData(ShopSalesperson row) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		appendPayloadFieldsBeforeRole(out, row);
		out.put("role", row.getRole() != null ? row.getRole() : "");
		appendPayloadFieldsAfterRole(out, row);
		return out;
	}

	private void appendPayloadFieldsBeforeRole(LinkedHashMap<String, Object> out, ShopSalesperson row) {
		out.put("salesperson_id", String.valueOf(row.getSalespersonId()));
		out.put("name", row.getName() != null ? sensitiveFieldEncryptor.decrypt(row.getName()) : "");
		out.put("mobile", row.getMobile() != null ? sensitiveFieldEncryptor.decrypt(row.getMobile()) : "");
		out.put("created_time", resolveCreatedTimeEpochSeconds(row));
		out.put("salesperson_type", row.getSalespersonType() != null ? row.getSalespersonType() : "");
		out.put("company_id", String.valueOf(row.getCompanyId()));
		out.put("user_id", String.valueOf(row.getUserId()));
		out.put("child_count", row.getChildCount() != null ? row.getChildCount() : 0);
		out.put("is_valid", row.getIsValid() != null ? row.getIsValid() : "");
		out.put("shop_id", row.getShopId() != null ? row.getShopId() : "");
		out.put("shop_name", row.getShopName());
		out.put("number", row.getNumber() != null ? row.getNumber() : "");
		out.put("friend_count", row.getFriendCount() != null ? row.getFriendCount() : 0);
		out.put("avatar", row.getAvatar());
		out.put("work_userid", row.getWorkUserid());
		out.put("work_configid", row.getWorkConfigid());
		out.put("work_qrcode_configid", row.getWorkQrcodeConfigid());
	}

	private void appendPayloadFieldsAfterRole(LinkedHashMap<String, Object> out, ShopSalesperson row) {
		out.put("salesperson_job", row.getSalespersonJob() != null ? row.getSalespersonJob() : "");
		out.put("employee_status", row.getEmployeeStatus() != null ? row.getEmployeeStatus() : 0);
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		out.put("work_clear_userid", row.getWorkClearUserid());
	}

	private static long resolveCreatedTimeEpochSeconds(ShopSalesperson row) {
		String ct = row.getCreatedTime();
		if (ct != null && !ct.trim().isEmpty()) {
			try {
				return Long.parseLong(ct.trim());
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		Long created = row.getCreated();
		return created != null ? created : 0L;
	}
}
