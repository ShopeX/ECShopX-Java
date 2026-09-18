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

package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityPassphraseEnterpriseMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ActivityPassphraseAdminService {

	private final ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper;
	private final EnterprisesMapper enterprisesMapper;

	public ActivityPassphraseAdminService(
			ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper,
			EnterprisesMapper enterprisesMapper) {
		this.activityPassphraseEnterpriseMapper = activityPassphraseEnterpriseMapper;
		this.enterprisesMapper = enterprisesMapper;
	}

	public void appendAdminPassphraseFields(Activities activity, Map<String, Object> target) {
		boolean enabled = activity != null && Boolean.TRUE.equals(activity.getIsPassphraseEnabled());
		target.put("is_passphrase_enabled", enabled ? "true" : "false");
		// 新契约活动口令/额度走 enterprise_configs，不再回显 passphrase_enterprises
		if (PurchaseModeSupport.isNewContractActivity(activity)) {
			return;
		}
		if (activity == null || activity.getId() == null) {
			target.put("passphrase_enterprises", List.of());
			return;
		}
		target.put(
				"passphrase_enterprises",
				buildPassphraseEnterpriseList(activity.getCompanyId(), activity.getId()));
	}

	public List<Map<String, Object>> buildPassphraseEnterpriseList(long companyId, long activityId) {
		List<ActivityPassphraseEnterprise> rows =
				activityPassphraseEnterpriseMapper.selectList(
						Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
								.eq(ActivityPassphraseEnterprise::getCompanyId, companyId)
								.eq(ActivityPassphraseEnterprise::getActivityId, activityId)
								.orderByAsc(ActivityPassphraseEnterprise::getId));
		if (rows.isEmpty()) {
			return List.of();
		}
		Set<Long> enterpriseIds =
				rows.stream()
						.map(ActivityPassphraseEnterprise::getEnterpriseId)
						.filter(Objects::nonNull)
						.collect(Collectors.toSet());
		Map<Long, Enterprises> enterpriseById = loadEnterprises(companyId, enterpriseIds);
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (ActivityPassphraseEnterprise row : rows) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("enterprise_id", row.getEnterpriseId());
			m.put("participate_quota", row.getParticipateQuota());
			m.put("passphrase_limitfee", row.getPassphraseLimitfee());
			m.put("passphrase_code", row.getPassphraseCode());
			Enterprises ent = enterpriseById.get(row.getEnterpriseId());
			if (ent != null) {
				LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
				nested.put("id", ent.getId());
				nested.put("name", ent.getName());
				nested.put("enterprise_sn", ent.getEnterpriseSn());
				m.put("enterprise", nested);
			} else {
				m.put("enterprise", null);
			}
			out.add(m);
		}
		return out;
	}

	private Map<Long, Enterprises> loadEnterprises(long companyId, Set<Long> enterpriseIds) {
		if (enterpriseIds.isEmpty()) {
			return Map.of();
		}
		List<Enterprises> list =
				enterprisesMapper.selectList(
						Wrappers.<Enterprises>lambdaQuery()
								.eq(Enterprises::getCompanyId, companyId)
								.in(Enterprises::getId, enterpriseIds));
		LinkedHashMap<Long, Enterprises> out = new LinkedHashMap<>();
		for (Enterprises e : list) {
			if (e.getId() != null) {
				out.put(e.getId(), e);
			}
		}
		return out;
	}
}
