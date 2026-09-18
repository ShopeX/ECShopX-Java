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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PagesTemplateServices {

	private final PagesTemplateMapper pagesTemplateMapper;
	private final TransactionTemplate transactionTemplate;

	public PagesTemplateServices(
			PagesTemplateMapper pagesTemplateMapper, PlatformTransactionManager platformTransactionManager) {
		this.pagesTemplateMapper = pagesTemplateMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public void scheduleEnableTemplate() {
		LambdaQueryWrapper<PagesTemplate> base =
				new LambdaQueryWrapper<PagesTemplate>()
						.eq(PagesTemplate::getTimerStatus, 1)
						.isNull(PagesTemplate::getDeletedAt);

		long count = pagesTemplateMapper.selectCount(base);
		if (count < 1L) {
			return;
		}

		Page<PagesTemplate> page = new Page<>(1, 1000, false);
		base.orderByDesc(PagesTemplate::getCreatedAt);
		IPage<PagesTemplate> pageResult = pagesTemplateMapper.selectPage(page, base);
		List<PagesTemplate> records = pageResult.getRecords();
		if (records == null || records.isEmpty()) {
			return;
		}

		long nowEpoch = Instant.now().getEpochSecond();
		for (PagesTemplate val : records) {
			if (val.getTimerTime() != null && val.getTimerTime() > (int) nowEpoch) {
				continue;
			}
			transactionTemplate.executeWithoutResult(
					status -> applyDueRow(val, (int) nowEpoch));
		}
	}

	private void applyDueRow(PagesTemplate val, int nowEpochSeconds) {
		Long companyId = val.getCompanyId();
		Long regionauthId = val.getRegionauthId() != null ? val.getRegionauthId() : 0L;
		int distributorId = val.getDistributorId() != null ? val.getDistributorId() : 0;
		String weappPages = val.getWeappPages() != null ? val.getWeappPages() : "index";

		LambdaUpdateWrapper<PagesTemplate> bulk =
				new LambdaUpdateWrapper<PagesTemplate>()
						.set(PagesTemplate::getStatus, 2)
						.eq(PagesTemplate::getCompanyId, companyId)
						.eq(PagesTemplate::getRegionauthId, regionauthId)
						.eq(PagesTemplate::getDistributorId, distributorId)
						.eq(PagesTemplate::getWeappPages, weappPages)
						.eq(PagesTemplate::getStatus, 1)
						.isNull(PagesTemplate::getDeletedAt);
		pagesTemplateMapper.update(null, bulk);

		LambdaUpdateWrapper<PagesTemplate> single =
				new LambdaUpdateWrapper<PagesTemplate>()
						.set(PagesTemplate::getStatus, 1)
						.set(PagesTemplate::getTimerStatus, 2)
						.set(PagesTemplate::getTemplateStatusModifyTime, nowEpochSeconds)
						.eq(PagesTemplate::getCompanyId, companyId)
						.eq(PagesTemplate::getPagesTemplateId, val.getPagesTemplateId())
						.isNull(PagesTemplate::getDeletedAt);
		pagesTemplateMapper.update(null, single);
	}
}
