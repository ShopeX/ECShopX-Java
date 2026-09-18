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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.OpenScreenAdSaveRequest;
import cn.shopex.ecshopx.theme.domain.OpenScreenAd;
import cn.shopex.ecshopx.theme.mapper.OpenScreenAdMapper;
import cn.shopex.ecshopx.theme.support.OpenScreenAdColumnNamesDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OpenScreenAdSaveService {

	private final OpenScreenAdMapper openScreenAdMapper;
	private final ObjectMapper objectMapper;
	private final OpenScreenAdColumnNamesDataMapper openScreenAdColumnNamesDataMapper;

	public Object getInfo(long companyId) {
		var w = new LambdaQueryWrapper<OpenScreenAd>().eq(OpenScreenAd::getCompanyId, companyId);
		OpenScreenAd row = openScreenAdMapper.selectOne(w);
		if (row == null) {
			return Collections.emptyList();
		}
		Map<String, Object> data = new LinkedHashMap<>(openScreenAdColumnNamesDataMapper.toColumnNamesData(row));
		String raw = row.getAdUrl();
		if (StringUtils.hasText(raw)) {
			try {
				data.put("ad_url", objectMapper.readValue(raw, Object.class));
			} catch (JsonProcessingException e) {
				data.put("ad_url", null);
			}
		}
		return data;
	}

	public Map<String, Object> save(long companyId, OpenScreenAdSaveRequest req) {
		validateSaveRequest(req);

		long startTs = Long.parseLong(trim(req.getStartTime()));
		long endTs = Long.parseLong(trim(req.getEndTime()));

		String adUrlJson;
		try {
			adUrlJson = objectMapper.writeValueAsString(req.getAdUrl());
		} catch (JsonProcessingException e) {
			throw new ResourceException("操作失败");
		}

		String isEnableRaw = trim(req.getIsEnable());
		if (isEnableRaw == null || !isEnableRaw.matches("^[0-9]+$")) {
			throw new BadRequestException("请选择是否启用");
		}
		int isEnableVal = Integer.parseInt(isEnableRaw);

		String waitingRaw = trim(req.getWaitingTime());
		int waitingVal = Integer.parseInt(waitingRaw);

		Integer materialTypeVal = parseOptionalIntOrNull(req.getMaterialType());
		Integer isJumpVal = parseOptionalIntOrNull(req.getIsJump());

		String showTimeVal = trim(req.getShowTime());

		var w = new LambdaQueryWrapper<OpenScreenAd>().eq(OpenScreenAd::getCompanyId, companyId);
		OpenScreenAd existing = openScreenAdMapper.selectOne(w);

		int now = (int) (System.currentTimeMillis() / 1000L);

		if (existing == null) {
			OpenScreenAd entity = new OpenScreenAd();
			entity.setCompanyId(companyId);
			entity.setAdMaterial(trim(req.getAdMaterial()));
			entity.setIsEnable(isEnableVal);
			entity.setShowTime(showTimeVal);
			entity.setPosition(req.getPosition());
			entity.setIsJump(isJumpVal);
			entity.setMaterialType(materialTypeVal);
			entity.setWaitingTime(waitingVal);
			entity.setAdUrl(adUrlJson);
			entity.setApp(req.getApp());
			entity.setStartTime(startTs);
			entity.setEndTime(endTs);
			entity.setCreated(now);
			entity.setUpdated(now);
			int rows = openScreenAdMapper.insert(entity);
			if (rows != 1 || entity.getId() == null || entity.getId() <= 0L) {
				throw new ResourceException("操作失败");
			}
			return Map.of("status", Boolean.TRUE);
		}

		existing.setAdMaterial(trim(req.getAdMaterial()));
		existing.setIsEnable(isEnableVal);
		existing.setShowTime(showTimeVal);
		existing.setPosition(req.getPosition());
		existing.setIsJump(isJumpVal);
		existing.setMaterialType(materialTypeVal);
		existing.setWaitingTime(waitingVal);
		existing.setAdUrl(adUrlJson);
		existing.setApp(req.getApp());
		existing.setStartTime(startTs);
		existing.setEndTime(endTs);
		existing.setUpdated(now);
		int rows = openScreenAdMapper.updateById(existing);
		if (rows <= 0) {
			throw new ResourceException("操作失败");
		}
		return Map.of("status", Boolean.TRUE);
	}

	private void validateSaveRequest(OpenScreenAdSaveRequest req) {
		if (!StringUtils.hasText(trim(req.getAdMaterial()))) {
			throw new BadRequestException("请上传广告素材");
		}
		if (!StringUtils.hasText(trim(req.getIsEnable()))) {
			throw new BadRequestException("请选择是否启用");
		}
		String showTime = trim(req.getShowTime());
		if (!StringUtils.hasText(showTime)) {
			throw new BadRequestException("请选择曝光时间");
		}
		if (!"always".equals(showTime) && !"first".equals(showTime)) {
			throw new BadRequestException("请选择曝光时间");
		}
		String waiting = trim(req.getWaitingTime());
		if (!StringUtils.hasText(waiting)) {
			throw new BadRequestException("请设置秒数");
		}
		if (!waiting.matches("^[0-9]+$")) {
			throw new BadRequestException("请设置秒数");
		}
		String start = trim(req.getStartTime());
		if (!StringUtils.hasText(start)) {
			throw new BadRequestException("请选择展示开始时间");
		}
		try {
			Long.parseLong(start);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("请选择展示开始时间");
		}
		String end = trim(req.getEndTime());
		if (!StringUtils.hasText(end)) {
			throw new BadRequestException("请选择展示结束时间");
		}
		try {
			Long.parseLong(end);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("请选择展示结束时间");
		}
	}

	private static Integer parseOptionalIntOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("操作失败");
		}
	}

	private static String trim(String s) {
		return s == null ? null : s.trim();
	}
}
