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

package cn.shopex.ecshopx.systemlink.service.omsqueuelog;

import cn.shopex.ecshopx.common.systemlink.OmsQueueLogWritePort;
import cn.shopex.ecshopx.systemlink.domain.OmsQueueLog;
import cn.shopex.ecshopx.systemlink.mapper.OmsQueueLogMapper;
import org.springframework.stereotype.Service;

@Service
public class OmsQueueLogWriteAdapter implements OmsQueueLogWritePort {

	private final OmsQueueLogMapper omsQueueLogMapper;

	public OmsQueueLogWriteAdapter(OmsQueueLogMapper omsQueueLogMapper) {
		this.omsQueueLogMapper = omsQueueLogMapper;
	}

	@Override
	public long insertResponseFail(long companyId, String worker, String paramsJson) {
		OmsQueueLog log = new OmsQueueLog();
		log.setCompanyId(companyId);
		log.setApiType("response");
		log.setWorker(worker);
		log.setParams(paramsJson);
		log.setStatus("fail");
		log.setRuntime("0");
		log.setResult("[]");
		log.setCreated((int) (System.currentTimeMillis() / 1000));
		omsQueueLogMapper.insert(log);
		return log.getId();
	}

	@Override
	public long insertRequest(
			long companyId,
			String worker,
			String paramsJson,
			String resultJson,
			String status,
			String runtime,
			String msgId) {
		int now = (int) (System.currentTimeMillis() / 1000);
		OmsQueueLog log = new OmsQueueLog();
		log.setCompanyId(companyId);
		log.setApiType("request");
		log.setWorker(worker);
		log.setParams(paramsJson);
		log.setResult(resultJson);
		log.setStatus(status == null || status.isBlank() ? "fail" : status);
		log.setRuntime(runtime == null ? "0" : runtime);
		log.setMsgId(msgId);
		log.setCreated(now);
		log.setUpdated(now);
		omsQueueLogMapper.insert(log);
		return log.getId() == null ? 0L : log.getId();
	}

	@Override
	public void updateResult(long id, String status, String resultJson) {
		OmsQueueLog log = new OmsQueueLog();
		log.setId(id);
		log.setStatus(status);
		log.setResult(resultJson);
		omsQueueLogMapper.updateById(log);
	}
}
