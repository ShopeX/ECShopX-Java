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

package cn.shopex.ecshopx.companys.service.datapass;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.OperatorDataPass;
import cn.shopex.ecshopx.companys.mapper.OperatorDataPassMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OperatorDataPassApproveService {

	private final OperatorDataPassMapper operatorDataPassMapper;

	public OperatorDataPassApproveService(OperatorDataPassMapper operatorDataPassMapper) {
		this.operatorDataPassMapper = operatorDataPassMapper;
	}

	/**
	 * 管理端审核或关闭敏感数据申请：按 query 分支更新 {@code operator_data_pass}。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void approveDataPass(
			long companyId, long passId, String statusRaw, String remarksRaw, String isClosedRaw) {
		String remarks = (remarksRaw == null || remarksRaw.isBlank()) ? "" : remarksRaw.trim();
		boolean approvalBranch = isApproveStatusQueryActive(statusRaw);
		boolean closeBranch = isClosedRaw != null;

		OperatorDataPass detail =
				operatorDataPassMapper.selectOne(
						new LambdaQueryWrapper<OperatorDataPass>()
								.eq(OperatorDataPass::getPassId, passId)
								.eq(OperatorDataPass::getCompanyId, companyId));
		if (detail == null) {
			throw new ResourceException("数据不存在");
		}

		if (approvalBranch) {
			int statusInt;
			try {
				statusInt = Integer.parseInt(statusRaw.trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("状态错误");
			}
			if (!List.of(1, 2).contains(statusInt)) {
				throw new ResourceException("状态错误");
			}
			if (detail.getStatus() == null || detail.getStatus() != 0) {
				throw new ResourceException("状态错误");
			}
			detail.setStatus(statusInt);
			detail.setApproveTime((int) Instant.now().getEpochSecond());
			if (StringUtils.hasText(remarks)) {
				detail.setRemarks(remarks);
			}
			int rows = operatorDataPassMapper.updateById(detail);
			if (rows != 1) {
				throw new ResourceException("未查询到更新数据");
			}
			return;
		}

		if (closeBranch) {
			if (detail.getStatus() == null || detail.getStatus() != 1) {
				throw new ResourceException("状态错误");
			}
			int closedBit = closedBitFromIsClosedParam(isClosedRaw);
			detail.setIsClosed(closedBit);
			int rows = operatorDataPassMapper.updateById(detail);
			if (rows != 1) {
				throw new ResourceException("未查询到更新数据");
			}
			return;
		}

		throw new ResourceException("状态错误");
	}

	/**
	 * 与 query {@code status} 在审批分支上的「是否进入 if(status)」语义对齐：null/空白/"0" 为否，其余为是。
	 */
	private static boolean isApproveStatusQueryActive(String s) {
		if (s == null) {
			return false;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		return true;
	}

	/**
	 * 将 {@code is_closed} query 转为 0/1；非数字时按布尔字符串或「非空即真」规则。
	 */
	private static int closedBitFromIsClosedParam(String raw) {
		if (raw == null) {
			return 0;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return 0;
		}
		try {
			int n = Integer.parseInt(t);
			return n == 0 ? 0 : 1;
		} catch (NumberFormatException e) {
			if (t.equalsIgnoreCase("false")
					|| t.equalsIgnoreCase("no")
					|| t.equalsIgnoreCase("off")) {
				return 0;
			}
			return 1;
		}
	}
}
