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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.espier.service.upload.EspierUploadHeaderCatalog;
import cn.shopex.ecshopx.espier.service.upload.UploadHeaderColumnInfo;
import cn.shopex.ecshopx.espier.service.upload.UploadHeaderTitle;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.NormalGoodsHeaderInfo;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeListQueryService;
import cn.shopex.ecshopx.members.service.export.port.AdminMemberExportMemberCardGradesPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 对齐 PHP {@code NormalGoodsUploadService::getHeaderTitle}：按企业会员等级展开「会员价」列，平台模板不含「审核状态」。
 */
@Service
public class NormalGoodsUploadHeaderTitleService {

	private static final String MEMBER_PRICE_LABEL = "会员价";
	private static final String MEMBER_PRICE_FIELD = "member_price";

	private final AdminMemberExportMemberCardGradesPort memberCardGradesPort;
	private final VipGradeListQueryService vipGradeListQueryService;

	public NormalGoodsUploadHeaderTitleService(
			AdminMemberExportMemberCardGradesPort memberCardGradesPort,
			VipGradeListQueryService vipGradeListQueryService) {
		this.memberCardGradesPort = memberCardGradesPort;
		this.vipGradeListQueryService = vipGradeListQueryService;
	}

	public UploadHeaderTitle forCompany(long companyId) {
		UploadHeaderTitle base = EspierUploadHeaderCatalog.NORMAL_GOODS();
		LinkedHashMap<String, String> all = new LinkedHashMap<>(base.all());
		all.remove("审核状态");
		LinkedHashMap<String, String> isNeed = new LinkedHashMap<>(base.isNeed());
		LinkedHashMap<String, UploadHeaderColumnInfo> headerInfo = new LinkedHashMap<>(NormalGoodsHeaderInfo.map());
		headerInfo.remove("审核状态");

		if (companyId > 0L) {
			expandMemberPriceColumns(all, headerInfo, companyId);
		}
		return new UploadHeaderTitle(all, isNeed, headerInfo);
	}

	private void expandMemberPriceColumns(
			LinkedHashMap<String, String> all,
			LinkedHashMap<String, UploadHeaderColumnInfo> headerInfo,
			long companyId) {
		UploadHeaderColumnInfo memberPriceInfo =
				headerInfo.remove(MEMBER_PRICE_LABEL);
		if (memberPriceInfo == null) {
			memberPriceInfo = new UploadHeaderColumnInfo(255, "单位为(元)，最多两位小数", false);
		}
		LinkedHashMap<String, String> rebuilt = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : all.entrySet()) {
			if (!MEMBER_PRICE_FIELD.equals(e.getValue())) {
				rebuilt.put(e.getKey(), e.getValue());
				continue;
			}
			appendGradeColumns(rebuilt, headerInfo, memberPriceInfo, companyId);
		}
		all.clear();
		all.putAll(rebuilt);
	}

	private void appendGradeColumns(
			LinkedHashMap<String, String> all,
			LinkedHashMap<String, UploadHeaderColumnInfo> headerInfo,
			UploadHeaderColumnInfo memberPriceInfo,
			long companyId) {
		List<Map<String, Object>> userGrades = memberCardGradesPort.getGradeListByCompanyId(companyId, false);
		if (userGrades != null) {
			for (Map<String, Object> grade : userGrades) {
				String name = stringField(grade, "grade_name");
				long gradeId = longField(grade, "grade_id");
				if (!name.isEmpty() && gradeId > 0L) {
					all.put(name, "grade_price" + gradeId);
					headerInfo.put(name, memberPriceInfo);
				}
			}
		}
		List<Map<String, Object>> vipGrades = vipGradeListQueryService.listVipGradesForWxappMembercardGrades(companyId);
		if (vipGrades != null) {
			for (Map<String, Object> grade : vipGrades) {
				String name = stringField(grade, "grade_name");
				long vipGradeId = longField(grade, "vip_grade_id");
				if (!name.isEmpty() && vipGradeId > 0L) {
					all.put(name, "vipGrade_price" + vipGradeId);
					headerInfo.put(name, memberPriceInfo);
				}
			}
		}
	}

	private static String stringField(Map<String, Object> row, String key) {
		Object v = row.get(key);
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long longField(Map<String, Object> row, String key) {
		Object v = row.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
