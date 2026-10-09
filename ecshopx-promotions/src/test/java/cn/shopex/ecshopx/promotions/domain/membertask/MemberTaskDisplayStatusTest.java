package cn.shopex.ecshopx.promotions.domain.membertask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MemberTaskDisplayStatusTest {

	@Test
	void draftWhenNotPublished() {
		assertThat(MemberTaskDisplayStatus.resolve(0, 0, 100, 200, 150)).isEqualTo(MemberTaskDisplayStatus.DRAFT);
	}

	@Test
	void expiredWhenUnpublishedPastEndG3() {
		assertThat(MemberTaskDisplayStatus.resolve(0, 0, 100, 120, 150))
				.isEqualTo(MemberTaskDisplayStatus.EXPIRED);
	}

	@Test
	void pendingStartWhenPublishedBeforeBegin() {
		assertThat(MemberTaskDisplayStatus.resolve(1, 0, 200, 300, 150))
				.isEqualTo(MemberTaskDisplayStatus.PENDING_START);
	}

	@Test
	void inProgressWhenInsideWindow() {
		assertThat(MemberTaskDisplayStatus.resolve(1, 0, 100, 300, 150))
				.isEqualTo(MemberTaskDisplayStatus.IN_PROGRESS);
	}

	@Test
	void expiredWhenPastEnd() {
		assertThat(MemberTaskDisplayStatus.resolve(1, 0, 100, 200, 250))
				.isEqualTo(MemberTaskDisplayStatus.EXPIRED);
	}

	@Test
	void expiredWhenOffShelf() {
		assertThat(MemberTaskDisplayStatus.resolve(1, 1, 100, 300, 150))
				.isEqualTo(MemberTaskDisplayStatus.EXPIRED);
	}
}
