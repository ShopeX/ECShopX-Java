package cn.shopex.ecshopx.members.service.systemtag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberRelTagsAudit;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsAuditMapper;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class NewCustomerTagReconcileServiceTest {

	@Mock
	private MemberTagsMapper memberTagsMapper;

	@Mock
	private MemberRelTagsMapper memberRelTagsMapper;

	@Mock
	private MemberRelTagsAuditMapper memberRelTagsAuditMapper;

	@Mock
	private MembersMapper membersMapper;

	@Mock
	private NewCustomerEffectivePaidOrderQuery effectivePaidOrderQuery;

	@Mock
	private MemberSystemTagSeedService memberSystemTagSeedService;

	@InjectMocks
	private NewCustomerTagReconcileService service;

	private MemberTags newCustomerTag;
	private Members member;

	@BeforeEach
	void setUp() {
		newCustomerTag = new MemberTags();
		newCustomerTag.setTagId(11L);
		newCustomerTag.setCompanyId(7L);
		newCustomerTag.setTagCode(MemberSystemTagCodes.NEW_CUSTOMER);
		newCustomerTag.setTagKind(MemberSystemTagCodes.KIND_SYSTEM);
		member = new Members();
		member.setUserId(99L);
		member.setCompanyId(7L);
		member.setDisabled(false);
	}

	@Test
	void reconcile_zeroPaid_attachesOnce() {
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(memberTagsMapper.selectOne(any())).thenReturn(newCustomerTag);
		when(effectivePaidOrderQuery.countEffectivePaid(7L, 99L)).thenReturn(0L);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(null);

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_REGISTER);

		verify(memberRelTagsMapper).insert(any(MemberRelTags.class));
		ArgumentCaptor<MemberRelTagsAudit> audit = ArgumentCaptor.forClass(MemberRelTagsAudit.class);
		verify(memberRelTagsAuditMapper, times(1)).insert(audit.capture());
		assertEquals(MemberSystemTagCodes.ACTION_ATTACH, audit.getValue().getAction());
		assertEquals(MemberSystemTagCodes.SOURCE_REGISTER, audit.getValue().getSource());
	}

	@Test
	void reconcile_paidOrder_detachesExisting() {
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(memberTagsMapper.selectOne(any())).thenReturn(newCustomerTag);
		when(effectivePaidOrderQuery.countEffectivePaid(7L, 99L)).thenReturn(1L);
		MemberRelTags rel = new MemberRelTags();
		rel.setId(5L);
		rel.setTagId(11L);
		rel.setCompanyId(7L);
		rel.setUserId(99L);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(rel);

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_PAY);

		verify(memberRelTagsMapper).deleteById(5L);
		ArgumentCaptor<MemberRelTagsAudit> audit = ArgumentCaptor.forClass(MemberRelTagsAudit.class);
		verify(memberRelTagsAuditMapper).insert(audit.capture());
		assertEquals(MemberSystemTagCodes.ACTION_DETACH, audit.getValue().getAction());
		assertEquals(MemberSystemTagCodes.SOURCE_PAY, audit.getValue().getSource());
	}

	@Test
	void reconcile_paidOrder_withoutRel_skips() {
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(memberTagsMapper.selectOne(any())).thenReturn(newCustomerTag);
		when(effectivePaidOrderQuery.countEffectivePaid(7L, 99L)).thenReturn(1L);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(null);

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_PAY);

		verify(memberRelTagsMapper, never()).insert(any(MemberRelTags.class));
		verify(memberRelTagsMapper, never()).deleteById(anyLong());
		verify(memberRelTagsAuditMapper, never()).insert(any(MemberRelTagsAudit.class));
	}

	@Test
	void reconcile_disabled_detachesAllSystemTags() {
		member.setDisabled(true);
		when(membersMapper.selectOne(any())).thenReturn(member);
		MemberTags birthday = new MemberTags();
		birthday.setTagId(12L);
		birthday.setTagKind(MemberSystemTagCodes.KIND_SYSTEM);
		when(memberTagsMapper.selectList(any())).thenReturn(List.of(newCustomerTag, birthday));
		MemberRelTags rel1 = new MemberRelTags();
		rel1.setId(1L);
		rel1.setTagId(11L);
		rel1.setCompanyId(7L);
		rel1.setUserId(99L);
		MemberRelTags rel2 = new MemberRelTags();
		rel2.setId(2L);
		rel2.setTagId(12L);
		rel2.setCompanyId(7L);
		rel2.setUserId(99L);
		when(memberRelTagsMapper.selectList(any())).thenReturn(List.of(rel1, rel2));

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_DISABLE);

		verify(effectivePaidOrderQuery, never()).countEffectivePaid(anyLong(), anyLong());
		verify(memberRelTagsMapper).deleteById(1L);
		verify(memberRelTagsMapper).deleteById(2L);
		verify(memberRelTagsAuditMapper, times(2)).insert(any(MemberRelTagsAudit.class));
	}

	@Test
	void reconcile_alreadyAttached_skipsInsert() {
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(memberTagsMapper.selectOne(any())).thenReturn(newCustomerTag);
		when(effectivePaidOrderQuery.countEffectivePaid(7L, 99L)).thenReturn(0L);
		MemberRelTags rel = new MemberRelTags();
		rel.setId(5L);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(rel);

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_REFUND);

		verify(memberRelTagsMapper, never()).insert(any(MemberRelTags.class));
		verify(memberRelTagsAuditMapper, never()).insert(any(MemberRelTagsAudit.class));
	}

	@Test
	void reconcile_duplicateKey_doesNotWriteAudit() {
		when(membersMapper.selectOne(any())).thenReturn(member);
		when(memberTagsMapper.selectOne(any())).thenReturn(newCustomerTag);
		when(effectivePaidOrderQuery.countEffectivePaid(7L, 99L)).thenReturn(0L);
		when(memberRelTagsMapper.selectOne(any())).thenReturn(null);
		doThrow(new DuplicateKeyException("uk")).when(memberRelTagsMapper).insert(any(MemberRelTags.class));

		service.reconcile(7L, 99L, MemberSystemTagCodes.SOURCE_PAY);

		verify(memberRelTagsAuditMapper, never()).insert(any(MemberRelTagsAudit.class));
	}
}
