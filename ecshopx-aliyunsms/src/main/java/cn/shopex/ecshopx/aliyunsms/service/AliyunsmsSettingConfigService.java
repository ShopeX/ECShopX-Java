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

import cn.shopex.ecshopx.aliyunsms.domain.AccessKey;
import cn.shopex.ecshopx.aliyunsms.domain.Scene;
import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.mapper.AccessKeyMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsSettingConfigService {

	private final AccessKeyMapper accessKeyMapper;

	private final SceneMapper sceneMapper;

	private final SignMapper signMapper;

	private final TemplateMapper templateMapper;

	private final AliyunsmsSettingStatusService aliyunsmsSettingStatusService;

	public AliyunsmsSettingConfigService(
			AccessKeyMapper accessKeyMapper,
			SceneMapper sceneMapper,
			SignMapper signMapper,
			TemplateMapper templateMapper,
			AliyunsmsSettingStatusService aliyunsmsSettingStatusService) {
		this.accessKeyMapper = accessKeyMapper;
		this.sceneMapper = sceneMapper;
		this.signMapper = signMapper;
		this.templateMapper = templateMapper;
		this.aliyunsmsSettingStatusService = aliyunsmsSettingStatusService;
	}

	public Map<String, Object> getConfigAggregate(long companyId) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		AccessKey row =
				accessKeyMapper.selectOne(new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getCompanyId, companyId));
		if (row == null) {
			data.put("accesskey_id", "");
			data.put("accesskey_secret", "");
		} else {
			data.put("id", row.getId());
			data.put("company_id", row.getCompanyId());
			data.put("accesskey_id", row.getAccesskeyId() != null ? row.getAccesskeyId() : "");
			data.put("accesskey_secret", row.getAccesskeySecret() != null ? row.getAccesskeySecret() : "");
			data.put("created", row.getCreated());
			data.put("updated", row.getUpdated());
		}
		Long sceneNum = sceneMapper.selectCount(Wrappers.<Scene>lambdaQuery().eq(Scene::getCompanyId, companyId));
		data.put("scene_num", sceneNum == null ? 0L : sceneNum.longValue());
		Long signNum = signMapper.selectCount(
				Wrappers.<Sign>lambdaQuery().eq(Sign::getCompanyId, companyId).eq(Sign::getStatus, "1"));
		data.put("sign_num", signNum == null ? 0L : signNum.longValue());
		Long templateNum = templateMapper.selectCount(
				Wrappers.<Template>lambdaQuery()
						.eq(Template::getCompanyId, companyId)
						.eq(Template::getStatus, "1"));
		data.put("template_num", templateNum == null ? 0L : templateNum.longValue());
		data.put("status", aliyunsmsSettingStatusService.getStatus(companyId));
		return data;
	}

	public void setConfig(long companyId, String accesskeyId, String accesskeySecret) {
		int now = (int) Instant.now().getEpochSecond();
		LambdaQueryWrapper<AccessKey> w = new LambdaQueryWrapper<AccessKey>().eq(AccessKey::getCompanyId, companyId);
		AccessKey existing = accessKeyMapper.selectOne(w);
		if (existing == null) {
			AccessKey row = new AccessKey();
			row.setCompanyId(companyId);
			row.setAccesskeyId(accesskeyId);
			row.setAccesskeySecret(accesskeySecret);
			row.setCreated(now);
			row.setUpdated(now);
			accessKeyMapper.insert(row);
			return;
		}
		existing.setAccesskeyId(accesskeyId);
		existing.setAccesskeySecret(accesskeySecret);
		existing.setUpdated(now);
		int n = accessKeyMapper.updateById(existing);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}
}
