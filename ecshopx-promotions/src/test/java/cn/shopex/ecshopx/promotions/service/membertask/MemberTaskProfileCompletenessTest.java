package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MemberTaskProfileCompletenessTest {

	@Test
	void incomplete_whenRequiredUsernameMissing() {
		Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
		fields.put("username", field(1, 1));
		fields.put("mobile", field(1, 1));
		Map<String, Object> member = new LinkedHashMap<>();
		member.put("mobile", "13800138000");
		member.put("username", "");

		assertThat(MemberTaskProfileCompleteness.isComplete(fields, member)).isFalse();
	}

	@Test
	void complete_whenDefaultRequiredFilled() {
		Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
		fields.put("username", field(1, 1));
		fields.put("mobile", field(1, 1));
		fields.put("email", field(1, 0));
		Map<String, Object> member = new LinkedHashMap<>();
		member.put("username", "张三");
		member.put("mobile", "13800138000");

		assertThat(MemberTaskProfileCompleteness.isComplete(fields, member)).isTrue();
	}

	@Test
	void complete_checksCustomRequiredInOtherParams() {
		Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
		fields.put("username", field(1, 1));
		fields.put("custom_job", Map.of("is_required", 1, "is_default", 0));
		Map<String, Object> member = new LinkedHashMap<>();
		member.put("username", "李四");
		member.put("other_params", Map.of("custom_data", Map.of("custom_job", "工程师")));

		assertThat(MemberTaskProfileCompleteness.isComplete(fields, member)).isTrue();
	}

	@Test
	void incomplete_whenCustomRequiredMissing() {
		Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
		fields.put("custom_job", Map.of("is_required", 1, "is_default", 0));
		Map<String, Object> member = new LinkedHashMap<>();
		member.put("other_params", Map.of("custom_data", List.of()));

		assertThat(MemberTaskProfileCompleteness.isComplete(fields, member)).isFalse();
	}

	private static Map<String, Object> field(int isDefault, int isRequired) {
		return Map.of("is_default", isDefault, "is_required", isRequired);
	}
}
