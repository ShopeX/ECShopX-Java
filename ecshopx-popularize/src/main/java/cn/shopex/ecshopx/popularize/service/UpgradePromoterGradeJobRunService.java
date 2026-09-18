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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Executes promoter grade evaluation and persistence for a single (company, user) pair, aligned with the
 * queue job completion boundary for trade-finish brokerage.
 */
@Service
public class UpgradePromoterGradeJobRunService {

	private static final String GRADE_KEY_SECOND = "second_grade";
	private static final String GRADE_KEY_THIRD = "third_grade";

	private final PromoterGradeService promoterGradeService;
	private final PromoterMapper promoterMapper;

	public UpgradePromoterGradeJobRunService(
			PromoterGradeService promoterGradeService, PromoterMapper promoterMapper) {
		this.promoterGradeService = promoterGradeService;
		this.promoterMapper = promoterMapper;
	}

	public void run(long companyId, long userId) {
		if (!promoterGradeService.readIsOpenPromoterGrade(companyId)) {
			return;
		}
		Map<String, Object> gradeConfig = promoterGradeService.getPromoterGradeConfig(companyId);
		Object openFlag = gradeConfig.get("isOpenPromoterGrade");
		if (openFlag != null && "false".equalsIgnoreCase(String.valueOf(openFlag).trim())) {
			return;
		}

		Promoter promoter =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getCompanyId, companyId)
								.eq(Promoter::getUserId, userId)
								.last("LIMIT 1"));
		if (promoter == null) {
			return;
		}
		if (!Objects.equals(promoter.getIsPromoter(), 1)) {
			return;
		}
		if (promoter.getDisabled() != null && promoter.getDisabled() != 0) {
			return;
		}
		if (Objects.equals(promoter.getShopStatus(), 3)) {
			return;
		}
		int currentLevel = promoter.getGradeLevel() == null ? 1 : promoter.getGradeLevel();
		if (currentLevel >= 3) {
			return;
		}

		Object upgradeRoot = gradeConfig.get("upgrade");
		if (!(upgradeRoot instanceof Map<?, ?>)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> upgrade = (Map<String, Object>) upgradeRoot;
		Object filterObj = upgrade.get("filter");
		if (!(filterObj instanceof Map<?, ?>)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> filter = (Map<String, Object>) filterObj;
		boolean needChildren = truthyFilter(filter.get("children_num"));
		boolean needSales = truthyFilter(filter.get("children_sales_amount"));

		Object gradeObj = gradeConfig.get("grade");
		if (!(gradeObj instanceof Map<?, ?>)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> grade = (Map<String, Object>) gradeObj;

		long parentMemberUserId = promoter.getUserId() == null ? 0L : promoter.getUserId();
		if (parentMemberUserId <= 0L) {
			return;
		}
		long directChildren =
				promoterMapper.countDirectChildrenByParentMemberUserId(companyId, parentMemberUserId);
		long childSalesMinor = 0L;
		if (needSales) {
			childSalesMinor = promoterMapper.sumChildOrdersTotalFeeMinorForParentMember(companyId, parentMemberUserId);
		}

		int targetLevel = currentLevel;
		if (currentLevel <= 1
				&& meetsGradeThreshold(grade, GRADE_KEY_SECOND, needChildren, needSales, directChildren, childSalesMinor)) {
			targetLevel = 2;
		}
		if (targetLevel == 2
				&& meetsGradeThreshold(grade, GRADE_KEY_THIRD, needChildren, needSales, directChildren, childSalesMinor)) {
			targetLevel = 3;
		}
		if (targetLevel == currentLevel) {
			return;
		}

		promoterMapper.update(
				null,
				new LambdaUpdateWrapper<Promoter>()
						.eq(Promoter::getCompanyId, companyId)
						.eq(Promoter::getUserId, userId)
						.set(Promoter::getGradeLevel, targetLevel));
	}

	private static boolean meetsGradeThreshold(
			Map<String, Object> grade,
			String gradeKey,
			boolean needChildren,
			boolean needSales,
			long directChildren,
			long childSalesMinor) {
		Object rowObj = grade.get(gradeKey);
		if (!(rowObj instanceof Map<?, ?>)) {
			return false;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> row = (Map<String, Object>) rowObj;
		if (needChildren) {
			long need = longFromDynamic(row.get("children_num"));
			if (directChildren < need) {
				return false;
			}
		}
		if (needSales) {
			long need = longFromDynamic(row.get("children_sales_amount"));
			if (childSalesMinor < need) {
				return false;
			}
		}
		return true;
	}

	private static boolean truthyFilter(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		return "true".equalsIgnoreCase(String.valueOf(v).trim());
	}

	private static long longFromDynamic(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			try {
				return (long) Double.parseDouble(s);
			} catch (NumberFormatException e2) {
				return 0L;
			}
		}
	}
}
