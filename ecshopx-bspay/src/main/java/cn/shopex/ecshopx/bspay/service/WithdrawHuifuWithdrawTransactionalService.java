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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.domain.WithdrawApply;
import cn.shopex.ecshopx.bspay.mapper.WithdrawApplyMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawHuifuWithdrawTransactionalService {

	private final WithdrawApplyMapper withdrawApplyMapper;
	private final BsPayWithdrawExecuteService bsPayWithdrawExecuteService;

	@Transactional(rollbackFor = Exception.class)
	public void runWithdrawEncashmentInTransaction(
			long applyId, Map<String, Object> doWithdrawParams, WithdrawApply applyForLog) {
		Objects.requireNonNull(applyForLog, "applyForLog");
		if (applyForLog.getId() == null || !applyForLog.getId().equals(applyId)) {
			throw new IllegalStateException("applyForLog must be the row for applyId=" + applyId);
		}

		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<WithdrawApply> lockUw = Wrappers.lambdaUpdate();
		lockUw.eq(WithdrawApply::getId, applyId)
				.in(WithdrawApply::getStatus, List.of(1, 5))
				.set(WithdrawApply::getStatus, 3)
				.set(WithdrawApply::getUpdated, nowSec);
		int u = withdrawApplyMapper.update(null, lockUw);
		if (u == 0) {
			throw new ResourceException("提现申请状态已发生变化，无法执行");
		}

		int requestTime = (int) (System.currentTimeMillis() / 1000L);
		Map<String, Object> result = bsPayWithdrawExecuteService.doWithdraw(doWithdrawParams);

		String transStat = str(result.get("trans_stat"));
		int persistNow = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<WithdrawApply> uw2 = Wrappers.lambdaUpdate();
		uw2.eq(WithdrawApply::getId, applyId)
				.set(WithdrawApply::getHfSeqId, str(result.get("hf_seq_id")))
				.set(WithdrawApply::getReqSeqId, str(result.get("req_seq_id")))
				.set(WithdrawApply::getRequestTime, requestTime)
				.set(WithdrawApply::getUpdated, persistNow);
		if ("P".equals(transStat)) {
			uw2.set(WithdrawApply::getStatus, 3)
					.set(WithdrawApply::getFailureReason, "取现申请已受理，正在处理中");
		} else {
			uw2.set(WithdrawApply::getStatus, 4).set(WithdrawApply::getFailureReason, "");
		}
		withdrawApplyMapper.update(null, uw2);

		log.info(
				"bspay::doWithdraw::提现申请处理成功::apply_id:{},huifu_id:{},amount:{},hf_seq_id:{}",
				applyId,
				applyForLog.getHuifuId(),
				applyForLog.getAmount(),
				result.get("hf_seq_id"));
	}

	private static String str(Object o) {
		return o == null ? "" : Objects.toString(o, "").trim();
	}
}
