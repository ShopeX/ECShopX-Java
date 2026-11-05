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

package cn.shopex.ecshopx.members.service.systemtag;

import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberSystemTagSeedService {

	private final MemberTagsMapper memberTagsMapper;

	public MemberSystemTagSeedService(MemberTagsMapper memberTagsMapper) {
		this.memberTagsMapper = memberTagsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void seedCompany(long companyId) {
		ensureTag(
				companyId,
				MemberSystemTagCodes.NEW_CUSTOMER,
				MemberSystemTagCodes.NEW_CUSTOMER_NAME,
				MemberSystemTagCodes.NEW_CUSTOMER_DESC);
		ensureTag(
				companyId,
				MemberSystemTagCodes.BIRTHDAY_THIS_MONTH,
				MemberSystemTagCodes.BIRTHDAY_NAME,
				MemberSystemTagCodes.BIRTHDAY_DESC);
		ensureTag(
				companyId,
				MemberSystemTagCodes.SLEEPING_USER,
				MemberSystemTagCodes.SLEEPING_USER_NAME,
				MemberSystemTagCodes.SLEEPING_USER_DESC);
	}

	private void ensureTag(long companyId, String code, String name, String description) {
		LambdaQueryWrapper<MemberTags> q = new LambdaQueryWrapper<>();
		q.eq(MemberTags::getCompanyId, companyId).eq(MemberTags::getTagCode, code);
		Long count = memberTagsMapper.selectCount(q);
		if (count != null && count > 0L) {
			return;
		}
		long nowSec = System.currentTimeMillis() / 1000L;
		MemberTags row = new MemberTags();
		row.setCompanyId(companyId);
		row.setDistributorId(0L);
		row.setTagName(name);
		row.setDescription(description);
		row.setTagColor("#ff1939");
		row.setFontColor("#ffffff");
		row.setTagStatus("online");
		row.setSource("self");
		row.setTagCode(code);
		row.setTagKind(MemberSystemTagCodes.KIND_SYSTEM);
		row.setRuleVersion(MemberSystemTagCodes.RULE_VERSION);
		row.setCreated(nowSec);
		row.setUpdated(nowSec);
		memberTagsMapper.insert(row);
	}
}
