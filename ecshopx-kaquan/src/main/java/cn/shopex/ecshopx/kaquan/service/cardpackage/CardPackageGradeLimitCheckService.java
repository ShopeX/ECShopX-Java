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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.CardPackage;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageMapper;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.service.discount.KaquanDiscountCardMessages;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CardPackageGradeLimitCheckService {

	private final CardPackageMapper cardPackageMapper;
	private final CardPackageDiscountCardsLoadService cardPackageDiscountCardsLoadService;
	private final MemberCardGradeMapper memberCardGradeMapper;
	private final VipGradeMapper vipGradeMapper;

	public CardPackageGradeLimitCheckService(CardPackageMapper cardPackageMapper,
			CardPackageDiscountCardsLoadService cardPackageDiscountCardsLoadService,
			MemberCardGradeMapper memberCardGradeMapper, VipGradeMapper vipGradeMapper) {
		this.cardPackageMapper = cardPackageMapper;
		this.cardPackageDiscountCardsLoadService = cardPackageDiscountCardsLoadService;
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.vipGradeMapper = vipGradeMapper;
	}

	public List<Map<String, Object>> checkGradeLimit(long companyId, List<Long> packageIdList, String setType,
			long gradeId) {
		List<Map<String, Object>> merged = new ArrayList<>();
		for (Long packageId : packageIdList) {
			merged.addAll(checkOnePackage(companyId, packageId, setType, gradeId));
		}
		return merged;
	}

	private List<Map<String, Object>> checkOnePackage(long companyId, long packageId, String setType,
			long checkGradeId) {
		CardPackage pkg = cardPackageMapper.selectOne(new LambdaQueryWrapper<CardPackage>()
				.eq(CardPackage::getCompanyId, companyId)
				.eq(CardPackage::getPackageId, packageId)
				.eq(CardPackage::getRowStatus, 1));
		if (pkg == null) {
			throw new ResourceException(KaquanDiscountCardMessages.PACKAGE_NOT_FOUND);
		}

		List<DiscountCards> discountCards =
				cardPackageDiscountCardsLoadService.load(companyId, List.of(packageId)).cards();

		String gradeName = resolveGradeName(companyId, setType, checkGradeId);

		String packageTitle = pkg.getTitle() != null ? pkg.getTitle() : "";
		LinkedHashMap<String, Map<String, Object>> checkResult = new LinkedHashMap<>();

		for (DiscountCards card : discountCards) {
			Long cardId = card.getCardId();
			String cardTitle = card.getTitle() != null ? card.getTitle() : "";
			String reKey = packageTitle + "_" + cardId;

			String gradeIds = card.getGradeIds();
			if (!isCsvFieldEffectivelyEmpty(gradeIds)) {
				String trimmed = trimCommaEdges(gradeIds);
				String[] parts = trimmed.split(",", -1);
				for (String value : parts) {
					if (!"grade".equals(setType) || !valueEqualsCheckGradeId(value, checkGradeId)) {
						checkResult.put(reKey, buildRow(packageTitle, cardTitle, checkGradeId, gradeName));
					}
				}
			}

			String vipGradeIds = card.getVipGradeIds();
			if (!isCsvFieldEffectivelyEmpty(vipGradeIds)) {
				String trimmed = trimCommaEdges(vipGradeIds);
				String[] parts = trimmed.split(",", -1);
				for (String value : parts) {
					if (!"vip_grade".equals(setType) || !valueEqualsCheckGradeId(value, checkGradeId)) {
						checkResult.put(reKey, buildRow(packageTitle, cardTitle, checkGradeId, gradeName));
					}
				}
			}
		}

		return new ArrayList<>(checkResult.values());
	}

	private String resolveGradeName(long companyId, String setType, long checkGradeId) {
		if ("grade".equals(setType)) {
			List<MemberCardGrade> rows = memberCardGradeMapper.selectList(new LambdaQueryWrapper<MemberCardGrade>()
					.eq(MemberCardGrade::getCompanyId, String.valueOf(companyId)));
			Map<Long, MemberCardGrade> byId = new HashMap<>();
			for (MemberCardGrade row : rows) {
				if (row.getGradeId() != null) {
					byId.put(row.getGradeId(), row);
				}
			}
			MemberCardGrade g = byId.get(checkGradeId);
			return g != null && g.getGradeName() != null ? g.getGradeName() : "";
		}
		List<VipGrade> rows = vipGradeMapper.selectList(new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, (int) companyId));
		Map<Long, VipGrade> byId = new HashMap<>();
		for (VipGrade row : rows) {
			if (row.getVipGradeId() != null) {
				byId.put(row.getVipGradeId(), row);
			}
		}
		VipGrade v = byId.get(checkGradeId);
		return v != null && v.getGradeName() != null ? v.getGradeName() : "";
	}

	private boolean isCsvFieldEffectivelyEmpty(String s) {
		return s == null || s.isEmpty() || "0".equals(s);
	}

	private String trimCommaEdges(String s) {
		if (s == null) {
			return "";
		}
		int start = 0;
		int end = s.length();
		while (start < end && s.charAt(start) == ',') {
			start++;
		}
		while (end > start && s.charAt(end - 1) == ',') {
			end--;
		}
		return s.substring(start, end);
	}

	private boolean valueEqualsCheckGradeId(String value, long checkGradeId) {
		if (value == null) {
			return false;
		}
		String t = value.trim();
		if (t.isEmpty()) {
			return false;
		}
		try {
			return Long.parseLong(t) == checkGradeId;
		} catch (NumberFormatException e) {
			try {
				double d = Double.parseDouble(t);
				return d == (double) checkGradeId && d == Math.floor(d);
			} catch (NumberFormatException e2) {
				return false;
			}
		}
	}

	private Map<String, Object> buildRow(String packageTitle, String cardTitle, long checkGradeId, String gradeName) {
		Map<String, Object> row = new HashMap<>();
		row.put("package_title", packageTitle);
		row.put("title", cardTitle);
		row.put("grade_id", String.valueOf(checkGradeId));
		row.put("grade_name", gradeName != null ? gradeName : "");
		return row;
	}
}
