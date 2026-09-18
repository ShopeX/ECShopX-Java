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
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityPassphraseEnterpriseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ActivityPassphraseService {

	private final ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper;

	public ActivityPassphraseService(ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper) {
		this.activityPassphraseEnterpriseMapper = activityPassphraseEnterpriseMapper;
	}

	public boolean isPassphraseEnabled(Activities activity) {
		return activity != null && Boolean.TRUE.equals(activity.getIsPassphraseEnabled());
	}

	public ActivityPassphraseEnterprise requireEnterpriseConfig(
			long companyId, long activityId, long enterpriseId) {
		ActivityPassphraseEnterprise row =
				activityPassphraseEnterpriseMapper.selectOne(
						Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
								.eq(ActivityPassphraseEnterprise::getCompanyId, companyId)
								.eq(ActivityPassphraseEnterprise::getActivityId, activityId)
								.eq(ActivityPassphraseEnterprise::getEnterpriseId, enterpriseId)
								.last("LIMIT 1"));
		if (row == null) {
			return null;
		}
		return row;
	}

	public boolean isActivityEnterprisePassphraseMatch(
			Activities activity,
			long companyId,
			long activityId,
			long enterpriseId,
			String rawCode) {
		if (!isPassphraseEnabled(activity)) {
			return false;
		}
		ActivityPassphraseEnterprise cfg = requireEnterpriseConfig(companyId, activityId, enterpriseId);
		if (cfg == null || !StringUtils.hasText(cfg.getPassphraseCode())) {
			return false;
		}
		String input = rawCode == null ? "" : rawCode.trim();
		if (!StringUtils.hasText(input)) {
			return false;
		}
		return constantTimeEquals(cfg.getPassphraseCode().trim(), input);
	}

	public Map<String, Object> getPassphraseClientSummary(
			Activities activity, long companyId, long activityId, long enterpriseId) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		boolean enabled = isPassphraseEnabled(activity);
		out.put("is_passphrase_enabled", enabled ? 1 : 0);
		if (!enabled) {
			out.put("passphrase_participate_quota", null);
			out.put("passphrase_limitfee", null);
			return out;
		}
		ActivityPassphraseEnterprise cfg = requireEnterpriseConfig(companyId, activityId, enterpriseId);
		if (cfg == null) {
			out.put("passphrase_participate_quota", null);
			out.put("passphrase_limitfee", null);
			return out;
		}
		out.put("passphrase_participate_quota", cfg.getParticipateQuota());
		out.put("passphrase_limitfee", cfg.getPassphraseLimitfee());
		return out;
	}

	public Set<String> collectUsedCodesForCompany(long companyId, Long excludeActivityId) {
		List<ActivityPassphraseEnterprise> rows =
				activityPassphraseEnterpriseMapper.selectList(
						Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
								.eq(ActivityPassphraseEnterprise::getCompanyId, companyId));
		return toCodeSet(rows, excludeActivityId, false);
	}

	public Set<String> collectUsedCodesForActivity(long companyId, long activityId) {
		List<ActivityPassphraseEnterprise> rows =
				activityPassphraseEnterpriseMapper.selectList(
						Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
								.eq(ActivityPassphraseEnterprise::getCompanyId, companyId)
								.eq(ActivityPassphraseEnterprise::getActivityId, activityId));
		return toCodeSet(rows, activityId, true);
	}

	private static Set<String> toCodeSet(
			List<ActivityPassphraseEnterprise> rows, Long activityFilter, boolean onlyActivity) {
		Set<String> out = new HashSet<>();
		for (ActivityPassphraseEnterprise row : rows) {
			if (onlyActivity && !Objects.equals(row.getActivityId(), activityFilter)) {
				continue;
			}
			if (!onlyActivity && activityFilter != null && activityFilter > 0L
					&& Objects.equals(row.getActivityId(), activityFilter)) {
				continue;
			}
			if (StringUtils.hasText(row.getPassphraseCode())) {
				out.add(row.getPassphraseCode().trim());
			}
		}
		return out;
	}

	private static boolean constantTimeEquals(String expected, String actual) {
		if (expected.length() != actual.length()) {
			return false;
		}
		int diff = 0;
		for (int i = 0; i < expected.length(); i++) {
			diff |= expected.charAt(i) ^ actual.charAt(i);
		}
		return diff == 0;
	}
}
