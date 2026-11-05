package cn.shopex.ecshopx.members.service.systemtag;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberTags;
import org.junit.jupiter.api.Test;

class MemberSystemTagWriteGuardTest {

	private final MemberSystemTagWriteGuard guard = new MemberSystemTagWriteGuard(null);

	@Test
	void rejectWrite_throwsWhenTagKindIsSystem() {
		MemberTags tag = new MemberTags();
		tag.setTagKind(MemberSystemTagCodes.KIND_SYSTEM);
		tag.setTagCode(MemberSystemTagCodes.NEW_CUSTOMER);
		assertThrows(ResourceException.class, () -> guard.rejectWrite(tag));
	}

	@Test
	void rejectWrite_allowsManualTag() {
		MemberTags tag = new MemberTags();
		tag.setTagKind(MemberSystemTagCodes.KIND_MANUAL);
		assertDoesNotThrow(() -> guard.rejectWrite(tag));
	}

	@Test
	void rejectCreateReservedCode_throwsForBuiltinCodes() {
		assertThrows(ResourceException.class, () -> guard.rejectCreateReservedCode("new_customer"));
		assertThrows(ResourceException.class, () -> guard.rejectCreateReservedCode("birthday_this_month"));
		assertThrows(ResourceException.class, () -> guard.rejectCreateReservedCode("sleeping_user"));
	}

	@Test
	void rejectCreatePayload_rejectsSystemKindAndReservedCode() {
		assertThrows(ResourceException.class, () -> guard.rejectCreatePayload(java.util.Map.of("tag_kind", "system")));
		assertThrows(ResourceException.class, () -> guard.rejectCreatePayload(java.util.Map.of("code", "new_customer")));
		assertDoesNotThrow(() -> guard.rejectCreatePayload(java.util.Map.of("tag_name", "VIP")));
	}
}
