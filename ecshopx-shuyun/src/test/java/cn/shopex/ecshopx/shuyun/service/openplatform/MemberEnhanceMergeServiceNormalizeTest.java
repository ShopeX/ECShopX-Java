package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MemberEnhanceMergeServiceNormalizeTest {

	private final ObjectMapper om = new ObjectMapper();

	@Test
	void normalize_fromDataMember() throws Exception {
		ObjectNode root = om.createObjectNode();
		ObjectNode data = root.putObject("data");
		ObjectNode member = data.putObject("member");
		member.put("realName", "张三");
		member.put("birthday", "2000-01-02");
		member.put("gender", "M");
		Map<String, Object> snap = MemberEnhanceMergeService.normalizePayload(root);
		assertEquals("张三", snap.get("name"));
		assertEquals("2000-01-02", snap.get("birthday"));
		assertEquals("M", snap.get("gender"));
	}

	@Test
	void mapGender() {
		assertEquals("1", MemberEnhanceMergeService.mapGenderToLocalSex("male"));
		assertEquals("2", MemberEnhanceMergeService.mapGenderToLocalSex("女"));
		assertNull(MemberEnhanceMergeService.mapGenderToLocalSex(""));
	}
}
