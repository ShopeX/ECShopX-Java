package cn.shopex.ecshopx.members.service.systemtag;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberAudienceTagValidatorTest {

	@Mock
	private MemberTagsMapper memberTagsMapper;

	@InjectMocks
	private MemberAudienceTagValidator validator;

	@Test
	void assertUsable_skipsEmpty() {
		assertDoesNotThrow(() -> validator.assertUsable(1L, 3L, List.of()));
	}

	@Test
	void assertUsable_rejectsMissingTag() {
		when(memberTagsMapper.selectList(any())).thenReturn(List.of());
		assertThrows(ResourceException.class, () -> validator.assertUsable(1L, 3L, List.of(9L)));
	}

	@Test
	void assertUsable_allowsSystemTagRegardlessDistributor() {
		MemberTags system = new MemberTags();
		system.setTagId(9L);
		system.setCompanyId(1L);
		system.setDistributorId(0L);
		system.setTagKind(MemberSystemTagCodes.KIND_SYSTEM);
		when(memberTagsMapper.selectList(any())).thenReturn(List.of(system));
		assertDoesNotThrow(() -> validator.assertUsable(1L, 8L, List.of(9L)));
	}

	@Test
	void assertUsable_rejectsManualTagOfOtherDistributor() {
		MemberTags manual = new MemberTags();
		manual.setTagId(9L);
		manual.setCompanyId(1L);
		manual.setDistributorId(2L);
		manual.setTagKind(MemberSystemTagCodes.KIND_MANUAL);
		when(memberTagsMapper.selectList(any())).thenReturn(List.of(manual));
		assertThrows(ResourceException.class, () -> validator.assertUsable(1L, 8L, List.of(9L)));
	}

	@Test
	void assertUsable_rejectsTagNotInCurrentCompany() {
		when(memberTagsMapper.selectList(any())).thenReturn(List.of());
		assertThrows(ResourceException.class, () -> validator.assertUsable(1L, 0L, List.of(99L)));
	}
}
