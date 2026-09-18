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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.kaquan.domain.MemberCard;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberCardUpdateService {

	private final MemberCardMapper memberCardMapper;

	public OpenapiThirdApiV2MemberCardUpdateService(MemberCardMapper memberCardMapper) {
		this.memberCardMapper = memberCardMapper;
	}

	public void executeOpenapiUpdate(long companyId, Map<String, Object> params) {
		if (params == null || params.isEmpty()) {
			return;
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		MemberCard existing = memberCardMapper.selectById(companyId);
		if (existing == null) {
			MemberCard row = new MemberCard();
			row.setCompanyId(companyId);
			row.setCreated(now);
			row.setUpdated(now);
			applyOpenapiFields(row, params);
			memberCardMapper.insert(row);
		} else {
			existing.setUpdated(now);
			applyOpenapiFields(existing, params);
			memberCardMapper.updateById(existing);
		}
	}

	private static void applyOpenapiFields(MemberCard row, Map<String, Object> params) {
		if (params.containsKey("brand_name")) {
			row.setBrandName(toStringValue(params.get("brand_name")));
		}
		if (params.containsKey("logo_url")) {
			row.setLogoUrl(toStringValue(params.get("logo_url")));
		}
		if (params.containsKey("title")) {
			row.setTitle(toStringValue(params.get("title")));
		}
		if (params.containsKey("color")) {
			row.setColor(toStringValue(params.get("color")));
		}
		if (params.containsKey("background_pic_url")) {
			row.setBackgroundPicUrl(toStringValue(params.get("background_pic_url")));
		}
	}

	private static String toStringValue(Object v) {
		if (v == null) {
			return null;
		}
		return v instanceof String s ? s : String.valueOf(v);
	}
}
