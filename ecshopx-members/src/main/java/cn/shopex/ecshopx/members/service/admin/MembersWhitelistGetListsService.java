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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.domain.MembersWhitelist;
import cn.shopex.ecshopx.members.mapper.MembersWhitelistMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MembersWhitelistGetListsService {

	private static final Pattern CN_MOBILE = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MembersWhitelistMapper membersWhitelistMapper;

	public MembersWhitelistGetListsService(MembersWhitelistMapper membersWhitelistMapper) {
		this.membersWhitelistMapper = membersWhitelistMapper;
	}

	public Map<String, Object> getLists(long companyId, Map<String, Object> query) {
		int pageNum = firstIntMin(query.get("page"), 1, "分页参数错误");
		int pageSize = firstIntMin(query.get("pageSize"), 1, "每页显示数量最大100");
		if (pageSize > 100) {
			throw new BadRequestException("每页显示数量最大100");
		}
		validateMobileQuery(query);

		LambdaQueryWrapper<MembersWhitelist> w = new LambdaQueryWrapper<MembersWhitelist>()
				.eq(MembersWhitelist::getCompanyId, companyId);
		if (query.containsKey("mobile")) {
			String mobileRaw = String.valueOf(query.get("mobile")).trim();
			if (StringUtils.hasText(mobileRaw)) {
				w.eq(MembersWhitelist::getMobile, LegacyFixedMobileEncrypt.fixedEncryptMobile(mobileRaw));
			}
		}
		w.orderByDesc(MembersWhitelist::getCreated).orderByDesc(MembersWhitelist::getWhitelistId);

		long total = membersWhitelistMapper.selectCount(w);
		List<Map<String, Object>> list = new ArrayList<>();
		if (total > 0) {
			Page<MembersWhitelist> page = new Page<>(pageNum, pageSize, false);
			membersWhitelistMapper.selectPage(page, w);
			for (MembersWhitelist row : page.getRecords()) {
				LinkedHashMap<String, Object> item = new LinkedHashMap<>();
				item.put("whitelist_id", row.getWhitelistId());
				item.put("company_id", row.getCompanyId());
				item.put("mobile", LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(row.getMobile()));
				item.put("name", LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(row.getName()));
				item.put("created", row.getCreated());
				item.put("updated", row.getUpdated());
				list.add(item);
			}
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		result.put("datapass_block", 0);
		return result;
	}

	private static void validateMobileQuery(Map<String, Object> query) {
		if (!query.containsKey("mobile")) {
			return;
		}
		Object raw = query.get("mobile");
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			throw new BadRequestException("请填写正确的手机号");
		}
		String trimmed = String.valueOf(raw).trim();
		if (!CN_MOBILE.matcher(trimmed).matches()) {
			throw new BadRequestException("请填写正确的手机号");
		}
	}

	private static int firstIntMin(Object raw, int min, String err) {
		int v = (int) parseLongStrict(raw, err);
		if (v < min) {
			throw new BadRequestException(err);
		}
		return v;
	}

	private static long parseLongStrict(Object raw, String err) {
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (Exception e) {
			throw new BadRequestException(err);
		}
	}
}
