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

package cn.shopex.ecshopx.config;

import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.GoodsSearchDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.goods.search.rebuild.SearchRebuildJobHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchRebuildJobRegistrationConfigTest {

	@Mock private DispatchRegistry dispatchRegistry;
	@Mock private SearchRebuildJobHandler searchRebuildJobHandler;

	@Test
	void registerSearchRebuildJob_usesGoodsSearchDispatchJobName() {
		new SearchRebuildJobRegistrationConfig(dispatchRegistry, searchRebuildJobHandler).registerSearchRebuildJob();
		verify(dispatchRegistry).registerJob(GoodsSearchDispatchJobNames.SEARCH_REBUILD_JOB, searchRebuildJobHandler);
	}
}
