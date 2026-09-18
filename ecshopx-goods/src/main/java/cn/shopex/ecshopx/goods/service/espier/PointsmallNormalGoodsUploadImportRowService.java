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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.service.items.SupplierItemsAddService;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallNormalGoodsUploadImportRowService {

	private static final ConcurrentHashMap<Long, Session> SESSION = new ConcurrentHashMap<>();

	private final SupplierGoodsImportRowOrchestrator orchestrator;
	private final SupplierItemsAddService supplierItemsAddService;
	private final SupplierItemsRepository supplierItemsRepository;
	private final ItemsAttributesRepository itemsAttributesRepository;

	public PointsmallNormalGoodsUploadImportRowService(
			SupplierGoodsImportRowOrchestrator orchestrator,
			SupplierItemsAddService supplierItemsAddService,
			SupplierItemsRepository supplierItemsRepository,
			ItemsAttributesRepository itemsAttributesRepository) {
		this.orchestrator = orchestrator;
		this.supplierItemsAddService = supplierItemsAddService;
		this.supplierItemsRepository = supplierItemsRepository;
		this.itemsAttributesRepository = itemsAttributesRepository;
	}

	public void acceptRow(long companyId, long distributorId, Map<String, Object> row) {
		Map<String, String> r = trimRow(row);
		validate(r);
		if (StringUtils.hasText(r.get("item_spec"))) {
			throw new BadRequestException("多规格积分商品导入需完整规格与属性库数据，请使用后台商品管理维护");
		}
		long uploadKey = uploadKey(row);
		Session s = uploadKey == 0L ? null : SESSION.computeIfAbsent(uploadKey, k -> new Session());
		boolean isCreateRelData = true;
		Long defaultItemId = null;
		if (s != null) {
			String nm = r.get("item_name").trim();
			if (s.itemName != null && nm.equals(s.itemName)) {
				isCreateRelData = false;
				defaultItemId = s.defaultItemId;
			} else {
				s.itemName = nm;
				s.defaultItemId = null;
			}
		}

		Long goodsId = null;
		Long itemId = null;
		String bn = r.get("item_bn");
		if (StringUtils.hasText(bn)) {
			SupplierItems old = supplierItemsRepository.findByItemBnAndCompany(bn, companyId);
			if (old != null) {
				goodsId = old.getGoodsId();
				itemId = old.getItemId();
				if (old.getDefaultItemId() != null && old.getDefaultItemId() > 0L) {
					defaultItemId = old.getDefaultItemId();
				}
				isCreateRelData = Boolean.TRUE.equals(old.getIsDefault());
			}
		}

		long supplierId = 0L;
		long mainCatId = orchestrator.resolveMainCategoryLeafId(companyId, r.get("item_main_category"));
		long templateId = orchestrator.resolveTemplateId(companyId, r.get("templates_id"), supplierId, distributorId);
		List<Long> saleCatIds = orchestrator.resolveSaleCategories(companyId, distributorId, r.get("item_category"), false);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("supplier_id", supplierId);
		merged.put("operator_type", "");
		merged.put("item_type", "normal");
		merged.put("audit_status", "submitting");
		merged.put("distributor_id", distributorId);
		merged.put("item_main_cat_id", mainCatId);
		merged.put("item_name", r.get("item_name"));
		merged.put("item_bn", bn);
		merged.put("brief", r.get("brief"));
		merged.put("price", 0);
		merged.put("cost_price", moneyToFen(r.get("cost_price")));
		merged.put("market_price", moneyToFen(r.get("market_price")));
		merged.put("point", parsePoint(r.get("point")));
		merged.put("store", parseStoreInt(r.get("store")));
		merged.put("pics", splitComma(r.get("pics")));
		merged.put("intro", buildIntro(r.get("intro")));
		merged.put("videos", r.get("videos"));
		merged.put("item_category", saleCatIds);
		if (templateId > Integer.MAX_VALUE) {
			throw new BadRequestException("运费模板ID超出范围");
		}
		merged.put("templates_id", (int) templateId);
		merged.put("weight", parseWeight(r.get("weight")));
		merged.put("barcode", r.get("barcode"));
		merged.put("item_unit", r.get("item_unit"));
		merged.put("nospec", "true");
		merged.put("is_default", isCreateRelData);
		merged.put("approve_status", "onsale");
		merged.put("isCreateRelData", isCreateRelData);
		if (goodsId != null && goodsId > 0L) {
			merged.put("goods_id", goodsId);
		}
		if (itemId != null && itemId > 0L) {
			merged.put("item_id", itemId);
		}
		if (defaultItemId != null && defaultItemId > 0L) {
			merged.put("default_item_id", defaultItemId);
		}
		String brand = r.get("goods_brand");
		if (StringUtils.hasText(brand)) {
			int bid = itemsAttributesRepository
					.findFirstBrandByCompanyAndName(companyId, brand)
					.map(a -> a.getAttributeId() != null ? a.getAttributeId().intValue() : 0)
					.orElse(0);
			if (bid <= 0) {
				throw new BadRequestException(brand + " 品牌名称不存在");
			}
			merged.put("brand_id", bid);
		}
		supplierItemsAddService.addItemsTransactional(merged);
		if (s != null && isCreateRelData && StringUtils.hasText(bn)) {
			SupplierItems created = supplierItemsRepository.findByItemBnAndCompany(bn.trim(), companyId);
			if (created != null && created.getItemId() != null) {
				s.defaultItemId = created.getItemId();
			}
		}
	}

	private void validate(Map<String, String> r) {
		List<String> errs = new ArrayList<>();
		if (!StringUtils.hasText(r.get("item_name"))) {
			errs.add("请填写商品名称");
		}
		if (!StringUtils.hasText(r.get("point"))) {
			errs.add("请填写正确的积分价格");
		}
		if (!StringUtils.hasText(r.get("store"))) {
			errs.add("请填写库存");
		}
		if (!StringUtils.hasText(r.get("templates_id"))) {
			errs.add("请填写运费模板");
		}
		if (!StringUtils.hasText(r.get("item_main_category"))) {
			errs.add("请上传管理分类");
		}
		if (!StringUtils.hasText(r.get("item_category"))) {
			errs.add("请上传商品分类");
		}
		if (!errs.isEmpty()) {
			throw new BadRequestException(String.join(", ", errs));
		}
		int st = parseStoreInt(r.get("store"));
		if (st < 0 || st > 999_999_999) {
			throw new BadRequestException("库存为0-999999999的整数");
		}
	}

	private static long uploadKey(Map<String, Object> row) {
		Object v = row.get("__upload_file_id__");
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static final class Session {
		volatile String itemName;
		volatile Long defaultItemId;
	}

	private static Map<String, String> trimRow(Map<String, Object> row) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : row.entrySet()) {
			out.put(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue()).trim());
		}
		return out;
	}

	/** 见 {@link SupplierGoodsImportRowOrchestrator}：去掉导出留下的外层引号后再按逗号拆。 */
	private static List<String> splitComma(String pics) {
		if (!StringUtils.hasText(pics)) {
			return List.of();
		}
		String s = stripOuterQuotes(pics.trim());
		List<String> o = new ArrayList<>();
		for (String p : s.split(",")) {
			String t = stripOuterQuotes(p.trim());
			if (StringUtils.hasText(t)) {
				o.add(t);
			}
		}
		return o;
	}

	private static String stripOuterQuotes(String s) {
		if (!StringUtils.hasText(s)) {
			return "";
		}
		String t = s.trim();
		while (!t.isEmpty() && (t.charAt(0) == '\\' || t.charAt(0) == '"')) {
			t = t.substring(1).trim();
		}
		while (!t.isEmpty()) {
			char c = t.charAt(t.length() - 1);
			if (c != '\\' && c != '"') {
				break;
			}
			t = t.substring(0, t.length() - 1).trim();
		}
		return t;
	}

	private static String buildIntro(String introComma) {
		if (!StringUtils.hasText(introComma)) {
			return "";
		}
		StringBuilder intro = new StringBuilder();
		for (String u : introComma.split(",")) {
			String x = u.trim();
			if (!StringUtils.hasText(x)) {
				continue;
			}
			intro.append("<img src=\"").append(x).append("\" style=\"display: block;\">");
		}
		return intro.toString();
	}

	private static int parseStoreInt(String raw) {
		try {
			return new BigDecimal(raw.trim()).intValueExact();
		} catch (Exception e) {
			throw new BadRequestException("库存为0-999999999的整数");
		}
	}

	private static int parsePoint(String raw) {
		try {
			BigDecimal bd = new BigDecimal(raw.trim());
			return bd.setScale(0, RoundingMode.HALF_UP).intValue();
		} catch (Exception e) {
			throw new BadRequestException("请填写正确的积分价格");
		}
	}

	private static int moneyToFen(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0;
		}
		return new BigDecimal(raw.trim()).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private static double parseWeight(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0.0;
		}
		try {
			return new BigDecimal(raw.trim()).doubleValue();
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
}
