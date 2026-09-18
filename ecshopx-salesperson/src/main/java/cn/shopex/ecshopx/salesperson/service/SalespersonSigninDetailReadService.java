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
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SalespersonSigninDetailReadService {

	private static final String SALESPERSON_TYPE_SHOPPING_GUIDE = "shopping_guide";
	private static final String IS_VALID_TRUE = "true";

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public SalespersonSigninDetailReadService(
			ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Object getDetailForSigninPoll(long companyId, long salespersonId) {
		ShopSalesperson entity = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.eq(ShopSalesperson::getSalespersonType, SALESPERSON_TYPE_SHOPPING_GUIDE)
				.eq(ShopSalesperson::getIsValid, IS_VALID_TRUE)
				.last("LIMIT 1"));
		if (entity == null) {
			return Collections.emptyList();
		}

		LinkedHashMap<String, Object> info = new LinkedHashMap<>();
		appendPayloadFieldsBeforeRole(info, entity);
		info.put("role", entity.getRole() != null ? entity.getRole() : "");
		appendPayloadFieldsAfterRole(info, entity);

		List<ShopsRelSalesperson> relRows = shopsRelSalespersonMapper.selectList(
				new LambdaQueryWrapper<ShopsRelSalesperson>()
						.eq(ShopsRelSalesperson::getCompanyId, companyId)
						.eq(ShopsRelSalesperson::getSalespersonId, entity.getSalespersonId())
						.orderByAsc(ShopsRelSalesperson::getShopId));

		List<Long> shopIds = new ArrayList<>();
		List<Long> distributorIds = new ArrayList<>();

		for (ShopsRelSalesperson rel : relRows) {
			String st = rel.getStoreType();
			Long sid = rel.getShopId();
			if (sid == null) {
				continue;
			}
			if ("shop".equals(st)) {
				shopIds.add(sid);
				info.put("store_type", "shop");
			} else if ("distributor".equals(st)) {
				distributorIds.add(sid);
				info.put("store_type", "distributor");
			}
		}

		info.put("shop_ids", new ArrayList<>(shopIds));
		info.put("distributor_ids", new ArrayList<>(distributorIds));

		if (distributorIds.isEmpty()) {
			info.put("distributor_id", Boolean.FALSE);
		} else {
			info.put("distributor_id", String.valueOf(distributorIds.get(0)));
		}

		return info;
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
