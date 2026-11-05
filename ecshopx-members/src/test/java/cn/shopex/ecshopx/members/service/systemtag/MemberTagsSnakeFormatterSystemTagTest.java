package cn.shopex.ecshopx.members.service.systemtag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.service.MemberTagsSnakeFormatter;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MemberTagsSnakeFormatterSystemTagTest {

	private final MemberTagsSnakeFormatter formatter = new MemberTagsSnakeFormatter();

	@Test
	void toSnakeTagRow_exposesTypeCodeAndRuleVersion() {
		MemberTags tag = new MemberTags();
		tag.setTagId(9L);
		tag.setCompanyId(1L);
		tag.setTagName("新客");
		tag.setTagCode(MemberSystemTagCodes.NEW_CUSTOMER);
		tag.setTagKind(MemberSystemTagCodes.KIND_SYSTEM);
		tag.setRuleVersion(1);
		tag.setTagStatus("online");
		tag.setTagColor("#ff1939");
		tag.setFontColor("#ffffff");
		tag.setDistributorId(0L);
		tag.setCreated(1L);
		tag.setUpdated(1L);
		Map<String, Object> row = formatter.toSnakeTagRow(tag);
		assertEquals("system", row.get("type"));
		assertEquals("new_customer", row.get("code"));
		assertEquals(1, row.get("rule_version"));
	}

	@Test
	void toSnakeTagRow_manualDefaultsTypeManual() {
		MemberTags tag = new MemberTags();
		tag.setTagId(2L);
		tag.setCompanyId(1L);
		tag.setTagName("VIP");
		tag.setTagKind(null);
		tag.setTagStatus("online");
		tag.setTagColor("#ff1939");
		tag.setFontColor("#ffffff");
		tag.setDistributorId(3L);
		tag.setCreated(1L);
		tag.setUpdated(1L);
		Map<String, Object> row = formatter.toSnakeTagRow(tag);
		assertEquals("manual", row.get("type"));
		assertEquals(null, row.get("code"));
	}
}
