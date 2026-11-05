package cn.shopex.ecshopx.members.service.systemtag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTags;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MemberDetailTagFieldsAssemblerTest {

	private final MemberDetailTagFieldsAssembler assembler = new MemberDetailTagFieldsAssembler();

	@Test
	void assemble_splitsManualAndSystemAndKeepsMixedTagList() {
		MemberTags manual = new MemberTags();
		manual.setTagId(11L);
		manual.setTagName("VIP");
		manual.setTagKind(MemberSystemTagCodes.KIND_MANUAL);
		manual.setUpdated(100L);

		MemberTags system = new MemberTags();
		system.setTagId(22L);
		system.setTagName("新客");
		system.setTagCode(MemberSystemTagCodes.NEW_CUSTOMER);
		system.setTagKind(MemberSystemTagCodes.KIND_SYSTEM);
		system.setDescription("当前没有有效已付订单的会员");
		system.setRuleVersion(1);
		system.setUpdated(200L);

		MemberRelTags rel = new MemberRelTags();
		rel.setTagId(22L);
		rel.setEffectiveAt(50L);
		rel.setUpdatedAt(200L);

		Map<String, Object> fields =
				assembler.assemble(List.of(manual, system), Map.of(22L, rel), 999L);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> tagList = (List<Map<String, Object>>) fields.get("tagList");
		assertEquals(2, tagList.size());
		assertEquals("manual", tagList.get(0).get("type"));
		assertEquals("system", tagList.get(1).get("type"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> operation = (List<Map<String, Object>>) fields.get("operation_tags");
		assertEquals(1, operation.size());
		assertEquals(11L, operation.get(0).get("id"));
		assertEquals("VIP", operation.get(0).get("name"));
		assertEquals("manual", operation.get(0).get("type"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> systemTags = (List<Map<String, Object>>) fields.get("system_tags");
		assertEquals(1, systemTags.size());
		assertEquals("new_customer", systemTags.get(0).get("code"));
		assertEquals(50L, systemTags.get(0).get("effective_at"));

		@SuppressWarnings("unchecked")
		Map<String, Object> summary = (Map<String, Object>) fields.get("tag_summary");
		assertEquals(1, summary.get("operation_count"));
		assertEquals(1, summary.get("system_count"));
		assertEquals(999L, summary.get("snapshot_at"));

		@SuppressWarnings("unchecked")
		Map<String, Object> permissions = (Map<String, Object>) fields.get("tag_permissions");
		assertEquals(Boolean.TRUE, permissions.get("can_view"));
		assertEquals(Boolean.TRUE, permissions.get("can_add_operation"));
		assertEquals(Boolean.TRUE, permissions.get("can_remove_operation"));
		assertFalse((Boolean) permissions.get("can_edit_system"));
		assertEquals("ready", fields.get("tag_data_status"));
	}

	@Test
	void assemble_emptyTags_returnsReadyEmptyBuckets() {
		Map<String, Object> fields = assembler.assemble(List.of(), Map.of(), 1L);
		assertEquals(List.of(), fields.get("tagList"));
		assertEquals(List.of(), fields.get("operation_tags"));
		assertEquals(List.of(), fields.get("system_tags"));
		assertEquals("ready", fields.get("tag_data_status"));
	}
}
