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
import cn.shopex.ecshopx.members.service.systemtag.MemberSystemTagSeedService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CompanyActivatedSystemTagListener {

	private final MemberSystemTagSeedService memberSystemTagSeedService;

	public CompanyActivatedSystemTagListener(MemberSystemTagSeedService memberSystemTagSeedService) {
		this.memberSystemTagSeedService = memberSystemTagSeedService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void onCompanyActivated(CompanyActivatedEvent event) {
		memberSystemTagSeedService.seedCompany(event.getCompanyId());
	}
}
