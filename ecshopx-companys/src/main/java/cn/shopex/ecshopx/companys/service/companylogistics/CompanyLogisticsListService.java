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

package cn.shopex.ecshopx.companys.service.companylogistics;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.domain.CompanyRelLogistics;
import cn.shopex.ecshopx.companys.mapper.CompanyRelLogisticsMapper;
import cn.shopex.ecshopx.superadmin.domain.Logistics;
import cn.shopex.ecshopx.superadmin.mapper.LogisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CompanyLogisticsListService {

	private final LogisticsMapper logisticsMapper;
	private final CompanyRelLogisticsMapper companyRelLogisticsMapper;

	public CompanyLogisticsListService(
			LogisticsMapper logisticsMapper, CompanyRelLogisticsMapper companyRelLogisticsMapper) {
		this.logisticsMapper = logisticsMapper;
		this.companyRelLogisticsMapper = companyRelLogisticsMapper;
	}

	public Map<String, Object> getCompanyLogisticsList(
			long companyId,
			int distributorId,
			int supplierId,
			String corpNameFilter,
			String statusInput) {
		if (companyId > Integer.MAX_VALUE) {
			throw new BadRequestException("参数错误");
		}
		int cid = (int) companyId;

		Optional<Integer> statusOpt = normalizedStatusCodeIfFilterActive(statusInput);
		boolean statusFilterActive = statusOpt.isPresent();
		Integer statusNorm = statusOpt.orElse(null);

		long totalCount = logisticsMapper.selectCount(Wrappers.lambdaQuery(Logistics.class));
		List<Logistics> logisticsRows = logisticsMapper.selectList(Wrappers.lambdaQuery(Logistics.class));

		LambdaQueryWrapper<CompanyRelLogistics> base =
				Wrappers.lambdaQuery(CompanyRelLogistics.class)
						.eq(CompanyRelLogistics::getCompanyId, cid)
						.eq(CompanyRelLogistics::getDistributorId, (long) distributorId)
						.eq(CompanyRelLogistics::getSupplierId, (long) supplierId);
		long relTotal = companyRelLogisticsMapper.selectCount(base);

		List<CompanyRelLogistics> relRows;
		if (relTotal == 0) {
			relRows = Collections.emptyList();
		} else {
			LambdaQueryWrapper<CompanyRelLogistics> listWrapper =
					base.clone()
							.select(CompanyRelLogistics::getCorpId, CompanyRelLogistics::getId, CompanyRelLogistics::getCompanyId)
							.orderByAsc(CompanyRelLogistics::getId);
			relRows = companyRelLogisticsMapper.selectList(listWrapper);
		}

		Map<Integer, CompanyRelLogistics> relByCorpId =
				relRows.stream()
						.collect(Collectors.toMap(CompanyRelLogistics::getCorpId, Function.identity(), (a, b) -> b, LinkedHashMap::new));

		List<Map<String, Object>> mergedList = new ArrayList<>(logisticsRows.size());
		for (Logistics logisticsRow : logisticsRows) {
			Integer corpKey = logisticsRow.getCorpId();
			LinkedHashMap<String, Object> row = new LinkedHashMap<>(16);
			row.put("corp_id", logisticsRow.getCorpId());
			row.put("corp_code", logisticsRow.getCorpCode());
			row.put("kuaidi_code", logisticsRow.getKuaidiCode());
			row.put("full_name", logisticsRow.getFullName());
			row.put("corp_name", logisticsRow.getCorpName());
			row.put("order_sort", logisticsRow.getOrderSort());
			row.put("custom", logisticsRow.getCustom());
			row.put("created", logisticsRow.getCreated());
			row.put("updated", logisticsRow.getUpdated());
			row.put("phone", logisticsRow.getPhone());
			row.put("logo", logisticsRow.getLogo());
			if (relByCorpId.containsKey(corpKey)) {
				CompanyRelLogistics rel = relByCorpId.get(corpKey);
				row.put("corp_id", rel.getCorpId());
				row.put("id", rel.getId());
				row.put("company_id", rel.getCompanyId());
			} else {
				row.put("id", null);
				row.put("company_id", null);
			}
			mergedList.add(row);
		}

		boolean corpNameActive = corpNameFilter != null && !corpNameFilter.trim().isEmpty();
		Iterator<Map<String, Object>> it = mergedList.iterator();
		while (it.hasNext()) {
			Map<String, Object> row = it.next();
			Integer corpKey = (Integer) row.get("corp_id");
			boolean remove = false;
			if (corpNameActive) {
				if (!Objects.equals(corpNameFilter.trim(), Objects.toString(row.get("corp_name"), "").trim())
						&& !relByCorpId.containsKey(corpKey)) {
					remove = true;
				}
			}
			if (!remove && statusFilterActive && Objects.equals(statusNorm, Integer.valueOf(1))) {
				if (!relByCorpId.containsKey(corpKey)) {
					remove = true;
				}
			}
			if (!remove && statusFilterActive && Objects.equals(statusNorm, Integer.valueOf(2))) {
				if (relByCorpId.containsKey(corpKey)) {
					remove = true;
				}
			}
			if (remove) {
				it.remove();
				totalCount--;
			}
		}

		Map<String, Object> data = new LinkedHashMap<>(2);
		data.put("total_count", totalCount);
		data.put("list", mergedList);
		return data;
	}

	/**
	 * @return {@link Optional#empty()} 表示不启用 status 筛选：入参为 null、仅空白、字符串 {@code "0"}、
	 *         无法解析为整数、或解析结果为 0；{@link Optional#of(Integer)} 且值为非 0 时表示启用筛选，
	 *         值为归一化后的 status 码。
	 */
	public Optional<Integer> normalizedStatusCodeIfFilterActive(String statusInput) {
		if (statusInput == null) {
			return Optional.empty();
		}
		String t = statusInput.trim();
		if (t.isEmpty()) {
			return Optional.empty();
		}
		if ("0".equals(t)) {
			return Optional.empty();
		}
		int parsed;
		try {
			parsed = Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
		if (parsed == 0) {
			return Optional.empty();
		}
		return Optional.of(Integer.valueOf(parsed));
	}
}
