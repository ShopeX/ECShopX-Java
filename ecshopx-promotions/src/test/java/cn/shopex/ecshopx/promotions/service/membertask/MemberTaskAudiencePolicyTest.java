package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MemberTaskAudiencePolicyTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void visible_union_matchesAnyTag() {
		MemberTask task = new MemberTask();
		task.setAudienceMode("visible");
		task.setAudienceTagCombine("union");
		task.setAudienceTagIds("[10,20]");
		assertThat(MemberTaskAudiencePolicy.visibleToUser(task, Set.of(20L), objectMapper))
				.isTrue();
		assertThat(MemberTaskAudiencePolicy.visibleToUser(task, Set.of(99L), objectMapper))
				.isFalse();
	}

	@Test
	void hidden_excludesMatchingUsers() {
		MemberTask task = new MemberTask();
		task.setAudienceMode("hidden");
		task.setAudienceTagCombine("union");
		task.setAudienceTagIds("[10]");
		assertThat(MemberTaskAudiencePolicy.visibleToUser(task, Set.of(10L), objectMapper))
				.isFalse();
		assertThat(MemberTaskAudiencePolicy.visibleToUser(task, Set.of(), objectMapper)).isTrue();
	}
}
