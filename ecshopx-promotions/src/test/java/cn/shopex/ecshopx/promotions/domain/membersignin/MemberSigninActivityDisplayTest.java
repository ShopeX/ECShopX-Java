package cn.shopex.ecshopx.promotions.domain.membersignin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MemberSigninActivityDisplayTest {

	@Test
	void resolve_notStarted_inProgress_ended() {
		long now = 1_000L;
		assertThat(MemberSigninActivityDisplay.resolve(now + 100, now + 200, now))
				.isEqualTo(MemberSigninDisplayStatus.NOT_STARTED);
		assertThat(MemberSigninActivityDisplay.resolve(now - 100, now + 100, now))
				.isEqualTo(MemberSigninDisplayStatus.IN_PROGRESS);
		assertThat(MemberSigninActivityDisplay.resolve(now - 200, now - 100, now))
				.isEqualTo(MemberSigninDisplayStatus.ENDED);
	}

	@Test
	void resolve_cancelledWhenEndBeforeBegin() {
		assertThat(MemberSigninActivityDisplay.resolve(2_000L, 1_000L, 500L))
				.isEqualTo(MemberSigninDisplayStatus.ENDED);
	}
}
