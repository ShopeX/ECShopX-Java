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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import cn.shopex.ecshopx.kaquan.domain.CardPackage;
import cn.shopex.ecshopx.kaquan.domain.CardPackageTrigger;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageMapper;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageTriggerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CardPackageBindListQueryService {

	private final CardPackageTriggerMapper cardPackageTriggerMapper;
	private final CardPackageMapper cardPackageMapper;

	public CardPackageBindListQueryService(CardPackageTriggerMapper cardPackageTriggerMapper,
			CardPackageMapper cardPackageMapper) {
		this.cardPackageTriggerMapper = cardPackageTriggerMapper;
		this.cardPackageMapper = cardPackageMapper;
	}

	/**
	 * 按触发类型与关联 ID 列表查询已绑定且有效的卡券包摘要，按关联 ID 分组。
	 */
	public Map<Long, List<Map<String, Object>>> getBindPackageList(long companyId, List<Long> gradeIdList,
			String triggerType) {
		if (gradeIdList == null || gradeIdList.isEmpty()) {
			return Collections.emptyMap();
		}
		List<CardPackageTrigger> triggers = cardPackageTriggerMapper.selectList(new LambdaQueryWrapper<CardPackageTrigger>()
				.eq(CardPackageTrigger::getCompanyId, companyId)
				.eq(CardPackageTrigger::getTriggerType, triggerType)
				.in(CardPackageTrigger::getAssociationId, gradeIdList)
				.orderByAsc(CardPackageTrigger::getId));
		if (triggers.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<Long> packageIdSet = new LinkedHashSet<>();
		for (CardPackageTrigger t : triggers) {
			if (t.getPackageId() != null) {
				packageIdSet.add(t.getPackageId());
			}
		}
		if (packageIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Long> packageIdList = new ArrayList<>(packageIdSet);
		List<CardPackage> packages = cardPackageMapper.selectList(new LambdaQueryWrapper<CardPackage>()
				.eq(CardPackage::getCompanyId, companyId)
				.in(CardPackage::getPackageId, packageIdList)
				.eq(CardPackage::getRowStatus, 1));
		Map<Long, Map<String, Object>> packageIndex = packages.stream()
				.collect(Collectors.toMap(CardPackage::getPackageId, this::toSummaryMap, (a, b) -> a, LinkedHashMap::new));
		Map<Long, List<Map<String, Object>>> byAssociation = new LinkedHashMap<>();
		for (CardPackageTrigger t : triggers) {
			Long assoc = t.getAssociationId();
			Long pid = t.getPackageId();
			if (assoc == null || pid == null) {
				continue;
			}
			Map<String, Object> pkg = packageIndex.get(pid);
			if (pkg == null) {
				continue;
			}
			byAssociation.computeIfAbsent(assoc, k -> new ArrayList<>()).add(pkg);
		}
		return byAssociation;
	}

	private Map<String, Object> toSummaryMap(CardPackage p) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("package_id", p.getPackageId());
		m.put("title", p.getTitle());
		m.put("package_describe", p.getPackageDescribe());
		m.put("limit_count", p.getLimitCount());
		m.put("get_num", p.getGetNum());
		return m;
	}
}
