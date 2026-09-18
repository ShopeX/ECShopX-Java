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

package cn.shopex.ecshopx.bspay.dispatch;

import cn.shopex.ecshopx.bspay.service.WithdrawApplyService;
import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WithdrawJobHandler implements DispatchHandler {

	private final WithdrawApplyService withdrawApplyService;

	public WithdrawJobHandler(WithdrawApplyService withdrawApplyService) {
		this.withdrawApplyService = withdrawApplyService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		Object raw = payload == null ? null : payload.get("apply_id");
		if (!(raw instanceof Number)) {
			throw new BadRequestException("apply_id required");
		}
		long applyId = ((Number) raw).longValue();
		withdrawApplyService.executeHuifuWithdraw(applyId);
	}
}
