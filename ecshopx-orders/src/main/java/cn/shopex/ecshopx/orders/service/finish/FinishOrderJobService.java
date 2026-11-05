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

package cn.shopex.ecshopx.orders.service.finish;

import cn.shopex.ecshopx.members.service.systemtag.SleepingUserTagReconcileService;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinishOrderJobService {

	private final FinishOrderBatchService finishOrderBatchService;
	private final SleepingUserTagReconcileService sleepingUserTagReconcileService;

	public int execute(Map<String, Object> jobData) {
		Objects.requireNonNull(jobData, "jobData");
		int finished = finishOrderBatchService.runEquivalentToFinishOrderJobHandle();
		try {
			sleepingUserTagReconcileService.executeDaily();
		} catch (RuntimeException e) {
			log.error("[finish-orders] sleeping-user tag reconcile failed after batch", e);
		}
		return finished;
	}
}
