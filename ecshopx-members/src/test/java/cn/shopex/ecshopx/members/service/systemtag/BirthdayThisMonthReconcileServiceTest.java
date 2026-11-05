package cn.shopex.ecshopx.members.service.systemtag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsAuditMapper;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BirthdayThisMonthReconcileServiceTest {

	@Mock
	private CompanysMapper companysMapper;

	@Mock
	private MemberTagsMapper memberTagsMapper;

	@Mock
	private MemberRelTagsMapper memberRelTagsMapper;

	@Mock
	private MemberRelTagsAuditMapper memberRelTagsAuditMapper;

	@Mock
	private MembersMapper membersMapper;

	@Mock
	private MembersInfoMapper membersInfoMapper;

	@Mock
	private MemberSystemTagSeedService memberSystemTagSeedService;

	@InjectMocks
	private BirthdayThisMonthReconcileService service;

	@Test
	void resolveBirthMonth_prefersMonthField() {
		assertEquals(9, BirthdayThisMonthReconcileService.resolveBirthMonth(9, "1990-01-01"));
		assertEquals(3, BirthdayThisMonthReconcileService.resolveBirthMonth(0, "1990-03-15"));
		assertEquals(null, BirthdayThisMonthReconcileService.resolveBirthMonth(0, ""));
		assertEquals(null, BirthdayThisMonthReconcileService.resolveBirthMonth(13, "bad"));
	}

	@Test
	void executeMonthly_skipsWhenNotFirstDay() {
		service.executeMonthly(LocalDate.of(2026, 9, 21));
		verify(companysMapper, never()).selectList(any());
	}

	@Test
	void executeMonthly_attachesMatchingAndDetachesMismatch() {
		Companys company = new Companys();
		company.setCompanyId(7L);
		when(companysMapper.selectList(null)).thenReturn(List.of(company));
		MemberTags tag = new MemberTags();
		tag.setTagId(12L);
		tag.setTagCode(MemberSystemTagCodes.BIRTHDAY_THIS_MONTH);
		when(memberTagsMapper.selectOne(any())).thenReturn(tag);

		MembersInfo match = new MembersInfo();
		match.setUserId(1L);
		match.setCompanyId(7L);
		match.setMonth(9);
		MembersInfo mismatch = new MembersInfo();
		mismatch.setUserId(2L);
		mismatch.setCompanyId(7L);
		mismatch.setMonth(1);
		when(membersInfoMapper.selectList(any())).thenReturn(List.of(match, mismatch));
		Members enabled = new Members();
		enabled.setUserId(1L);
		enabled.setDisabled(false);
		Members enabled2 = new Members();
		enabled2.setUserId(2L);
		enabled2.setDisabled(false);
		when(membersMapper.selectList(any())).thenReturn(List.of(enabled, enabled2));

		MemberRelTags stale = new MemberRelTags();
		stale.setId(88L);
		stale.setTagId(12L);
		stale.setCompanyId(7L);
		stale.setUserId(2L);
		when(memberRelTagsMapper.selectList(any())).thenReturn(List.of(stale));

		service.executeMonthly(LocalDate.of(2026, 9, 1));

		verify(memberRelTagsMapper).insert(any(MemberRelTags.class));
		verify(memberRelTagsMapper).deleteById(88L);
	}
}
