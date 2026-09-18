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
import cn.shopex.ecshopx.common.wechat.WorkWechatRelSalespersonBatchPort;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.service.support.ShopSalespersonApiFields;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WorkWechatRelSalespersonBatchPortImpl implements WorkWechatRelSalespersonBatchPort {

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public WorkWechatRelSalespersonBatchPortImpl(ShopSalespersonMapper shopSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Override
	public Map<Long, Map<String, Object>> loadBySalespersonIds(Collection<Long> salespersonIds) {
		if (salespersonIds == null || salespersonIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Long> ids = new ArrayList<>();
		for (Long id : salespersonIds) {
			if (id != null) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapper<ShopSalesperson> w = new LambdaQueryWrapper<>();
		w.in(ShopSalesperson::getSalespersonId, ids);
		List<ShopSalesperson> list = shopSalespersonMapper.selectList(w);
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (ShopSalesperson sp : list) {
			if (sp.getSalespersonId() == null) {
				continue;
			}
			String namePlain = sp.getName() == null ? "" : sensitiveFieldEncryptor.decrypt(sp.getName());
			String mobilePlain = sp.getMobile() == null ? "" : sensitiveFieldEncryptor.decrypt(sp.getMobile());
			Map<String, Object> row = new LinkedHashMap<>();
			putSalespersonRow(row, sp, namePlain, mobilePlain);
			out.put(sp.getSalespersonId(), row);
		}
		return out;
	}

	private static void putSalespersonRow(Map<String, Object> row, ShopSalesperson sp, String namePlain,
			String mobilePlain) {
		row.put("salesperson_id", sp.getSalespersonId());
		row.put("company_id", sp.getCompanyId());
		row.put("shop_id", ShopSalespersonApiFields.shopIdForJson(sp.getShopId()));
		row.put("shop_name", sp.getShopName());
		row.put("name", namePlain);
		row.put("mobile", mobilePlain);
		row.put("salesperson_type", sp.getSalespersonType());
		row.put("created_time", sp.getCreatedTime());
		row.put("user_id", sp.getUserId() == null ? 0 : sp.getUserId());
		row.put("child_count", sp.getChildCount() == null ? 0 : sp.getChildCount());
		row.put("is_valid", sp.getIsValid());
		row.put("role", sp.getRole());
		row.put("number", sp.getNumber());
		row.put("friend_count", sp.getFriendCount());
		row.put("avatar", sp.getAvatar());
		row.put("work_userid", sp.getWorkUserid());
		row.put("work_clear_userid", sp.getWorkClearUserid());
		row.put("work_configid", sp.getWorkConfigid());
		row.put("work_qrcode_configid", sp.getWorkQrcodeConfigid());
		row.put("salesperson_job", sp.getSalespersonJob());
		row.put("employee_status", sp.getEmployeeStatus());
		row.put("created", sp.getCreated());
		row.put("updated", sp.getUpdated());
		row.put("salesman_name", namePlain);
	}
}
