package cn.shopex.ecshopx.members.service.systemtag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberSystemTagSeedServiceTest {

	@Mock
	private MemberTagsMapper memberTagsMapper;

	@InjectMocks
	private MemberSystemTagSeedService seedService;

	@Test
	void seedCompany_insertsMissingSystemTags() {
		when(memberTagsMapper.selectCount(any())).thenReturn(0L);
		seedService.seedCompany(88L);
		ArgumentCaptor<MemberTags> captor = ArgumentCaptor.forClass(MemberTags.class);
		verify(memberTagsMapper, times(3)).insert(captor.capture());
		assertEquals(MemberSystemTagCodes.NEW_CUSTOMER, captor.getAllValues().get(0).getTagCode());
		assertEquals(MemberSystemTagCodes.BIRTHDAY_THIS_MONTH, captor.getAllValues().get(1).getTagCode());
		assertEquals(MemberSystemTagCodes.SLEEPING_USER, captor.getAllValues().get(2).getTagCode());
		assertEquals(0L, captor.getAllValues().get(0).getDistributorId());
		assertEquals(MemberSystemTagCodes.KIND_SYSTEM, captor.getAllValues().get(0).getTagKind());
	}

	@Test
	void seedCompany_skipsWhenAllExist() {
		when(memberTagsMapper.selectCount(any())).thenReturn(1L);
		seedService.seedCompany(88L);
		verify(memberTagsMapper, never()).insert(any(MemberTags.class));
	}
}
