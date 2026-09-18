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

package cn.shopex.ecshopx.form.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.form.domain.TranscriptProperties;
import cn.shopex.ecshopx.form.domain.Transcripts;
import cn.shopex.ecshopx.form.mapper.TranscriptPropertiesMapper;
import cn.shopex.ecshopx.form.mapper.TranscriptsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TranscriptUpdateService {

	private final TranscriptsMapper transcriptsMapper;
	private final TranscriptPropertiesMapper transcriptPropertiesMapper;

	public TranscriptUpdateService(
			TranscriptsMapper transcriptsMapper,
			TranscriptPropertiesMapper transcriptPropertiesMapper) {
		this.transcriptsMapper = transcriptsMapper;
		this.transcriptPropertiesMapper = transcriptPropertiesMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> update(long companyId, long transcriptId, Map<String, Object> params) {
		Transcripts entity = transcriptsMapper.selectById(transcriptId);
		if (entity == null) {
			throw new ResourceException("transcriptId为" + transcriptId + "的成绩单不存在");
		}

		if (params.containsKey("transcript_name") && params.get("transcript_name") != null) {
			entity.setTranscriptName(String.valueOf(params.get("transcript_name")));
		}
		if (params.containsKey("template_name") && params.get("template_name") != null) {
			entity.setTemplateName(String.valueOf(params.get("template_name")));
		}
		if (params.containsKey("company_id") && params.get("company_id") != null) {
			entity.setCompanyId(toLongCompany(params.get("company_id")));
		}
		applyTranscriptStatusForUpdate(entity, params);

		entity.setUpdated((int) Instant.now().getEpochSecond());
		transcriptsMapper.updateById(entity);

		Map<String, Object> transcriptResult = new LinkedHashMap<>();
		transcriptResult.put("transcript_id", entity.getTranscriptId());
		transcriptResult.put("transcript_name", entity.getTranscriptName());
		transcriptResult.put("company_id", entity.getCompanyId());
		transcriptResult.put("template_name", entity.getTemplateName());
		transcriptResult.put("transcript_status", entity.getTranscriptStatus());
		transcriptResult.put("created", entity.getCreated());
		transcriptResult.put("updated", entity.getUpdated());

		LambdaQueryWrapper<TranscriptProperties> byTranscript = new LambdaQueryWrapper<>();
		byTranscript
				.eq(TranscriptProperties::getCompanyId, companyId)
				.eq(TranscriptProperties::getTranscriptId, transcriptId);
		List<TranscriptProperties> oldProps = transcriptPropertiesMapper.selectList(byTranscript);

		List<Map<String, Object>> transcriptProps = new ArrayList<>();
		Object rawEvaluate = params.get("evaluateItems");

		if (oldProps.isEmpty() && params.containsKey("evaluateItems") && isTruthyEvaluateItems(rawEvaluate)) {
			if (!(rawEvaluate instanceof List<?> evalList)) {
				throw new BadRequestException("evaluateItems 须为数组");
			}
			for (Object el : evalList) {
				if (!(el instanceof Map<?, ?> row)) {
					continue;
				}
				if (!row.containsKey("prop_name") || row.get("prop_name") == null) {
					continue;
				}
				String propName = String.valueOf(row.get("prop_name"));
				assertNoDuplicatePropName(companyId, transcriptId, propName, null);
				TranscriptProperties prop = insertProp(companyId, transcriptId, row, propName);
				transcriptProps.add(toPropRowMap(prop));
			}
		} else {
			if (!params.containsKey("evaluateItems") || params.get("evaluateItems") == null) {
				throw new BadRequestException("缺少 evaluateItems，无法更新考评项");
			}
			Object evalObj = params.get("evaluateItems");
			if (!(evalObj instanceof List<?> evalList)) {
				throw new BadRequestException("evaluateItems 须为数组");
			}

			@SuppressWarnings("unchecked")
			List<Object> evalAsObjects = (List<Object>) (List<?>) evalObj;
			Set<Long> newPropIds = collectTruthyPropIds(evalAsObjects);
			Set<Long> oldPropIds =
					oldProps.stream().map(TranscriptProperties::getPropId).collect(Collectors.toCollection(LinkedHashSet::new));
			Set<Long> delIds = new LinkedHashSet<>(oldPropIds);
			delIds.removeAll(newPropIds);

			for (Long delId : delIds) {
				TranscriptProperties row = transcriptPropertiesMapper.selectById(delId);
				if (row == null) {
					throw new ResourceException("propid为" + delId + "的属性不存在");
				}
				transcriptPropertiesMapper.deleteById(delId);
			}

			if (isTruthyEvaluateItems(evalObj)) {
				for (Object el : evalList) {
					if (!(el instanceof Map<?, ?> v)) {
						continue;
					}
					Object pidObj = v.get("prop_id");
					if (isTruthyPropId(pidObj)) {
						long propId = parsePropIdStrict(pidObj);
						TranscriptProperties existing = transcriptPropertiesMapper.selectById(propId);
						if (existing == null) {
							throw new ResourceException("propid为" + propId + "的属性不存在");
						}
						if (v.containsKey("prop_name") && v.get("prop_name") != null) {
							existing.setPropName(String.valueOf(v.get("prop_name")));
						}
						if (v.containsKey("prop_unit")) {
							existing.setPropUnit(
									v.get("prop_unit") == null ? "" : String.valueOf(v.get("prop_unit")));
						}
						existing.setTranscriptId(transcriptId);
						existing.setCompanyId(companyId);
						assertNoDuplicatePropName(companyId, transcriptId, existing.getPropName(), propId);
						transcriptPropertiesMapper.updateById(existing);
						transcriptProps.add(toPropRowMap(existing));
					} else {
						if (!v.containsKey("prop_name") || v.get("prop_name") == null) {
							continue;
						}
						String propName = String.valueOf(v.get("prop_name"));
						assertNoDuplicatePropName(companyId, transcriptId, propName, null);
						TranscriptProperties prop = insertProp(companyId, transcriptId, v, propName);
						transcriptProps.add(toPropRowMap(prop));
					}
				}
			}
		}

		transcriptResult.put("evaluate_items", transcriptProps);
		return transcriptResult;
	}

	private static long toLongCompany(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static void applyTranscriptStatusForUpdate(Transcripts entity, Map<String, Object> params) {
		boolean isset = params.containsKey("transcript_status") && params.get("transcript_status") != null;
		if (!isset) {
			entity.setTranscriptStatus("off");
			return;
		}
		Object v = params.get("transcript_status");
		if (v instanceof Boolean b) {
			entity.setTranscriptStatus(b ? "on" : "");
			return;
		}
		if (v instanceof Number n) {
			entity.setTranscriptStatus(n.intValue() != 0 ? "on" : "");
			return;
		}
		entity.setTranscriptStatus(String.valueOf(v));
	}

	private static boolean isTruthyEvaluateItems(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Map<?, ?>) {
			return false;
		}
		if (v instanceof List<?> list) {
			return !list.isEmpty();
		}
		return false;
	}

	private static boolean isTruthyPropId(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0;
		}
		String s = String.valueOf(v);
		if (!StringUtils.hasText(s)) {
			return false;
		}
		return !"0".equals(s.trim());
	}

	private static long parsePropIdStrict(Object v) {
		try {
			if (v instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("prop_id 格式无效");
		}
	}

	private Set<Long> collectTruthyPropIds(List<Object> evalList) {
		Set<Long> ids = new LinkedHashSet<>();
		for (Object el : evalList) {
			if (!(el instanceof Map<?, ?> row)) {
				continue;
			}
			if (!row.containsKey("prop_id")) {
				continue;
			}
			Object pid = row.get("prop_id");
			if (!isTruthyPropId(pid)) {
				continue;
			}
			ids.add(parsePropIdStrict(pid));
		}
		return ids;
	}

	private void assertNoDuplicatePropName(long companyId, long transcriptId, String propName, Long excludePropId) {
		LambdaQueryWrapper<TranscriptProperties> dup = new LambdaQueryWrapper<>();
		dup.eq(TranscriptProperties::getCompanyId, companyId)
				.eq(TranscriptProperties::getTranscriptId, transcriptId)
				.eq(TranscriptProperties::getPropName, propName);
		if (excludePropId != null) {
			dup.ne(TranscriptProperties::getPropId, excludePropId);
		}
		if (transcriptPropertiesMapper.selectCount(dup) > 0) {
			throw new ResourceException("考评项目名称不能重复！");
		}
	}

	private TranscriptProperties insertProp(long companyId, long transcriptId, Map<?, ?> row, String propName) {
		TranscriptProperties prop = new TranscriptProperties();
		prop.setTranscriptId(transcriptId);
		prop.setCompanyId(companyId);
		prop.setPropName(propName);
		String propUnit = "";
		if (row.containsKey("prop_unit") && row.get("prop_unit") != null) {
			propUnit = String.valueOf(row.get("prop_unit"));
		}
		prop.setPropUnit(propUnit);
		transcriptPropertiesMapper.insert(prop);
		return prop;
	}

	private static Map<String, Object> toPropRowMap(TranscriptProperties prop) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("prop_id", prop.getPropId());
		item.put("transcript_id", prop.getTranscriptId());
		item.put("company_id", prop.getCompanyId());
		item.put("prop_name", prop.getPropName());
		item.put("prop_unit", prop.getPropUnit() != null ? prop.getPropUnit() : "");
		return item;
	}
}
