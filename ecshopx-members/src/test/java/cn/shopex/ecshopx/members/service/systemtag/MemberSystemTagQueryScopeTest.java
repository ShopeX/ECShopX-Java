package cn.shopex.ecshopx.members.service.systemtag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MemberSystemTagQueryScopeTest {

	@Test
	void normalizeTagKind_acceptsManualAndSystem() {
		assertEquals("system", MemberSystemTagQueryScope.normalizeTagKind("system"));
		assertEquals("manual", MemberSystemTagQueryScope.normalizeTagKind(" manual "));
		assertNull(MemberSystemTagQueryScope.normalizeTagKind(null));
		assertNull(MemberSystemTagQueryScope.normalizeTagKind("staff"));
	}
}
