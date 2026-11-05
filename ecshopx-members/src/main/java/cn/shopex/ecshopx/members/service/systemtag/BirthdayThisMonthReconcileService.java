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
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsAuditMapper;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BirthdayThisMonthReconcileService {

	private static final Logger log = LoggerFactory.getLogger(BirthdayThisMonthReconcileService.class);
	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final CompanysMapper companysMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final MemberRelTagsAuditMapper memberRelTagsAuditMapper;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final MemberSystemTagSeedService memberSystemTagSeedService;

	public BirthdayThisMonthReconcileService(
			CompanysMapper companysMapper,
			MemberTagsMapper memberTagsMapper,
			MemberRelTagsMapper memberRelTagsMapper,
			MemberRelTagsAuditMapper memberRelTagsAuditMapper,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			MemberSystemTagSeedService memberSystemTagSeedService) {
		this.companysMapper = companysMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.memberRelTagsAuditMapper = memberRelTagsAuditMapper;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.memberSystemTagSeedService = memberSystemTagSeedService;
	}

	public void executeMonthly() {
		executeMonthly(LocalDate.now(ZoneId.systemDefault()));
	}

	public void executeMonthly(LocalDate today) {
		if (today == null || today.getDayOfMonth() != 1) {
			log.info("[birthday-tag] skip, today={} is not the first day of month", today);
			return;
		}
		int month = today.getMonthValue();
		List<Companys> companies = companysMapper.selectList(null);
		int failed = 0;
		for (Companys company : companies) {
			if (company == null || company.getCompanyId() == null || company.getCompanyId() <= 0L) {
				continue;
			}
			try {
				reconcileCompany(company.getCompanyId(), month);
			} catch (RuntimeException e) {
				failed++;
				log.error("[birthday-tag] company {} failed", company.getCompanyId(), e);
			}
		}
		if (failed > 0) {
			throw new IllegalStateException("birthday tag job failed companies=" + failed);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void reconcileCompany(long companyId, int month) {
		memberSystemTagSeedService.seedCompany(companyId);
		MemberTags tag =
				memberTagsMapper.selectOne(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.eq(MemberTags::getTagCode, MemberSystemTagCodes.BIRTHDAY_THIS_MONTH)
								.last("LIMIT 1"));
		if (tag == null || tag.getTagId() == null) {
			return;
		}
		Map<Long, Boolean> disabledByUser = new HashMap<>();
		List<Members> members =
				membersMapper.selectList(new LambdaQueryWrapper<Members>().eq(Members::getCompanyId, companyId));
		if (members != null) {
			for (Members m : members) {
				if (m != null && m.getUserId() != null) {
					disabledByUser.put(m.getUserId(), Boolean.TRUE.equals(m.getDisabled()));
				}
			}
		}
		Set<Long> desired = new HashSet<>();
		List<MembersInfo> infos =
				membersInfoMapper.selectList(
						new LambdaQueryWrapper<MembersInfo>().eq(MembersInfo::getCompanyId, companyId));
		if (infos != null) {
			for (MembersInfo info : infos) {
				if (info == null || info.getUserId() == null) {
					continue;
				}
				Boolean disabled = disabledByUser.get(info.getUserId());
				if (disabled == null || Boolean.TRUE.equals(disabled)) {
					continue;
				}
				Integer birthMonth = resolveBirthMonth(info.getMonth(), info.getBirthday());
				if (birthMonth != null && birthMonth == month) {
					desired.add(info.getUserId());
				}
			}
		}
		List<MemberRelTags> rels =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getCompanyId, companyId)
								.eq(MemberRelTags::getTagId, tag.getTagId()));
		Set<Long> existing = new HashSet<>();
		if (rels != null) {
			for (MemberRelTags rel : rels) {
				if (rel == null || rel.getUserId() == null) {
					continue;
				}
				existing.add(rel.getUserId());
				if (!desired.contains(rel.getUserId())) {
					detach(rel);
				}
			}
		}
		for (Long userId : desired) {
			if (!existing.contains(userId)) {
				attach(companyId, userId, tag.getTagId());
			}
		}
	}

	static Integer resolveBirthMonth(Integer monthField, String birthday) {
		if (monthField != null && monthField >= 1 && monthField <= 12) {
			return monthField;
		}
		if (!StringUtils.hasText(birthday)) {
			return null;
		}
		String raw = birthday.trim();
		LocalDate d = parseBirthday(raw);
		return d == null ? null : d.getMonthValue();
	}

	private static LocalDate parseBirthday(String raw) {
		try {
			return LocalDate.parse(raw, ISO_DATE);
		} catch (DateTimeParseException ignored) {
		}
		try {
			return LocalDate.parse(raw, DATE_TIME);
		} catch (DateTimeParseException ignored) {
		}
		if (raw.length() >= 10) {
			try {
				return LocalDate.parse(raw.substring(0, 10), ISO_DATE);
			} catch (DateTimeParseException ignored) {
			}
		}
		return null;
	}

	private void attach(long companyId, long userId, long tagId) {
		long now = System.currentTimeMillis() / 1000L;
		MemberRelTags row = new MemberRelTags();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setTagId(tagId);
		row.setSource(MemberSystemTagCodes.SOURCE_COMPENSATE);
		row.setRuleVersion(MemberSystemTagCodes.RULE_VERSION);
		row.setEffectiveAt(now);
		row.setUpdatedAt(now);
		try {
			memberRelTagsMapper.insert(row);
		} catch (DuplicateKeyException e) {
			return;
		}
		insertAudit(companyId, userId, tagId, MemberSystemTagCodes.ACTION_ATTACH);
	}

	private void detach(MemberRelTags rel) {
		if (rel.getId() != null) {
			memberRelTagsMapper.deleteById(rel.getId());
		}
		insertAudit(
				rel.getCompanyId(),
				rel.getUserId(),
				rel.getTagId(),
				MemberSystemTagCodes.ACTION_DETACH);
	}

	private void insertAudit(long companyId, long userId, long tagId, String action) {
		MemberRelTagsAudit audit = new MemberRelTagsAudit();
		audit.setCompanyId(companyId);
		audit.setUserId(userId);
		audit.setTagId(tagId);
		audit.setAction(action);
		audit.setSource(MemberSystemTagCodes.SOURCE_COMPENSATE);
		audit.setRuleVersion(MemberSystemTagCodes.RULE_VERSION);
		audit.setReason("birthday_this_month");
		audit.setCreated(System.currentTimeMillis() / 1000L);
		memberRelTagsAuditMapper.insert(audit);
	}
}
