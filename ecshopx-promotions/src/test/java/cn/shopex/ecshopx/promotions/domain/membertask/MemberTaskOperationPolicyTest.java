/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.promotions.domain.membertask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MemberTaskOperationPolicyTest {

	@Test
	void publishOnlyFromDraftBeforeEnd() {
		assertThat(MemberTaskOperationPolicy.canPublish(MemberTaskDisplayStatus.DRAFT, 200, 150))
				.isTrue();
		assertThat(MemberTaskOperationPolicy.canPublish(MemberTaskDisplayStatus.DRAFT, 100, 150))
				.isFalse();
		assertThat(MemberTaskOperationPolicy.canPublish(MemberTaskDisplayStatus.IN_PROGRESS, 200, 150))
				.isFalse();
	}

	@Test
	void offShelfFromPendingOrInProgress() {
		assertThat(MemberTaskOperationPolicy.canOffShelf(MemberTaskDisplayStatus.PENDING_START))
				.isTrue();
		assertThat(MemberTaskOperationPolicy.canOffShelf(MemberTaskDisplayStatus.IN_PROGRESS))
				.isTrue();
		assertThat(MemberTaskOperationPolicy.canOffShelf(MemberTaskDisplayStatus.DRAFT)).isFalse();
	}

	@Test
	void editOnlyDraft() {
		assertThat(MemberTaskOperationPolicy.canEdit(MemberTaskDisplayStatus.DRAFT)).isTrue();
		assertThat(MemberTaskOperationPolicy.canEdit(MemberTaskDisplayStatus.IN_PROGRESS)).isFalse();
	}
}
