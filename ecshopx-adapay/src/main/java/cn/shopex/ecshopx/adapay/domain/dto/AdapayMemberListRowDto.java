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

package cn.shopex.ecshopx.adapay.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Map;
import lombok.Data;

/**
 * 运营端 AdaPay 成员列表单行；与 {@code AdapayMemberRelCorpListMapper} 列一致。
 * <p>
 * 序列化使用 {@link PropertyNamingStrategies.SnakeCaseStrategy} 将 Java 属性名映射为 JSON snake_case。
 * {@link JsonInclude.Include#ALWAYS} 使各字段在值为 {@code null} 时仍输出 JSON 键，保证列表行键集稳定。
 * 外层 {@code ApiResult} 若使用 {@code NON_NULL}，嵌套 {@code Map} 中 null 值条目可能被省略，故列表行采用 DTO
 * 承载固定字段而非裸 {@code Map}。
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdapayMemberListRowDto {

	private Long id;
	private Long companyId;
	private String location;
	private String userName;
	private String memberType;
	private String operatorType;
	private String legalPerson;

	public static AdapayMemberListRowDto fromRow(Map<String, Object> row) {
		AdapayMemberListRowDto d = new AdapayMemberListRowDto();
		if (row == null || row.isEmpty()) {
			return d;
		}
		d.setId(toLong(row.get("id")));
		d.setCompanyId(toLong(row.get("company_id")));
		d.setLocation(toStringOrNull(row.get("location")));
		d.setUserName(toStringOrNull(row.get("user_name")));
		d.setMemberType(toStringOrNull(row.get("member_type")));
		d.setOperatorType(toStringOrNull(row.get("operator_type")));
		d.setLegalPerson(toStringOrNull(row.get("legal_person")));
		return d;
	}

	private static Long toLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String toStringOrNull(Object o) {
		if (o == null) {
			return null;
		}
		return o.toString();
	}
}
