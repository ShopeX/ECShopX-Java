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

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.members.service.systemtag.MemberSystemTagSeedService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(200)
public class MemberSystemTagHistoricalSeedRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MemberSystemTagHistoricalSeedRunner.class);

	private final CompanysMapper companysMapper;
	private final MemberSystemTagSeedService memberSystemTagSeedService;

	public MemberSystemTagHistoricalSeedRunner(
			CompanysMapper companysMapper, MemberSystemTagSeedService memberSystemTagSeedService) {
		this.companysMapper = companysMapper;
		this.memberSystemTagSeedService = memberSystemTagSeedService;
	}

	@Override
	public void run(ApplicationArguments args) {
		List<Companys> companies = companysMapper.selectList(null);
		int seeded = 0;
		for (Companys company : companies) {
			if (company == null || company.getCompanyId() == null || company.getCompanyId() <= 0L) {
				continue;
			}
			memberSystemTagSeedService.seedCompany(company.getCompanyId());
			seeded++;
		}
		log.info("[system-tag] historical seed scanned companies={}", seeded);
	}
}
