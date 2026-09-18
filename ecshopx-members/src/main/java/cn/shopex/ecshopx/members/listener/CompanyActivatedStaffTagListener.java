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

package cn.shopex.ecshopx.members.listener;

import cn.shopex.ecshopx.companys.service.activation.CompanyActivatedEvent;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CompanyActivatedStaffTagListener {

	private final MemberTagsMapper memberTagsMapper;

	public CompanyActivatedStaffTagListener(MemberTagsMapper memberTagsMapper) {
		this.memberTagsMapper = memberTagsMapper;
	}

	/**
	 * 事件在持久层事务提交之后、编排线程上常无活动事务时发布；
	 * {@code fallbackExecution = true} 避免监听器被静默跳过。
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void onCompanyActivated(CompanyActivatedEvent event) {
		long companyId = event.getCompanyId();
		LambdaQueryWrapper<MemberTags> q = new LambdaQueryWrapper<>();
		q.eq(MemberTags::getCompanyId, companyId).eq(MemberTags::getSource, "staff");
		Long count = memberTagsMapper.selectCount(q);
		if (count != null && count > 0L) {
			return;
		}
		long nowSec = System.currentTimeMillis() / 1000L;
		MemberTags row = new MemberTags();
		row.setCompanyId(companyId);
		row.setDistributorId(0L);
		row.setTagName("员工");
		row.setTagColor("#ff1939");
		row.setFontColor("#ffffff");
		row.setDescription("用于标识会员是否是员工身份");
		row.setCreated(nowSec);
		row.setUpdated(nowSec);
		row.setSource("staff");
		memberTagsMapper.insert(row);
	}
}
