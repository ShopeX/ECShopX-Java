package cn.shopex.ecshopx.promotions.service.systemtag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberCrowdMatcherTest {

	@Mock
	private MemberRelTagsMapper memberRelTagsMapper;

	@InjectMocks
	private MemberCrowdMatcher matcher;

	@Test
	void emptyTagIds_alwaysMatch() {
		assertTrue(matcher.matchesMemberTags(1L, 0L, "[]", "include"));
		assertTrue(matcher.matchesMemberTags(1L, 9L, null, "exclude"));
	}

	@Test
	void guest_withTags_hidden() {
		assertFalse(matcher.matchesMemberTags(1L, 0L, "[11]", "include"));
	}

	@Test
	void include_anyHit() {
		MemberRelTags rel = new MemberRelTags();
		rel.setTagId(11L);
		when(memberRelTagsMapper.selectList(any())).thenReturn(List.of(rel));
		assertTrue(matcher.matchesMemberTags(1L, 9L, "[11,22]", "include"));
		assertFalse(matcher.matchesMemberTags(1L, 9L, "[33]", "include"));
	}

	@Test
	void exclude_anyHit_blocks() {
		MemberRelTags rel = new MemberRelTags();
		rel.setTagId(11L);
		when(memberRelTagsMapper.selectList(any())).thenReturn(List.of(rel));
		assertFalse(matcher.matchesMemberTags(1L, 9L, "[11]", "exclude"));
		assertTrue(matcher.matchesMemberTags(1L, 9L, "[33]", "exclude"));
	}

	@Test
	void missingMode_defaultsInclude() {
		MemberRelTags rel = new MemberRelTags();
		rel.setTagId(11L);
		when(memberRelTagsMapper.selectList(any())).thenReturn(List.of(rel));
		assertTrue(matcher.matchesMemberTags(1L, 9L, "[11]", null));
	}

	@Test
	void otherMarketingType_skipsCrowd() {
		assertTrue(matcher.matchesFullCrowdActivity(1L, 0L, "plus_price_buy", "[11]", "include"));
	}

	@Test
	void normalSeckill_skipsCrowd() {
		assertTrue(matcher.matchesLimitedTimeSale(1L, 0L, "normal", "[11]", "include"));
	}
}
