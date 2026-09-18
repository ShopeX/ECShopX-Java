package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class MemberEnhanceCardSyncServiceTest {

	private final ObjectMapper om = new ObjectMapper();

	@Test
	void extractMemberIdFromNestedData() throws Exception {
		ObjectNode root = om.createObjectNode();
		ObjectNode data = root.putObject("data");
		data.put("memberId", "CARD-9");
		assertEquals("CARD-9", MemberEnhanceCardSyncService.extractMemberId(root));
	}

	@Test
	void extractMemberIdFromFlat() throws Exception {
		ObjectNode root = om.createObjectNode();
		root.put("member_id", "M1");
		assertEquals("M1", MemberEnhanceCardSyncService.extractMemberId(root));
	}
}
