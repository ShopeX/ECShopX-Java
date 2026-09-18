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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.export.port.AdminMemberExportMemberCardGradesPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberTagSegmentRuleStructureService {

	private final AdminMemberExportMemberCardGradesPort adminMemberExportMemberCardGradesPort;

	public MemberTagSegmentRuleStructureService(
			AdminMemberExportMemberCardGradesPort adminMemberExportMemberCardGradesPort) {
		this.adminMemberExportMemberCardGradesPort = adminMemberExportMemberCardGradesPort;
	}

	public List<Map<String, Object>> getRuleStructure(long companyId) {
		try {
			List<Map<String, Object>> gradeRows =
					adminMemberExportMemberCardGradesPort.getGradeListByCompanyId(companyId, false);

			List<Map<String, Object>> mapValue = new ArrayList<>();
			if (gradeRows != null) {
				for (Map<String, Object> row : gradeRows) {
					Object gid = row.get("grade_id");
					if (gid == null || String.valueOf(gid).trim().isEmpty()) {
						continue;
					}
					Long rowId = null;
					if (gid instanceof Number) {
						Number parsed = (Number) gid;
						rowId = Long.valueOf(((Number) parsed).longValue());
					} else {
						String s = String.valueOf(gid).trim();
						try {
							rowId = Long.valueOf(Long.parseLong(s));
						} catch (NumberFormatException ex) {
							continue;
						}
					}
					Object nameObj = row.get("grade_name");
					String name = nameObj == null ? "" : String.valueOf(nameObj);
					LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
					entry.put("id", rowId);
					entry.put("name", name);
					mapValue.add(entry);
				}
			}

			LinkedHashMap<String, Object> memberRoot = new LinkedHashMap<>();
			memberRoot.put("type", "member");
			memberRoot.put("total_condition", new ArrayList<>());

			List<Map<String, Object>> memberSub = new ArrayList<>();

			LinkedHashMap<String, Object> birthday = new LinkedHashMap<>();
			birthday.put("type", "birthday");
			birthday.put("lebel", "生日");
			birthday.put("condition_type", "timeRange");
			memberSub.add(birthday);

			LinkedHashMap<String, Object> grade = new LinkedHashMap<>();
			grade.put("type", "grade");
			grade.put("lebel", "会员等级");
			grade.put("condition_type", "mapping");
			grade.put("map_value", mapValue);
			memberSub.add(grade);

			LinkedHashMap<String, Object> point = new LinkedHashMap<>();
			point.put("type", "point");
			point.put("lebel", "会员积分");
			point.put("condition_type", "number_rang");
			point.put("unit", "积分");
			memberSub.add(point);

			memberRoot.put("sub", memberSub);

			LinkedHashMap<String, Object> orderRoot = new LinkedHashMap<>();
			orderRoot.put("type", "order");

			LinkedHashMap<String, Object> totalCond0 = new LinkedHashMap<>();
			totalCond0.put("condition_type", "timeRange");
			totalCond0.put("lebel", "订单时间范围");
			totalCond0.put("unit", "");
			List<Map<String, Object>> totalCondition = new ArrayList<>();
			totalCondition.add(totalCond0);
			orderRoot.put("total_condition", totalCondition);

			List<Map<String, Object>> orderSub = new ArrayList<>();

			LinkedHashMap<String, Object> perOrder = new LinkedHashMap<>();
			perOrder.put("type", "perOrder");
			perOrder.put("lebel", "单笔金额");
			perOrder.put("condition_type", "number_rang");
			perOrder.put("unit", "元");
			orderSub.add(perOrder);

			LinkedHashMap<String, Object> sumaryOrder = new LinkedHashMap<>();
			sumaryOrder.put("type", "sumaryOrder");
			sumaryOrder.put("lebel", "累计金额");
			sumaryOrder.put("condition_type", "number_rang");
			sumaryOrder.put("map_value", "元");
			orderSub.add(sumaryOrder);

			LinkedHashMap<String, Object> orderItem = new LinkedHashMap<>();
			orderItem.put("type", "orderItem");
			orderItem.put("lebel", "下单商品");
			orderItem.put("condition_type", "number_items");
			orderItem.put("map_value", "");
			orderSub.add(orderItem);

			LinkedHashMap<String, Object> hasOrder = new LinkedHashMap<>();
			hasOrder.put("type", "hasOrder");
			hasOrder.put("lebel", "商城有下单");
			hasOrder.put("condition_type", "radio");
			List<Map<String, Object>> hasOrderMapValue = new ArrayList<>();
			LinkedHashMap<String, Object> hasOrderNo = new LinkedHashMap<>();
			hasOrderNo.put("id", Long.valueOf(0));
			hasOrderNo.put("name", "否");
			hasOrderNo.put("is_default", Integer.valueOf(1));
			hasOrderMapValue.add(hasOrderNo);
			LinkedHashMap<String, Object> hasOrderYes = new LinkedHashMap<>();
			hasOrderYes.put("id", Long.valueOf(1));
			hasOrderYes.put("name", "是");
			hasOrderYes.put("is_default", Integer.valueOf(0));
			hasOrderMapValue.add(hasOrderYes);
			hasOrder.put("map_value", hasOrderMapValue);
			orderSub.add(hasOrder);

			orderRoot.put("sub", orderSub);

			return List.of(memberRoot, orderRoot);
		} catch (Exception e) {
			throw new ResourceException("获取规则结构失败：" + e.getMessage());
		}
	}
}
