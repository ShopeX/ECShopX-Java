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

import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberRelTagsAudit;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsAuditMapper;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NewCustomerTagReconcileService {

	private static final Logger log = LoggerFactory.getLogger(NewCustomerTagReconcileService.class);

	private final MemberTagsMapper memberTagsMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final MemberRelTagsAuditMapper memberRelTagsAuditMapper;
	private final MembersMapper membersMapper;
	private final NewCustomerEffectivePaidOrderQuery effectivePaidOrderQuery;
	private final MemberSystemTagSeedService memberSystemTagSeedService;

	public NewCustomerTagReconcileService(
			MemberTagsMapper memberTagsMapper,
			MemberRelTagsMapper memberRelTagsMapper,
			MemberRelTagsAuditMapper memberRelTagsAuditMapper,
			MembersMapper membersMapper,
			NewCustomerEffectivePaidOrderQuery effectivePaidOrderQuery,
			MemberSystemTagSeedService memberSystemTagSeedService) {
		this.memberTagsMapper = memberTagsMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.memberRelTagsAuditMapper = memberRelTagsAuditMapper;
		this.membersMapper = membersMapper;
		this.effectivePaidOrderQuery = effectivePaidOrderQuery;
		this.memberSystemTagSeedService = memberSystemTagSeedService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void reconcile(long companyId, long userId, String source) {
		if (companyId <= 0L || userId <= 0L) {
			return;
		}
		String src = StringUtils.hasText(source) ? source.trim() : MemberSystemTagCodes.SOURCE_COMPENSATE;
		Members member =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		if (MemberSystemTagCodes.SOURCE_DISABLE.equals(src)
				|| member == null
				|| Boolean.TRUE.equals(member.getDisabled())) {
			detachAllSystemTags(companyId, userId, src);
			return;
		}
		long paid = effectivePaidOrderQuery.countEffectivePaid(companyId, userId);
		boolean desired = paid == 0L;
		MemberTags tag = requireNewCustomerTag(companyId);
		if (tag == null || tag.getTagId() == null) {
			return;
		}
		MemberRelTags rel = findRel(companyId, userId, tag.getTagId());
		if (desired) {
			if (rel == null) {
				attach(companyId, userId, tag.getTagId(), src);
			}
			return;
		}
		if (rel != null) {
			detach(rel, src);
		}
	}

	private MemberTags requireNewCustomerTag(long companyId) {
		MemberTags tag = findNewCustomerTag(companyId);
		if (tag != null) {
			return tag;
		}
		memberSystemTagSeedService.seedCompany(companyId);
		return findNewCustomerTag(companyId);
	}

	private MemberTags findNewCustomerTag(long companyId) {
		return memberTagsMapper.selectOne(
				new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getCompanyId, companyId)
						.eq(MemberTags::getTagCode, MemberSystemTagCodes.NEW_CUSTOMER)
						.last("LIMIT 1"));
	}

	private MemberRelTags findRel(long companyId, long userId, long tagId) {
		return memberRelTagsMapper.selectOne(
				new LambdaQueryWrapper<MemberRelTags>()
						.eq(MemberRelTags::getCompanyId, companyId)
						.eq(MemberRelTags::getUserId, userId)
						.eq(MemberRelTags::getTagId, tagId)
						.last("LIMIT 1"));
	}

	private void attach(long companyId, long userId, long tagId, String source) {
		long now = System.currentTimeMillis() / 1000L;
		MemberRelTags row = new MemberRelTags();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setTagId(tagId);
		row.setSource(source);
		row.setRuleVersion(MemberSystemTagCodes.RULE_VERSION);
		row.setEffectiveAt(now);
		row.setUpdatedAt(now);
		try {
			memberRelTagsMapper.insert(row);
		} catch (DuplicateKeyException e) {
			log.debug("new customer rel already exists companyId={} userId={}", companyId, userId);
			return;
		}
		insertAudit(companyId, userId, tagId, MemberSystemTagCodes.ACTION_ATTACH, source);
	}

	private void detach(MemberRelTags rel, String source) {
		if (rel.getId() != null) {
			memberRelTagsMapper.deleteById(rel.getId());
		}
		insertAudit(rel.getCompanyId(), rel.getUserId(), rel.getTagId(), MemberSystemTagCodes.ACTION_DETACH, source);
	}

	private void detachAllSystemTags(long companyId, long userId, String source) {
		List<MemberTags> tags =
				memberTagsMapper.selectList(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.eq(MemberTags::getTagKind, MemberSystemTagCodes.KIND_SYSTEM));
		if (tags == null || tags.isEmpty()) {
			return;
		}
		List<Long> tagIds = tags.stream().map(MemberTags::getTagId).filter(id -> id != null && id > 0L).toList();
		if (tagIds.isEmpty()) {
			return;
		}
		List<MemberRelTags> rels =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getCompanyId, companyId)
								.eq(MemberRelTags::getUserId, userId)
								.in(MemberRelTags::getTagId, tagIds));
		if (rels == null || rels.isEmpty()) {
			return;
		}
		for (MemberRelTags rel : rels) {
			detach(rel, source);
		}
	}

	private void insertAudit(long companyId, long userId, long tagId, String action, String source) {
		MemberRelTagsAudit audit = new MemberRelTagsAudit();
		audit.setCompanyId(companyId);
		audit.setUserId(userId);
		audit.setTagId(tagId);
		audit.setAction(action);
		audit.setSource(source);
		audit.setRuleVersion(MemberSystemTagCodes.RULE_VERSION);
		audit.setCreated(System.currentTimeMillis() / 1000L);
		memberRelTagsAuditMapper.insert(audit);
	}
}
