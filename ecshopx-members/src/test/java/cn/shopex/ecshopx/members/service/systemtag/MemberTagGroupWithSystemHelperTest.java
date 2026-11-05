package cn.shopex.ecshopx.members.service.systemtag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MemberTagGroupWithSystemHelperTest {

	@Test
	void parseWithSystem_acceptsTruthy() {
		assertTrue(MemberTagGroupWithSystemHelper.parseWithSystem("1"));
		assertTrue(MemberTagGroupWithSystemHelper.parseWithSystem("true"));
		assertTrue(MemberTagGroupWithSystemHelper.parseWithSystem("YES"));
		assertFalse(MemberTagGroupWithSystemHelper.parseWithSystem(null));
		assertFalse(MemberTagGroupWithSystemHelper.parseWithSystem(""));
		assertFalse(MemberTagGroupWithSystemHelper.parseWithSystem("0"));
		assertFalse(MemberTagGroupWithSystemHelper.parseWithSystem("false"));
	}

	@Test
	void shouldPrepend_onlyFirstPage() {
		assertTrue(MemberTagGroupWithSystemHelper.shouldPrepend(true, 1));
		assertTrue(MemberTagGroupWithSystemHelper.shouldPrepend(true, 0));
		assertFalse(MemberTagGroupWithSystemHelper.shouldPrepend(true, 2));
		assertFalse(MemberTagGroupWithSystemHelper.shouldPrepend(false, 1));
	}

	@Test
	void buildVirtualSystemGroup_shape() {
		Map<String, Object> tag =
				Map.of(
						"tag_id",
						"1",
						"type",
						MemberSystemTagCodes.KIND_SYSTEM,
						"code",
						MemberSystemTagCodes.NEW_CUSTOMER);
		Map<String, Object> group =
				MemberTagGroupWithSystemHelper.buildVirtualSystemGroup(141L, List.of(tag));
		assertEquals(MemberTagGroupWithSystemHelper.VIRTUAL_GROUP_ID, group.get("group_id"));
		assertEquals(MemberTagGroupWithSystemHelper.VIRTUAL_GROUP_NAME, group.get("group_name"));
		assertEquals(141, group.get("company_id"));
		assertEquals(0, group.get("distributor_id"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> tags = (List<Map<String, Object>>) group.get("tags");
		assertEquals(1, tags.size());
		assertEquals(MemberSystemTagCodes.NEW_CUSTOMER, tags.get(0).get("code"));
	}
}
