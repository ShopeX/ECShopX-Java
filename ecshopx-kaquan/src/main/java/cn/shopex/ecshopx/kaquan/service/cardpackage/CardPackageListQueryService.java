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
import cn.shopex.ecshopx.kaquan.mapper.CardPackageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CardPackageListQueryService {

	private final CardPackageMapper cardPackageMapper;
	private final CardPackageMultiLangReadService cardPackageMultiLangReadService;

	public CardPackageListQueryService(CardPackageMapper cardPackageMapper,
			CardPackageMultiLangReadService cardPackageMultiLangReadService) {
		this.cardPackageMapper = cardPackageMapper;
		this.cardPackageMultiLangReadService = cardPackageMultiLangReadService;
	}

	public Map<String, Object> getList(long companyId, long page, long pageSize, String title, String countryCode) {
		LambdaQueryWrapper<CardPackage> countWrapper = new LambdaQueryWrapper<>();
		applyPackageListWhere(countWrapper, companyId, title);
		long totalCount = cardPackageMapper.selectCount(countWrapper);

		List<Map<String, Object>> list = new ArrayList<>();
		if (totalCount > 0L) {
			LambdaQueryWrapper<CardPackage> listWrapper = new LambdaQueryWrapper<>();
			applyPackageListWhere(listWrapper, companyId, title);
			listWrapper.select(
					CardPackage::getPackageId,
					CardPackage::getTitle,
					CardPackage::getPackageDescribe,
					CardPackage::getLimitCount,
					CardPackage::getGetNum);
			listWrapper.orderByDesc(CardPackage::getCreated);
			Page<CardPackage> mpPage = new Page<>(page, pageSize, false);
			cardPackageMapper.selectPage(mpPage, listWrapper);
			for (CardPackage pkg : mpPage.getRecords()) {
				LinkedHashMap<String, Object> row = new LinkedHashMap<>();
				int packageIdInt = pkg.getPackageId() != null ? pkg.getPackageId().intValue() : 0;
				row.put("package_id", packageIdInt);
				row.put("title", pkg.getTitle() != null ? pkg.getTitle() : "");
				row.put("package_describe", pkg.getPackageDescribe() != null ? pkg.getPackageDescribe() : "");
				row.put("limit_count", pkg.getLimitCount() != null ? pkg.getLimitCount().intValue() : 0);
				row.put("get_num", pkg.getGetNum() != null ? pkg.getGetNum().intValue() : 0);
				long packageIdLong = pkg.getPackageId() != null ? pkg.getPackageId() : 0L;
				cardPackageMultiLangReadService.overlay(companyId, packageIdLong, row, countryCode);
				list.add(row);
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", list);
		return out;
	}

	private static void applyPackageListWhere(LambdaQueryWrapper<CardPackage> w, long companyId, String title) {
		w.eq(CardPackage::getCompanyId, companyId).eq(CardPackage::getRowStatus, 1);
		String t = title != null ? title.trim() : "";
		if (StringUtils.hasText(t)) {
			w.like(CardPackage::getTitle, "%" + t + "%");
		}
	}
}
