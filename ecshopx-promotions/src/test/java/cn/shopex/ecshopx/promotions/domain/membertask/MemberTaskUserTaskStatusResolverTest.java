package cn.shopex.ecshopx.promotions.domain.membertask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MemberTaskUserTaskStatusResolverTest {

	@Test
	void resolve_grantStatuses() {
		assertThat(MemberTaskUserTaskStatusResolver.resolve(null))
				.isEqualTo(MemberTaskUserTaskStatus.TODO);
		assertThat(MemberTaskUserTaskStatusResolver.resolve("pending"))
				.isEqualTo(MemberTaskUserTaskStatus.PENDING_REWARD);
		assertThat(MemberTaskUserTaskStatusResolver.resolve("granted"))
				.isEqualTo(MemberTaskUserTaskStatus.REWARDED);
		assertThat(MemberTaskUserTaskStatusResolver.resolve("failed"))
				.isEqualTo(MemberTaskUserTaskStatus.REWARD_FAILED);
		assertThat(MemberTaskUserTaskStatusResolver.resolve("cancelled"))
				.isEqualTo(MemberTaskUserTaskStatus.TODO);
	}

	@Test
	void shouldShow_expiredTodoHidden() {
		assertThat(
						MemberTaskUserTaskStatusResolver.shouldShowInFrontList(
								MemberTaskDisplayStatus.EXPIRED, MemberTaskUserTaskStatus.TODO))
				.isFalse();
		assertThat(
						MemberTaskUserTaskStatusResolver.shouldShowInFrontList(
								MemberTaskDisplayStatus.EXPIRED, MemberTaskUserTaskStatus.PENDING_REWARD))
				.isTrue();
	}
}
