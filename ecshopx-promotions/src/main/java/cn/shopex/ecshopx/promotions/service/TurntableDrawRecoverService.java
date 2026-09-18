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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawStatus;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawStructuredLog;
import cn.shopex.ecshopx.promotions.mapper.TurntableLogMapper;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * PROCESSING 超时补偿（PRD §6.4.1）：禁止盲扣积分、禁止盲建发奖单；按 process_step 续跑主路径。
 */
@Service
public class TurntableDrawRecoverService {

	private static final int DEFAULT_TIMEOUT_SECONDS = 60;
	private static final int BATCH_LIMIT = 200;

	private final TurntableLogMapper turntableLogMapper;
	private final TurntableFrontJoinTurntableService joinService;

	public TurntableDrawRecoverService(
			TurntableLogMapper turntableLogMapper, @Lazy TurntableFrontJoinTurntableService joinService) {
		this.turntableLogMapper = turntableLogMapper;
		this.joinService = joinService;
	}

	public int recoverStuck() {
		return recoverStuck(DEFAULT_TIMEOUT_SECONDS);
	}

	public int recoverStuck(int timeoutSeconds) {
		int effectiveTimeout = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
		int cutoff = (int) Math.min(System.currentTimeMillis() / 1000L - effectiveTimeout, Integer.MAX_VALUE);
		List<TurntableLog> stuck = turntableLogMapper.selectStuckProcessing(cutoff, BATCH_LIMIT);
		int recovered = 0;
		for (TurntableLog row : stuck) {
			if (recoverOne(row)) {
				recovered++;
			}
		}
		TurntableDrawStructuredLog.infoRecoverBatch(recovered, stuck.size(), effectiveTimeout);
		return recovered;
	}

	public boolean recoverOne(TurntableLog log) {
		if (log == null || !TurntableDrawStatus.PROCESSING.equals(log.getStatus())) {
			return false;
		}
		return joinService.resumeProcessing(log);
	}
}
