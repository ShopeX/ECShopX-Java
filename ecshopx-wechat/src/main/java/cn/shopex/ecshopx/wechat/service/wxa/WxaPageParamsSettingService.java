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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.service.MerchantBaseSettingSaveService;
import cn.shopex.ecshopx.wechat.domain.WeappSetting;
import cn.shopex.ecshopx.wechat.port.WxaWeappTemplateParamsEnrichPort;
import cn.shopex.ecshopx.wechat.repository.WeappSettingRepository;
import cn.shopex.ecshopx.wechat.service.wxa.model.SavePageAllParamsRow;
import cn.shopex.ecshopx.wechat.support.WeappSettingLegacySerializeCodec;
import cn.shopex.ecshopx.wechat.support.WeappSettingOutsideLangParamsLoader;
import cn.shopex.ecshopx.wechat.support.WeappSettingOutsideLangParamsWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WxaPageParamsSettingService {

	private final SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService;
	private final WeappSettingRepository weappSettingRepository;
	private final MerchantBaseSettingSaveService merchantBaseSettingSaveService;
	private final WeappSettingOutsideLangParamsLoader weappSettingOutsideLangParamsLoader;
	private final WxaWeappTemplateParamsEnrichPort wxaWeappTemplateParamsEnrichPort;

	private final WeappSettingOutsideLangParamsWriter weappSettingOutsideLangParamsWriter;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	public WxaPageParamsSettingService(
			SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService,
			WeappSettingRepository weappSettingRepository,
			MerchantBaseSettingSaveService merchantBaseSettingSaveService,
			WeappSettingOutsideLangParamsLoader weappSettingOutsideLangParamsLoader,
			WxaWeappTemplateParamsEnrichPort wxaWeappTemplateParamsEnrichPort,
			WeappSettingOutsideLangParamsWriter weappSettingOutsideLangParamsWriter) {
		this.superadminWxappTemplateMetadataService = superadminWxappTemplateMetadataService;
		this.weappSettingRepository = weappSettingRepository;
		this.merchantBaseSettingSaveService = merchantBaseSettingSaveService;
		this.weappSettingOutsideLangParamsLoader = weappSettingOutsideLangParamsLoader;
		this.wxaWeappTemplateParamsEnrichPort = wxaWeappTemplateParamsEnrichPort;
		this.weappSettingOutsideLangParamsWriter = weappSettingOutsideLangParamsWriter;
	}

	public Object getParamByTempName(
			long companyId,
			String templateName,
			String nameQuery,
			String pageNameQuery,
			String distributorIdRaw,
			String versionQuery,
			String localeTagForI18n) {
		superadminWxappTemplateMetadataService.assertValidWxappTemplateForPageParams(templateName);
		String pageName =
				pageNameQuery == null || pageNameQuery.isBlank() ? "index" : pageNameQuery.trim();
		String version = resolveVersion(distributorIdRaw, versionQuery);
		String configNameFilter = isFalsyNameForAggregateMode(nameQuery) ? null : nameQuery.trim();
		List<WeappSetting> rows =
				weappSettingRepository.listForAdminPageParams(
						companyId, templateName, pageName, version, configNameFilter);
		List<Long> ids = rows.stream().map(WeappSetting::getId).collect(Collectors.toList());
		Map<Long, String> langById =
				weappSettingOutsideLangParamsLoader.findParamsByLocale(companyId, ids, localeTagForI18n);
		List<Map<String, Object>> list = new ArrayList<>();
		Map<String, Object> companyBaseForMerchantStatus = null;
		for (WeappSetting row : rows) {
			Object paramsObj =
					WeappSettingLegacySerializeCodec.decode(row.getParams() == null ? "" : row.getParams());
			String langRaw = langById.get(row.getId());
			if (langRaw != null && !langRaw.isBlank()) {
				paramsObj = WeappSettingLegacySerializeCodec.decode(langRaw);
			}
			if (!(paramsObj instanceof Map<?, ?> rawMap)) {
				throw new ResourceException("装修配置格式无效");
			}
			LinkedHashMap<String, Object> params = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : rawMap.entrySet()) {
				params.put(String.valueOf(e.getKey()), e.getValue());
			}
			params.put("user_id", 0);
			params.put("distributor_id", 0);
			if (!params.containsKey("e_activity_id")) {
				params.put("e_activity_id", 0L);
			}
			String widgetName = row.getName();
			if (shouldEnrichGoodsWidgetForAdmin(widgetName, params)) {
				Boolean merchantStatusPerItemOrNull;
				if (oemShuyun) {
					merchantStatusPerItemOrNull = null;
				} else {
					if (companyBaseForMerchantStatus == null) {
						companyBaseForMerchantStatus =
								merchantBaseSettingSaveService.loadCompanyBaseSetting(companyId);
					}
					Object st = companyBaseForMerchantStatus.get("status");
					merchantStatusPerItemOrNull = (st instanceof Boolean b) ? b : Boolean.FALSE;
				}
				wxaWeappTemplateParamsEnrichPort.enrichAdminRow(
						companyId, widgetName, params, merchantStatusPerItemOrNull);
			}
			String pageNameOut =
					row.getPageName() == null || row.getPageName().isBlank()
							? "index"
							: row.getPageName();
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", row.getId());
			item.put("template_name", row.getTemplateName());
			item.put("company_id", row.getCompanyId());
			item.put("name", row.getName());
			item.put("page_name", pageNameOut);
			item.put("params", params);
			list.add(item);
		}
		if (list.isEmpty()) {
			Map<String, Object> synthetic = new LinkedHashMap<>();
			LinkedHashMap<String, Object> p = new LinkedHashMap<>();
			p.put("is_open", true);
			synthetic.put("params", p);
			list.add(synthetic);
		} else {
			Object p0 = list.get(0).get("params");
			if (p0 instanceof Map<?, ?> pm && !pm.containsKey("is_open")) {
				@SuppressWarnings("unchecked")
				Map<String, Object> pmObj = (Map<String, Object>) (Map<?, ?>) pm;
				pmObj.put("is_open", true);
			}
		}
		if (isFalsyNameForAggregateMode(nameQuery)) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("list", list);
			out.put("config", buildConfigFromList(list));
			return out;
		}
		return list;
	}

	public static List<Map<String, Object>> buildConfigFromList(List<Map<String, Object>> list) {
		List<Map<String, Object>> config = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Object p = row.get("params");
			if (p instanceof Map<?, ?> raw) {
				if (raw.containsKey("name") && raw.containsKey("base")) {
					@SuppressWarnings("unchecked")
					Map<String, Object> params = (Map<String, Object>) (Map<?, ?>) raw;
					config.add(new LinkedHashMap<>(params));
				}
			}
		}
		return config;
	}

	private static boolean isNonEmptyDataList(Object dataObj) {
		return dataObj instanceof List<?> list && !list.isEmpty();
	}

	/**
	 * goodsScroll / goodsGrid: non-empty {@code data} list. goodsGridTab: non-null {@code list} that is a
	 * Map or List (tab container).
	 */
	private static boolean shouldEnrichGoodsWidgetForAdmin(
			String widgetName, LinkedHashMap<String, Object> params) {
		if (widgetName == null) {
			return false;
		}
		return switch (widgetName) {
			case "goodsScroll", "goodsGrid" -> isNonEmptyDataList(params.get("data"));
			case "goodsGridTab" -> hasGoodsGridTabListContainer(params.get("list"));
			default -> false;
		};
	}

	private static boolean hasGoodsGridTabListContainer(Object listObj) {
		if (listObj == null) {
			return false;
		}
		return listObj instanceof Map<?, ?> || listObj instanceof List<?>;
	}

	private static boolean isFalsyNameForAggregateMode(String name) {
		return name == null || name.isBlank() || "0".equals(name);
	}

	private static boolean isTruthyDistributorIdParam(String distributorIdRaw) {
		if (distributorIdRaw == null) {
			return false;
		}
		String t = distributorIdRaw.trim();
		return !t.isEmpty() && !"0".equals(t);
	}

	private static String resolveVersion(String distributorIdRaw, String versionQuery) {
		if (isTruthyDistributorIdParam(distributorIdRaw)) {
			return "shop_" + distributorIdRaw.trim();
		}
		String v = versionQuery == null ? "" : versionQuery.trim();
		return v.isEmpty() ? "v1.0.0" : v;
	}

	public void setPageParams(
			long companyId,
			String templateName,
			String pageName,
			String configName,
			Map<String, Object> params) {
		superadminWxappTemplateMetadataService.assertValidWxappTemplateForPageParams(templateName);
		weappSettingRepository.insertWithParamsIdRewrite(
				companyId, templateName, pageName, configName, params, "v1.0.0", 0, 0);
	}

	@Transactional(rollbackFor = Exception.class)
	public void savePageAllParams(
			long companyId,
			String templateName,
			String pageName,
			String version,
			List<SavePageAllParamsRow> configRows) {
		superadminWxappTemplateMetadataService.assertValidWxappTemplateForPageParams(templateName);
		List<Long> existingIds =
				weappSettingRepository.listIdsByCompanyTemplatePageNameVersion(
						companyId, templateName, pageName, version);
		List<Long> editIds = new ArrayList<>();
		for (SavePageAllParamsRow item : configRows) {
			int sortBy = item.sortBy();
			Map<String, Object> row = new LinkedHashMap<>(item.row());
			Object idObj = row.get("id");
			long rowId = 0;
			if (idObj instanceof Number n && n.longValue() > 0) {
				rowId = n.longValue();
			} else if (idObj instanceof String s) {
				String t = s.trim();
				if (!t.isEmpty()) {
					try {
						long parsed = Long.parseLong(t);
						if (parsed > 0) {
							rowId = parsed;
						}
					} catch (NumberFormatException ignored) {
						rowId = 0;
					}
				}
			}
			String configName = row.get("name") == null ? null : String.valueOf(row.get("name"));
			if (rowId > 0) {
				row.put("id", rowId);
				weappSettingRepository.updateRowByIdWithSerializedParams(
						rowId, companyId, templateName, pageName, version, configName, row, sortBy, 0);
				syncDefaultLangOverlay(companyId, rowId, row);
				editIds.add(rowId);
			} else {
				weappSettingRepository.insertWithParamsIdRewrite(
						companyId, templateName, pageName, configName, row, version, 0, sortBy);
				Object newId = row.get("id");
				if (newId instanceof Number num) {
					long insertedId = num.longValue();
					syncDefaultLangOverlay(companyId, insertedId, row);
					editIds.add(insertedId);
				}
			}
		}
		Set<Long> editSet = new HashSet<>(editIds);
		List<Long> diffIds =
				existingIds.stream().filter(id -> !editSet.contains(id)).collect(Collectors.toList());
		if (!diffIds.isEmpty()) {
			weappSettingOutsideLangParamsWriter.deleteLangParamsForWeappSettingIds(companyId, diffIds);
			weappSettingRepository.deleteByIdsForCompany(diffIds, companyId);
		}
	}

	public void updateParamsById(long companyId, Object idRaw, Object paramsRaw) {
		String serialized = WeappSettingLegacySerializeCodec.serializeLoose(paramsRaw);
		Long id = parseWeappSettingIdForUpdate(idRaw);
		weappSettingRepository.updateParamsById(companyId, id, serialized);
		if (id != null && id > 0L) {
			weappSettingOutsideLangParamsWriter.syncDefaultLangParams(companyId, id, serialized);
		}
	}

	private void syncDefaultLangOverlay(long companyId, long weappSettingId, Map<String, Object> params) {
		String serialized = WeappSettingLegacySerializeCodec.serialize(params);
		weappSettingOutsideLangParamsWriter.syncDefaultLangParams(companyId, weappSettingId, serialized);
	}

	private static Long parseWeappSettingIdForUpdate(Object idRaw) {
		if (idRaw == null) {
			return null;
		}
		if (idRaw instanceof Number n) {
			return n.longValue();
		}
		String s;
		if (idRaw instanceof String str) {
			s = str.trim();
		} else {
			s = String.valueOf(idRaw).trim();
		}
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return -1L;
		}
	}
}
