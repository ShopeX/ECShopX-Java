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

package cn.shopex.ecshopx.workwechat.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatVerifyDomainFile;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatVerifyDomainFileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkWechatVerifyDomainFileService {

	private final WorkWechatVerifyDomainFileMapper workWechatVerifyDomainFileMapper;

	public WorkWechatVerifyDomainFileService(WorkWechatVerifyDomainFileMapper workWechatVerifyDomainFileMapper) {
		this.workWechatVerifyDomainFileMapper = workWechatVerifyDomainFileMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void saveVerifyInfo(long companyId, long operatorId, String name, String contents) {
		int created = (int) Instant.now().getEpochSecond();
		LambdaQueryWrapper<WorkWechatVerifyDomainFile> w = new LambdaQueryWrapper<>();
		w.eq(WorkWechatVerifyDomainFile::getName, name);
		WorkWechatVerifyDomainFile existing = workWechatVerifyDomainFileMapper.selectOne(w);
		if (existing != null) {
			existing.setCompanyId(companyId);
			existing.setOperatorId(operatorId);
			existing.setName(name);
			existing.setContents(contents);
			existing.setCreated(created);
			existing.setUpdated(created);
			int rows = workWechatVerifyDomainFileMapper.updateById(existing);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		} else {
			WorkWechatVerifyDomainFile row = new WorkWechatVerifyDomainFile();
			row.setCompanyId(companyId);
			row.setOperatorId(operatorId);
			row.setName(name);
			row.setContents(contents);
			row.setCreated(created);
			row.setUpdated(created);
			workWechatVerifyDomainFileMapper.insert(row);
		}
	}
}
