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

package cn.shopex.ecshopx.kaquan.service.membercard;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.kaquan.domain.MemberCard;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberCardSetService {

	private static final String ERR_BRAND_NAME = "商户名称必填";
	private static final String ERR_LOGO_URL = "商户logo必传";
	private static final String ERR_TITLE = "会员卡名称必填";
	private static final String ERR_COLOR = "会员卡背景颜色必填";
	private static final String ERR_CODE_TYPE = "会员卡code类型必选";

	private final MemberCardMapper memberCardMapper;

	public MemberCardSetService(MemberCardMapper memberCardMapper) {
		this.memberCardMapper = memberCardMapper;
	}

	public Map<String, Object> setMemberCard(long companyId, Map<String, Object> input) {
		validateRequired(input);

		int now = (int) (System.currentTimeMillis() / 1000L);
		MemberCard existing = memberCardMapper.selectById(companyId);

		MemberCard row;
		if (existing == null) {
			row = new MemberCard();
			row.setCompanyId(companyId);
			row.setCreated(now);
			row.setUpdated(now);
			applyInputFields(row, input);
			memberCardMapper.insert(row);
		} else {
			row = existing;
			row.setUpdated(now);
			applyInputFields(row, input);
			memberCardMapper.updateById(row);
		}

		return toResponseMap(row);
	}

	public Map<String, Object> getMemberCard(long companyId) {
		MemberCard row = memberCardMapper.selectById(companyId);
		if (row == null) {
			return new LinkedHashMap<>();
		}
		return toResponseMap(row);
	}

	private void validateRequired(Map<String, Object> input) {
		List<String> errors = new ArrayList<>();
		if (isMissingOrBlank(input, "brand_name")) {
			errors.add(ERR_BRAND_NAME);
		}
		if (isMissingOrBlank(input, "logo_url")) {
			errors.add(ERR_LOGO_URL);
		}
		if (isMissingOrBlank(input, "title")) {
			errors.add(ERR_TITLE);
		}
		if (isMissingOrBlank(input, "color")) {
			errors.add(ERR_COLOR);
		}
		if (isMissingOrBlank(input, "code_type")) {
			errors.add(ERR_CODE_TYPE);
		}
		if (!errors.isEmpty()) {
			throw new BadRequestException(String.join("，", errors));
		}
	}

	private static boolean isMissingOrBlank(Map<String, Object> input, String key) {
		if (!input.containsKey(key)) {
			return true;
		}
		Object v = input.get(key);
		if (v == null) {
			return true;
		}
		String s = v instanceof String str ? str : String.valueOf(v);
		return s.trim().isEmpty();
	}

	private static void applyInputFields(MemberCard row, Map<String, Object> input) {
		if (input.containsKey("brand_name")) {
			row.setBrandName(toStringValue(input.get("brand_name")));
		}
		if (input.containsKey("logo_url")) {
			row.setLogoUrl(toStringValue(input.get("logo_url")));
		}
		if (input.containsKey("title")) {
			row.setTitle(toStringValue(input.get("title")));
		}
		if (input.containsKey("color")) {
			row.setColor(toStringValue(input.get("color")));
		}
		if (input.containsKey("code_type")) {
			row.setCodeType(toStringValue(input.get("code_type")));
		}
		if (input.containsKey("background_pic_url")) {
			Object v = input.get("background_pic_url");
			row.setBackgroundPicUrl(v == null ? null : toStringValue(v));
		}
	}

	private static String toStringValue(Object v) {
		if (v == null) {
			return null;
		}
		return v instanceof String s ? s : String.valueOf(v);
	}

	private static Map<String, Object> toResponseMap(MemberCard e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", e.getCompanyId());
		m.put("brand_name", e.getBrandName());
		m.put("logo_url", e.getLogoUrl());
		m.put("title", e.getTitle());
		m.put("color", e.getColor());
		m.put("code_type", e.getCodeType());
		m.put("background_pic_url", e.getBackgroundPicUrl());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}
}
