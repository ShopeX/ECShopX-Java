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

package cn.shopex.ecshopx.comments.api.front.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ECX-10918：公开评论列表 pageSize 须有上限（对齐 PHP MAX_PAGE_SIZE=50）。 */
class CommentsControllerPageSizeTest {

	@Test
	void oversizedPageSize_isCappedToFifty() {
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("pageSize", "100000");

		assertThat(CommentsController.parsePageSizeForTest(q)).isEqualTo(50);
	}

	@Test
	void smallPageSize_passesThrough() {
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("pageSize", "20");

		assertThat(CommentsController.parsePageSizeForTest(q)).isEqualTo(20);
	}

	@Test
	void defaultPageSize_isFifty() {
		assertThat(CommentsController.parsePageSizeForTest(new LinkedHashMap<>())).isEqualTo(50);
	}

	@Test
	void invalidPageSize_stillRejected() {
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("pageSize", "abc");

		assertThatThrownBy(() -> CommentsController.parsePageSizeForTest(q))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("每页条数格式无效");
	}
}
