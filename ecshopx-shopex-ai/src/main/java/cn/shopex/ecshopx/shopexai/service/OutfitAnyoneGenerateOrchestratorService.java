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

package cn.shopex.ecshopx.shopexai.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.shopexai.api.admin.v1.dto.GenerateOutfitRequest;
import cn.shopex.ecshopx.shopexai.api.admin.v1.dto.OutfitGenerateAcceptedResponse;
import cn.shopex.ecshopx.shopexai.api.admin.v1.dto.OutfitGenerateDirectResponse;
import cn.shopex.ecshopx.shopexai.dispatch.GenerateOutfitJobDispatchPublisher;
import cn.shopex.ecshopx.shopexai.domain.MemberOutfit;
import cn.shopex.ecshopx.shopexai.domain.MemberOutfitLog;
import cn.shopex.ecshopx.shopexai.mapper.MemberOutfitLogMapper;
import cn.shopex.ecshopx.shopexai.mapper.MemberOutfitMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OutfitAnyoneGenerateOrchestratorService {

	private static final int CACHE_TTL_SECONDS = 3600;

	private final MemberOutfitLogMapper memberOutfitLogMapper;
	private final MemberOutfitMapper memberOutfitMapper;
	private final OutfitGenerationResultCacheService outfitGenerationResultCacheService;
	private final GenerateOutfitJobDispatchPublisher generateOutfitJobDispatchPublisher;
	private final OutfitAnyoneGenerationService outfitAnyoneGenerationService;

	public OutfitAnyoneGenerateOrchestratorService(
			MemberOutfitLogMapper memberOutfitLogMapper,
			MemberOutfitMapper memberOutfitMapper,
			OutfitGenerationResultCacheService outfitGenerationResultCacheService,
			GenerateOutfitJobDispatchPublisher generateOutfitJobDispatchPublisher,
			OutfitAnyoneGenerationService outfitAnyoneGenerationService) {
		this.memberOutfitLogMapper = memberOutfitLogMapper;
		this.memberOutfitMapper = memberOutfitMapper;
		this.outfitGenerationResultCacheService = outfitGenerationResultCacheService;
		this.generateOutfitJobDispatchPublisher = generateOutfitJobDispatchPublisher;
		this.outfitAnyoneGenerationService = outfitAnyoneGenerationService;
	}

	public OutfitGenerateAcceptedResponse generateOutfitQueued(
			GenerateOutfitRequest request, long companyId, long operatorId, long distributorId) {
		MemberOutfitLog logRow = createAndLoadOutfitLog(request);
		MemberOutfit model = memberOutfitMapper.selectById(logRow.getModelId());
		if (model == null) {
			throw new BadRequestException("穿搭模型不存在");
		}
		String personImageUrl = model.getModelImage();
		if (personImageUrl == null || personImageUrl.isBlank()) {
			throw new BadRequestException("模特图地址无效");
		}

		String cacheKey = logRow.getRequestId();
		OutfitGenerationPendingCacheValue pending = OutfitGenerationPendingCacheValue.pendingNow();
		outfitGenerationResultCacheService.writePendingStatus(cacheKey, CACHE_TTL_SECONDS, pending);

		String bottom = logRow.getBottomGarmentUrl() == null ? "" : logRow.getBottomGarmentUrl();
		generateOutfitJobDispatchPublisher.enqueueGenerateOutfit(
				personImageUrl,
				logRow.getTopGarmentUrl(),
				bottom,
				cacheKey,
				CACHE_TTL_SECONDS,
				companyId,
				operatorId,
				distributorId);

		return new OutfitGenerateAcceptedResponse(cacheKey);
	}

	@SuppressWarnings("unused")
	public OutfitGenerateDirectResponse generateOutfitDirect(
			GenerateOutfitRequest request, long companyId, long operatorId, long distributorId) {
		MemberOutfitLog logRow = createAndLoadOutfitLog(request);
		MemberOutfit model = memberOutfitMapper.selectById(logRow.getModelId());
		if (model == null) {
			throw new BadRequestException("穿搭模型不存在");
		}
		String personImageUrl = model.getModelImage();
		if (personImageUrl == null || personImageUrl.isBlank()) {
			throw new BadRequestException("模特图地址无效");
		}
		String cacheKey = logRow.getRequestId();
		String bottom = logRow.getBottomGarmentUrl() == null ? "" : logRow.getBottomGarmentUrl();
		try {
			Map<String, Object> result =
					outfitAnyoneGenerationService.generateOutfit(
							personImageUrl, logRow.getTopGarmentUrl(), bottom);
			Map<String, Object> merged = new LinkedHashMap<>(result);
			merged.put("job_completed", true);
			merged.put("completed_at", OutfitGenerationPendingCacheValue.formatNow());
			outfitGenerationResultCacheService.saveResult(cacheKey, merged, CACHE_TTL_SECONDS);
			return new OutfitGenerateDirectResponse(cacheKey, true);
		} catch (RuntimeException e) {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("job_completed", true);
			err.put("error", true);
			err.put("person_image_url", personImageUrl);
			err.put("top_garment_url", logRow.getTopGarmentUrl());
			err.put("bottom_garment_url", bottom);
			outfitGenerationResultCacheService.saveResult(cacheKey, err, CACHE_TTL_SECONDS);
			return new OutfitGenerateDirectResponse(cacheKey, false);
		}
	}

	private MemberOutfitLog createAndLoadOutfitLog(GenerateOutfitRequest request) {
		LocalDateTime now = LocalDateTime.now();
		MemberOutfitLog logRow = new MemberOutfitLog();
		logRow.setRequestId(UUID.randomUUID().toString());
		logRow.setMemberId(request.getMemberId());
		logRow.setItemId(request.getItemId());
		logRow.setModelId(request.getModelId());
		logRow.setTopGarmentUrl(request.getTopGarmentUrl());
		logRow.setBottomGarmentUrl(request.getBottomGarmentUrl());
		logRow.setCreatedAt(now);
		logRow.setUpdatedAt(now);
		memberOutfitLogMapper.insert(logRow);
		MemberOutfitLog loaded = memberOutfitLogMapper.selectById(logRow.getId());
		if (loaded == null) {
			throw new BadRequestException("穿搭日志写入失败");
		}
		return loaded;
	}
}
