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

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
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
public class SleepingUserTagReconcileService {

	private static final Logger log = LoggerFactory.getLogger(SleepingUserTagReconcileService.class);
	private static final int ATTACH_PAGE_SIZE = 200;

	private final CompanysMapper companysMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final MemberRelTagsAuditMapper memberRelTagsAuditMapper;
	private final MembersMapper membersMapper;
	private final SleepingUserLastCompletedOrderQuery lastCompletedOrderQuery;
	private final MemberSystemTagSeedService memberSystemTagSeedService;

	public SleepingUserTagReconcileService(
			CompanysMapper companysMapper,
			MemberTagsMapper memberTagsMapper,
			MemberRelTagsMapper memberRelTagsMapper,
			MemberRelTagsAuditMapper memberRelTagsAuditMapper,
			MembersMapper membersMapper,
			SleepingUserLastCompletedOrderQuery lastCompletedOrderQuery,
			MemberSystemTagSeedService memberSystemTagSeedService) {
		this.companysMapper = companysMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.memberRelTagsAuditMapper = memberRelTagsAuditMapper;
		this.membersMapper = membersMapper;
		this.lastCompletedOrderQuery = lastCompletedOrderQuery;
		this.memberSystemTagSeedService = memberSystemTagSeedService;
	}

	public void executeDaily() {
		executeDaily(System.currentTimeMillis() / 1000L);
	}

	public void executeDaily(long nowEpochSec) {
		List<Companys> companies = companysMapper.selectList(null);
		if (companies == null || companies.isEmpty()) {
			return;
		}
		for (Companys company : companies) {
			if (company == null || company.getCompanyId() == null || company.getCompanyId() <= 0L) {
				continue;
			}
			try {
				reconcileCompany(company.getCompanyId(), nowEpochSec);
			} catch (RuntimeException e) {
				log.error("[sleeping-tag] company {} failed", company.getCompanyId(), e);
			}
		}
	}

	public void reconcileCompany(long companyId, long nowEpochSec) {
		MemberTags tag = requireSleepingTag(companyId);
		if (tag == null || tag.getTagId() == null) {
			return;
		}
		long before = nowEpochSec - MemberSystemTagCodes.SLEEPING_THRESHOLD_SECONDS;
		long offset = 0L;
		while (true) {
			List<Long> userIds =
					lastCompletedOrderQuery.listUserIdsLastCompletedBefore(
							companyId, before, offset, ATTACH_PAGE_SIZE);
			if (userIds == null || userIds.isEmpty()) {
				break;
			}
			for (Long userId : userIds) {
				if (userId != null && userId > 0L) {
					reconcile(companyId, userId, MemberSystemTagCodes.SOURCE_DAILY, nowEpochSec);
				}
			}
			if (userIds.size() < ATTACH_PAGE_SIZE) {
				break;
			}
			offset += ATTACH_PAGE_SIZE;
		}
		List<MemberRelTags> existing =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getCompanyId, companyId)
								.eq(MemberRelTags::getTagId, tag.getTagId()));
		if (existing == null || existing.isEmpty()) {
			return;
		}
		for (MemberRelTags rel : existing) {
			if (rel == null || rel.getUserId() == null || rel.getUserId() <= 0L) {
				continue;
			}
			reconcile(companyId, rel.getUserId(), MemberSystemTagCodes.SOURCE_DAILY, nowEpochSec);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void reconcile(long companyId, long userId, String source) {
		reconcile(companyId, userId, source, System.currentTimeMillis() / 1000L);
	}

	@Transactional(rollbackFor = Exception.class)
	public void reconcile(long companyId, long userId, String source, long nowEpochSec) {
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
		MemberTags tag = requireSleepingTag(companyId);
		if (tag == null || tag.getTagId() == null) {
			return;
		}
		MemberRelTags rel = findRel(companyId, userId, tag.getTagId());
		if (member == null || Boolean.TRUE.equals(member.getDisabled())) {
			if (rel != null) {
				detach(rel, src);
			}
			return;
		}
		boolean desired = isDesiredSleeping(companyId, userId, nowEpochSec);
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

	static boolean isDesiredSleeping(Long lastEnd, long nowEpochSec) {
		if (lastEnd == null || lastEnd <= 0L) {
			return false;
		}
		return (nowEpochSec - lastEnd) > MemberSystemTagCodes.SLEEPING_THRESHOLD_SECONDS;
	}

	private boolean isDesiredSleeping(long companyId, long userId, long nowEpochSec) {
		return isDesiredSleeping(lastCompletedOrderQuery.findLastCompletedEndTime(companyId, userId), nowEpochSec);
	}

	private MemberTags requireSleepingTag(long companyId) {
		MemberTags tag = findSleepingTag(companyId);
		if (tag != null) {
			return tag;
		}
		memberSystemTagSeedService.seedCompany(companyId);
		return findSleepingTag(companyId);
	}

	private MemberTags findSleepingTag(long companyId) {
		return memberTagsMapper.selectOne(
				new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getCompanyId, companyId)
						.eq(MemberTags::getTagCode, MemberSystemTagCodes.SLEEPING_USER)
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
			log.debug("sleeping rel already exists companyId={} userId={}", companyId, userId);
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
