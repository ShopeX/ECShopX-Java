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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.theme.NewDistributorPagesTemplatePort;
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.domain.PagesTemplateSet;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateSetMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NewDistributorPagesTemplatePortImpl implements NewDistributorPagesTemplatePort {

	private static final String WEAPP_PAGES_HQ_INDEX = "index";

	private static final String WEAPP_PAGES_STORE_COPY = "distributor_index";

	private final PagesTemplateMapper pagesTemplateMapper;

	private final PagesTemplateSetMapper pagesTemplateSetMapper;

	private final PagesTemplateStoreSyncApplyService pagesTemplateStoreSyncApplyService;

	public NewDistributorPagesTemplatePortImpl(
			PagesTemplateMapper pagesTemplateMapper,
			PagesTemplateSetMapper pagesTemplateSetMapper,
			PagesTemplateStoreSyncApplyService pagesTemplateStoreSyncApplyService) {
		this.pagesTemplateMapper = pagesTemplateMapper;
		this.pagesTemplateSetMapper = pagesTemplateSetMapper;
		this.pagesTemplateStoreSyncApplyService = pagesTemplateStoreSyncApplyService;
	}

	@Override
	public void bindDefaultTemplates(long companyId, long distributorId, Map<String, Object> distributorRowSnapshot) {
		if (companyId <= 0L || distributorId <= 0L) {
			return;
		}
		Map<String, Object> snap =
				distributorRowSnapshot != null ? distributorRowSnapshot : Collections.emptyMap();
		long regionauthFromDistributor = extractLong(snap.get("regionauth_id"));

		PagesTemplate info =
				pagesTemplateMapper.selectOne(
						new LambdaQueryWrapper<PagesTemplate>()
								.eq(PagesTemplate::getCompanyId, companyId)
								.eq(PagesTemplate::getTemplateType, 0)
								.eq(PagesTemplate::getDistributorId, 0)
								.eq(PagesTemplate::getRegionauthId, regionauthFromDistributor)
								.eq(PagesTemplate::getStatus, 1)
								.eq(PagesTemplate::getWeappPages, WEAPP_PAGES_HQ_INDEX)
								.isNull(PagesTemplate::getDeletedAt)
								.orderByDesc(PagesTemplate::getPagesTemplateId)
								.last("LIMIT 1"));

		if (info == null) {
			throw new ResourceException("无效的模板");
		}
		if (info.getPagesTemplateId() == null || info.getPagesTemplateId() <= 0L) {
			throw new ResourceException("无效的模板");
		}

		long regionauthIdKey = info.getRegionauthId() == null ? 0L : info.getRegionauthId();

		int isEnforceSync = 2;
		PagesTemplateSet setRow =
				pagesTemplateSetMapper.selectOne(
						new LambdaQueryWrapper<PagesTemplateSet>()
								.eq(PagesTemplateSet::getCompanyId, companyId)
								.eq(PagesTemplateSet::getRegionauthId, regionauthIdKey)
								.last("LIMIT 1"));
		if (setRow != null && setRow.getIsEnforceSync() != null) {
			isEnforceSync = setRow.getIsEnforceSync();
		}

		int enforce = isEnforceSync;
		long cnt =
				pagesTemplateMapper.selectCount(
						new LambdaQueryWrapper<PagesTemplate>()
								.eq(PagesTemplate::getCompanyId, companyId)
								.eq(
										PagesTemplate::getDistributorId,
										(int) Math.min(Integer.MAX_VALUE, distributorId))
								.isNull(PagesTemplate::getDeletedAt));
		if (cnt == 0L) {
			enforce = 1;
		}

		String localeTag = resolveLocaleTag(snap);

		pagesTemplateStoreSyncApplyService.applyOneDistributor(
				companyId,
				distributorId,
				enforce,
				info.getTemplateTitle(),
				info.getTemplatePic(),
				WEAPP_PAGES_STORE_COPY,
				info.getElementEditStatus(),
				info.getPagesTemplateId(),
				info.getTemplateName(),
				localeTag);
	}

	private static String resolveLocaleTag(Map<String, Object> row) {
		if (row == null) {
			return "";
		}
		Object v = row.get("locale_tag");
		if (v == null) {
			v = row.get("lang");
		}
		return v == null ? "" : String.valueOf(v);
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		return Long.parseLong(s);
	}
}
