package cn.shopex.ecshopx.members.service.systemtag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberRelTagsAudit;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsAuditMapper;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class SleepingUserTagReconcileServiceTest {

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
	private SleepingUserLastCompletedOrderQuery lastCompletedOrderQuery;

	@Mock
	private MemberSystemTagSeedService memberSystemTagSeedService;

	@InjectMocks
	private SleepingUserTagReconcileService service;

	private MemberTags sleepingTag;
	private Members member;
	private long now;
	private long thresholdPlusOne;
	private long thresholdExact;

	@BeforeEach
	void setUp() {
		sleepingTag = new MemberTags();
		sleepingTag.setTagId(33L);
		sleepingTag.setCompanyId(7L);
		sleepingTag.setTagCode(MemberSystemTagCodes.SLEEPING_USER);
		sleepingTag.setTagKind(MemberSystemTagCodes.KIND_SYSTEM);
		member = new Members();
		member.setUserId(99L);
		member.setCompanyId(7L);
		member.setDisabled(false);
		now = 1_700_000_000L;
		thresholdExact = now - MemberSystemTagCodes.SLEEPING_THRESHOLD_SECONDS;
		thresholdPlusOne = thresholdExact - 1L;
	}

	@Test
	void isDesiredSleeping_thresholdExact_false_plusOne_true() {
		assertFalse(SleepingUserTagReconcileService.isDesiredSleeping(thresholdExact, now));
		assertTrue(SleepingUserTagReconcileService.isDesiredSleeping(thresholdPlusOne, now));
		assertFalse(SleepingUserTagReconcileService.isDesiredSleeping(null, now));
		assertFalse(SleepingUserTagReconcileService.isDesiredSleeping(0L, now));
	}

	@Test
	void reconcile_overThreshold_attaches() {
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(memberTagsMapper.selectOne(any())).thenReturn(sleepingTag);
		when(lastCompletedOrderQuery.findLastCompletedEndTime(7L, 99L)).thenReturn(thresholdPlusOne);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(null);

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_DAILY, now);

		verify(memberRelTagsMapper).insert(any(MemberRelTags.class));
		ArgumentCaptor<MemberRelTagsAudit> audit = ArgumentCaptor.forClass(MemberRelTagsAudit.class);
		verify(memberRelTagsAuditMapper).insert(audit.capture());
		assertEquals(MemberSystemTagCodes.ACTION_ATTACH, audit.getValue().getAction());
		assertEquals(MemberSystemTagCodes.SOURCE_DAILY, audit.getValue().getSource());
	}

	@Test
	void reconcile_exactThreshold_doesNotAttach() {
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(memberTagsMapper.selectOne(any())).thenReturn(sleepingTag);
		when(lastCompletedOrderQuery.findLastCompletedEndTime(7L, 99L)).thenReturn(thresholdExact);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(null);

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_DAILY, now);

		verify(memberRelTagsMapper, never()).insert(any(MemberRelTags.class));
		verify(memberRelTagsAuditMapper, never()).insert(any(MemberRelTagsAudit.class));
	}

	@Test
	void reconcile_noCompletedOrder_doesNotAttach() {
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(memberTagsMapper.selectOne(any())).thenReturn(sleepingTag);
		when(lastCompletedOrderQuery.findLastCompletedEndTime(7L, 99L)).thenReturn(null);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(null);

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_DAILY, now);

		verify(memberRelTagsMapper, never()).insert(any(MemberRelTags.class));
	}

	@Test
	void reconcile_recentCompleted_detachesExisting() {
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(memberTagsMapper.selectOne(any())).thenReturn(sleepingTag);
		when(lastCompletedOrderQuery.findLastCompletedEndTime(7L, 99L)).thenReturn(now - 10L);
		MemberRelTags rel = new MemberRelTags();
		rel.setId(5L);
		rel.setTagId(33L);
		rel.setCompanyId(7L);
		rel.setUserId(99L);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(rel);

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_DAILY, now);

		verify(memberRelTagsMapper).deleteById(5L);
		ArgumentCaptor<MemberRelTagsAudit> audit = ArgumentCaptor.forClass(MemberRelTagsAudit.class);
		verify(memberRelTagsAuditMapper).insert(audit.capture());
		assertEquals(MemberSystemTagCodes.ACTION_DETACH, audit.getValue().getAction());
	}

	@Test
	void reconcile_disabled_detachesExisting() {
		member.setDisabled(true);
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(memberTagsMapper.selectOne(any())).thenReturn(sleepingTag);
		MemberRelTags rel = new MemberRelTags();
		rel.setId(5L);
		rel.setTagId(33L);
		rel.setCompanyId(7L);
		rel.setUserId(99L);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(rel);

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_DAILY, now);

		verify(lastCompletedOrderQuery, never()).findLastCompletedEndTime(anyLong(), anyLong());
		verify(memberRelTagsMapper).deleteById(5L);
	}

	@Test
	void reconcile_duplicateKey_isIdempotent() {
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(memberTagsMapper.selectOne(any())).thenReturn(sleepingTag);
		when(lastCompletedOrderQuery.findLastCompletedEndTime(7L, 99L)).thenReturn(thresholdPlusOne);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(null);
		doThrow(new DuplicateKeyException("dup")).when(memberRelTagsMapper).insert(any(MemberRelTags.class));

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_DAILY, now);

		verify(memberRelTagsMapper).insert(any(MemberRelTags.class));
		verify(memberRelTagsAuditMapper, never()).insert(any(MemberRelTagsAudit.class));
	}

	@Test
	void reconcileCompany_pagesAttachCandidatesThenCompensatesExisting() {
		when(memberTagsMapper.selectOne(any())).thenReturn(sleepingTag);
		when(lastCompletedOrderQuery.listUserIdsLastCompletedBefore(eq(7L), anyLong(), eq(0L), eq(200)))
				.thenReturn(java.util.List.of(99L));
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(lastCompletedOrderQuery.findLastCompletedEndTime(7L, 99L)).thenReturn(thresholdPlusOne);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(null);
		when(memberRelTagsMapper.selectList(any())).thenReturn(java.util.List.of());

		service.reconcileCompany(7L, now);

		verify(lastCompletedOrderQuery, times(1))
				.listUserIdsLastCompletedBefore(eq(7L), anyLong(), eq(0L), eq(200));
		verify(memberRelTagsMapper).insert(any(MemberRelTags.class));
	}
}
