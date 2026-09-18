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

package cn.shopex.ecshopx.community.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Map;
import lombok.Data;

/**
 * 管理端团长列表单行 DTO；字段与 {@code selectChiefListByDistributorJoin} 结果行（snake_case 列名）一一对应。
 * <p>
 * 使用 {@link JsonInclude.Include#ALWAYS}，避免外层 {@code ApiResult} 的 {@code NON_NULL} 在序列化嵌套 {@code Map} 时吃掉 null 键。
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CommunityChiefListRowDto {

	private Long chiefId;
	private Long companyId;
	private String chiefName;
	private String chiefAvatar;
	private String chiefMobile;
	private String chiefDesc;
	private String chiefIntro;
	private String province;
	private String city;
	private String area;
	private String regionsId;
	private String regions;
	private String address;
	private String lng;
	private String lat;
	private Long userId;
	private Long createdAt;
	private Long updatedAt;
	private String alipayName;
	private String alipayAccount;
	private String bankName;
	private String bankcardNo;
	private Long applyTime;
	private Long approveTime;
	private Long distributorId;
	private Integer source;
	private String extraData;

	public static CommunityChiefListRowDto fromRow(Map<String, Object> row) {
		CommunityChiefListRowDto d = new CommunityChiefListRowDto();
		if (row == null || row.isEmpty()) {
			return d;
		}
		d.setChiefId(toLong(getCi(row, "chief_id")));
		d.setCompanyId(toLong(getCi(row, "company_id")));
		d.setChiefName(toStringOrNull(getCi(row, "chief_name")));
		d.setChiefAvatar(toStringOrNull(getCi(row, "chief_avatar")));
		d.setChiefMobile(toStringOrNull(getCi(row, "chief_mobile")));
		d.setChiefDesc(toStringOrNull(getCi(row, "chief_desc")));
		d.setChiefIntro(toStringOrNull(getCi(row, "chief_intro")));
		d.setProvince(toStringOrNull(getCi(row, "province")));
		d.setCity(toStringOrNull(getCi(row, "city")));
		d.setArea(toStringOrNull(getCi(row, "area")));
		d.setRegionsId(toStringOrNull(getCi(row, "regions_id")));
		d.setRegions(toStringOrNull(getCi(row, "regions")));
		d.setAddress(toStringOrNull(getCi(row, "address")));
		d.setLng(toStringOrNull(getCi(row, "lng")));
		d.setLat(toStringOrNull(getCi(row, "lat")));
		d.setUserId(toLong(getCi(row, "user_id")));
		d.setCreatedAt(toLong(getCi(row, "created_at")));
		d.setUpdatedAt(toLong(getCi(row, "updated_at")));
		d.setAlipayName(toStringOrNull(getCi(row, "alipay_name")));
		d.setAlipayAccount(toStringOrNull(getCi(row, "alipay_account")));
		d.setBankName(toStringOrNull(getCi(row, "bank_name")));
		d.setBankcardNo(toStringOrNull(getCi(row, "bankcard_no")));
		d.setApplyTime(toLong(getCi(row, "apply_time")));
		d.setApproveTime(toLong(getCi(row, "approve_time")));
		d.setDistributorId(toLong(getCi(row, "distributor_id")));
		d.setSource(toInteger(getCi(row, "source")));
		d.setExtraData(toStringOrNull(getCi(row, "extra_data")));
		return d;
	}

	private static Object getCi(Map<String, Object> row, String key) {
		if (row.containsKey(key)) {
			return row.get(key);
		}
		for (Map.Entry<String, Object> e : row.entrySet()) {
			String k = e.getKey();
			if (k != null && k.equalsIgnoreCase(key)) {
				return e.getValue();
			}
		}
		return null;
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

	private static Integer toInteger(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
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
