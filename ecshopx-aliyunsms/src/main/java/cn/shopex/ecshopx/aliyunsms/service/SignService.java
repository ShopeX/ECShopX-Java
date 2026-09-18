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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsSignClient;
import cn.shopex.ecshopx.common.aliyunsms.GetSmsSignResult;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsQuerySmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SignService {

	private final SignMapper signMapper;
	private final AliyunsmsGetSmsSignClient aliyunsmsGetSmsSignClient;
	private final AliyunsmsQuerySmsSignJobDispatchPublisher querySmsSignJobDispatchPublisher;

	public SignService(
			SignMapper signMapper,
			AliyunsmsGetSmsSignClient aliyunsmsGetSmsSignClient,
			AliyunsmsQuerySmsSignJobDispatchPublisher querySmsSignJobDispatchPublisher) {
		this.signMapper = signMapper;
		this.aliyunsmsGetSmsSignClient = aliyunsmsGetSmsSignClient;
		this.querySmsSignJobDispatchPublisher = querySmsSignJobDispatchPublisher;
	}

	/**
	 * 查询审核中签名列表，对每条有效签名入队异步查询任务。返回成功入队的条数（跳过 {@code sign_name}
	 * 为空的行）。
	 */
	public int scheduleQueryAuditStatus() {
		List<Sign> pending =
				signMapper.selectList(
						new QueryWrapper<Sign>()
								.select("company_id", "sign_name")
								.eq("status", "0"));
		if (pending == null || pending.isEmpty()) {
			return 0;
		}
		int dispatched = 0;
		for (Sign item : pending) {
			long companyId = item.getCompanyId() != null ? item.getCompanyId() : 0L;
			String name = item.getSignName();
			if (name == null) {
				continue;
			}
			querySmsSignJobDispatchPublisher.publish(companyId, name);
			dispatched++;
		}
		return dispatched;
	}

	/**
	 * 拉取云端审核状态并在条件满足时写回本地 {@code aliyunsms_sign}（审核中状态过滤更新）。
	 */
	public void applyPendingSignAuditFromCloud(long companyId, String signName) {
		GetSmsSignResult cloud = aliyunsmsGetSmsSignClient.getSmsSign(companyId, signName);
		if (cloud.signStatus() == null) {
			return;
		}
		Sign current =
				signMapper.selectOne(
						new QueryWrapper<Sign>()
								.eq("company_id", companyId)
								.eq("sign_name", signName)
								.eq("status", "0"));
		if (current == null) {
			throw new ResourceException("未查询到更新数据");
		}
		String reason = cloud.rejectInfo() != null ? cloud.rejectInfo() : "";
		int now = (int) Instant.now().getEpochSecond();
		int rows =
				signMapper.update(
						null,
						new UpdateWrapper<Sign>()
								.eq("company_id", companyId)
								.eq("sign_name", signName)
								.eq("status", "0")
								.set("status", cloud.signStatus())
								.set("reason", reason)
								.set("updated", now));
		if (rows != 1) {
			throw new ResourceException("未查询到更新数据");
		}
	}
}
