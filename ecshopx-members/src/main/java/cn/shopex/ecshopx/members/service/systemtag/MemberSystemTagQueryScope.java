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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.util.StringUtils;

public final class MemberSystemTagQueryScope {

	private MemberSystemTagQueryScope() {}

	public static String normalizeTagKind(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String t = raw.trim();
		if (MemberSystemTagCodes.KIND_SYSTEM.equals(t) || MemberSystemTagCodes.KIND_MANUAL.equals(t)) {
			return t;
		}
		return null;
	}

	public static void applyListScope(
			LambdaQueryWrapper<MemberTags> w, long companyId, long distributorId, String tagKind) {
		w.eq(MemberTags::getCompanyId, companyId);
		String kind = normalizeTagKind(tagKind);
		if (MemberSystemTagCodes.KIND_SYSTEM.equals(kind)) {
			w.eq(MemberTags::getTagKind, MemberSystemTagCodes.KIND_SYSTEM);
			return;
		}
		if (MemberSystemTagCodes.KIND_MANUAL.equals(kind)) {
			w.eq(MemberTags::getDistributorId, distributorId);
			w.and(q -> q.isNull(MemberTags::getTagKind)
					.or()
					.ne(MemberTags::getTagKind, MemberSystemTagCodes.KIND_SYSTEM));
			return;
		}
		w.and(q -> q.eq(MemberTags::getDistributorId, distributorId)
				.or()
				.eq(MemberTags::getTagKind, MemberSystemTagCodes.KIND_SYSTEM));
	}

	public static void applyDetailScope(
			LambdaQueryWrapper<MemberTags> w, long companyId, long distributorId, long tagId) {
		w.eq(MemberTags::getCompanyId, companyId).eq(MemberTags::getTagId, tagId);
		w.and(q -> q.eq(MemberTags::getDistributorId, distributorId)
				.or()
				.eq(MemberTags::getTagKind, MemberSystemTagCodes.KIND_SYSTEM));
	}
}
