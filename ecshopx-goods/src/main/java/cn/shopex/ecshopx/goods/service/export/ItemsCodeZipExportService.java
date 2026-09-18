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

package cn.shopex.ecshopx.goods.service.export;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.espier.service.ExportZipFileService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import cn.shopex.ecshopx.goods.service.items.SupplierLinkedMainItemIdResolver;
import cn.shopex.ecshopx.merchant.port.CompanyDomainInfoRead;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsListQueryRepository;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsCodeZipExportService {

	private static final int ITEMS_CODE_EXPORT_BATCH_SIZE = 2;

	private static final String ITEM_DETAIL_PAGE = "subpages/item/espier-detail";

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final SupplierItemsListQueryRepository supplierItemsListQueryRepository;
	private final ItemsRepository itemsRepository;
	private final DistributorListQueryService distributorListQueryService;
	private final CompanyDomainInfoRead companyDomainInfoRead;
	private final ItemsCodeH5UrlQrcodePngService itemsCodeH5UrlQrcodePngService;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;
	private final ExportZipFileService exportZipFileService;
	private final ExportLogCreateService exportLogCreateService;

	public ItemsCodeZipExportService(ItemsListQueryRepository itemsListQueryRepository,
			SupplierItemsListQueryRepository supplierItemsListQueryRepository,
			ItemsRepository itemsRepository,
			DistributorListQueryService distributorListQueryService,
			CompanyDomainInfoRead companyDomainInfoRead,
			ItemsCodeH5UrlQrcodePngService itemsCodeH5UrlQrcodePngService,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient,
			ExportZipFileService exportZipFileService,
			ExportLogCreateService exportLogCreateService) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.supplierItemsListQueryRepository = supplierItemsListQueryRepository;
		this.itemsRepository = itemsRepository;
		this.distributorListQueryService = distributorListQueryService;
		this.companyDomainInfoRead = companyDomainInfoRead;
		this.itemsCodeH5UrlQrcodePngService = itemsCodeH5UrlQrcodePngService;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
		this.exportZipFileService = exportZipFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(ItemsCodeExportContext ctx) {
		long companyId = ctx.getCompanyId();
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(ctx.getFilterParams());

		// items / supplier_items.supplier_id 存的是 operator_id，不是 supplier 表主键
		if ("supplier".equalsIgnoreCase(ctx.getOperatorType())) {
			work.put(ItemsListQueryRepository.KEY_SUPPLIER_ID_EQ, (int) ctx.getOperatorId());
		}

		work.remove("merchant_id");
		String exportType = str(work.remove("export_type"));
		if (!StringUtils.hasText(exportType)) {
			exportType = "wxa";
		}
		String wxaAppid = str(work.remove("wxaappid"));
		String itemSource = str(work.get("item_source"));
		if (!StringUtils.hasText(itemSource)) {
			itemSource = "item";
		}
		work.remove("item_source");

		if ("distributor".equalsIgnoreCase(itemSource) && !hasPositiveDistributorFilter(work)) {
			return;
		}

		String operatorType = str(work.remove("operator_type"));
		if (!StringUtils.hasText(operatorType)) {
			operatorType = ctx.getOperatorType() != null ? ctx.getOperatorType() : "";
		}

		work = applyItemIdBranchSupplierIdPreservation(work, companyId);

		Object skuFlag = work.remove("isGetSkuList");
		boolean skuMode = Boolean.TRUE.equals(skuFlag);

		long total;
		if ("supplier".equalsIgnoreCase(operatorType)) {
			LinkedHashMap<String, Object> q = new LinkedHashMap<>(work);
			if (skuMode) {
				q.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN);
				q.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ);
				q.remove("is_default_true");
			}
			total = supplierItemsListQueryRepository.countByParams(q);
		} else {
			LinkedHashMap<String, Object> q = new LinkedHashMap<>(work);
			if (skuMode) {
				q.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN);
				q.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ);
				q.remove("is_default_true");
			}
			total = itemsListQueryRepository.countByParams(q);
		}

		if (total <= 0) {
			return;
		}

		Map<Long, String> distributorNameById = loadDistributorNames(companyId, work);

		String stamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault()).format(Instant.now());
		String tarBase = stamp + "productcode_" + exportType + ("distributor".equalsIgnoreCase(itemSource) ? "_distributor" : "");

		Path rootDir = null;
		try {
			rootDir = Files.createTempDirectory("items-code-export-");
			int batches = (int) Math.ceil(total / (double) ITEMS_CODE_EXPORT_BATCH_SIZE);
			for (int page = 0; page < batches; page++) {
				int off = page * ITEMS_CODE_EXPORT_BATCH_SIZE;
				List<Map<String, Object>> rows = loadBatch(ctx, work, operatorType, skuMode, off, ITEMS_CODE_EXPORT_BATCH_SIZE);
				for (Map<String, Object> row : rows) {
					writeOnePng(rootDir, row, companyId, exportType, wxaAppid, itemSource, distributorNameById);
				}
			}

			Map<String, String> uploaded = exportZipFileService.uploadZipFromDirectory(rootDir, tarBase);
			if (uploaded.isEmpty()) {
				return;
			}
			String fileName = uploaded.getOrDefault("filename", tarBase + ".zip");
			String url = uploaded.getOrDefault("url", "");
			long epoch = Instant.now().getEpochSecond();
			long merchantId = ctx.getMerchantId() != null ? ctx.getMerchantId() : 0L;
			long supplierIdForLog = "supplier".equalsIgnoreCase(ctx.getOperatorType()) ? ctx.getOperatorId() : 0L;
			exportLogCreateService.createFinishLog(companyId, ctx.getOperatorId(), merchantId, supplierIdForLog,
					"itemcode", fileName, url, epoch);
		} catch (Exception e) {
			throw new IllegalStateException("商品码导出失败", e);
		} finally {
			if (rootDir != null) {
				deleteRecursively(rootDir);
			}
		}
	}

	private List<Map<String, Object>> loadBatch(ItemsCodeExportContext ctx, LinkedHashMap<String, Object> work,
			String operatorType, boolean skuMode, int offset, int limit) {
		long companyId = ctx.getCompanyId();
		if ("supplier".equalsIgnoreCase(operatorType)) {
			LinkedHashMap<String, Object> q = new LinkedHashMap<>(work);
			if (skuMode) {
				q.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN);
				q.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ);
				q.remove("is_default_true");
			}
			List<SupplierItems> srows = supplierItemsListQueryRepository.selectPageByParams(q, offset, limit);
			List<Long> supItemIds = srows.stream().map(SupplierItems::getItemId).filter(id -> id != null).collect(Collectors.toList());
			Map<Integer, Long> mainItemIdBySupItemId =
					SupplierLinkedMainItemIdResolver.resolve(itemsRepository, companyId, supItemIds, 0L);
			List<Map<String, Object>> rows = new ArrayList<>();
			for (SupplierItems sr : srows) {
				if (sr.getItemId() == null) {
					continue;
				}
				long mainItemId = mainItemIdBySupItemId.getOrDefault(sr.getItemId().intValue(), 0L);
				rows.add(GoodsItemsListRowMapper.toRowFromSupplier(sr, mainItemId));
			}
			return rows;
		}
		LinkedHashMap<String, Object> q = new LinkedHashMap<>(work);
		if (skuMode) {
			q.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN);
			q.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ);
			q.remove("is_default_true");
			List<Items> items = itemsListQueryRepository.selectPageByParamsForSku(q, offset, limit);
			return items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		}
		List<Items> items = itemsListQueryRepository.selectPageByParams(q, offset, limit);
		return items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
	}

	private void writeOnePng(Path rootDir, Map<String, Object> row, long companyId, String exportType, String wxaAppid,
			String itemSource, Map<Long, String> distributorNameById) throws IOException {
		Object iid = row.get("item_id");
		long itemId = iid instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(iid));
		Object did = row.get("distributor_id");
		long distributorId = did instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(did != null ? did : "0"));
		String itemBn = str(row.get("item_bn"));
		if (!StringUtils.hasText(itemBn)) {
			itemBn = "item_" + itemId;
		}
		String safeBn = itemBn.replace('/', '_').replace('\\', '_');

		byte[] png;
		if ("h5".equalsIgnoreCase(exportType)) {
			Map<String, Object> di = companyDomainInfoRead.getDomainInfo(companyId);
			String h5 = str(di.get("h5_domain"));
			if (!StringUtils.hasText(h5)) {
				h5 = str(di.get("h5_default_domain"));
			}
			String url = String.format("https://%s/%s?id=%s&dtid=%s", h5, ITEM_DETAIL_PAGE, itemId, distributorId);
			png = itemsCodeH5UrlQrcodePngService.toPngBytes(url, 256, 256);
		} else {
			String scene = "id=" + itemId + "&dtid=" + distributorId;
			png = fetchWxaPng(wxaAppid, scene);
		}

		Path targetDir;
		if ("distributor".equalsIgnoreCase(itemSource) && distributorId > 0) {
			String dname = distributorNameById.getOrDefault(distributorId, "");
			String sub = (StringUtils.hasText(dname) ? dname : "d") + "_" + distributorId;
			sub = sub.replace('/', '_').replace('\\', '_');
			targetDir = rootDir.resolve(sub);
		} else {
			targetDir = rootDir;
		}
		Files.createDirectories(targetDir);
		Files.write(targetDir.resolve(safeBn + ".png"), png);
	}

	private byte[] fetchWxaPng(String wxaAppid, String scene) {
		try {
			return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxaAppid, scene, ITEM_DETAIL_PAGE);
		} catch (Exception e) {
			return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxaAppid, scene, "pages/goodsdetail");
		}
	}

	private Map<Long, String> loadDistributorNames(long companyId, Map<String, Object> work) {
		List<Long> ids = new ArrayList<>();
		Object eq = work.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ);
		if (eq != null) {
			long v = eq instanceof Number n ? n.longValue() : Long.parseLong(str(eq));
			if (v > 0) {
				ids.add(v);
			}
		}
		@SuppressWarnings("unchecked")
		List<Integer> din = (List<Integer>) work.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN);
		if (din != null) {
			for (Integer i : din) {
				if (i != null && i > 0) {
					ids.add(i.longValue());
				}
			}
		}
		if (ids.isEmpty()) {
			return Map.of();
		}
		List<Distributor> dists = distributorListQueryService.listByIdsAndCompany(companyId, ids.stream().distinct().toList());
		Map<Long, String> m = new LinkedHashMap<>();
		for (Distributor d : dists) {
			if (d.getDistributorId() != null) {
				m.put(d.getDistributorId(), d.getName() != null ? d.getName() : "");
			}
		}
		return m;
	}

	private static boolean hasPositiveDistributorFilter(Map<String, Object> work) {
		Object eq = work.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ);
		if (eq != null) {
			long v = eq instanceof Number n ? n.longValue() : Long.parseLong(str(eq));
			if (v > 0) {
				return true;
			}
		}
		@SuppressWarnings("unchecked")
		List<Integer> din = (List<Integer>) work.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN);
		if (din != null) {
			for (Integer i : din) {
				if (i != null && i > 0) {
					return true;
				}
			}
		}
		return false;
	}

	private LinkedHashMap<String, Object> applyItemIdBranchSupplierIdPreservation(Map<String, Object> source, long companyId) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>(source);
		if (!hasNonEmptyItemIdCondition(filter)) {
			return filter;
		}
		LinkedHashMap<String, Object> supplierPreserve = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : filter.entrySet()) {
			if (e.getKey() != null && e.getKey().startsWith("supplier_id")) {
				supplierPreserve.put(e.getKey(), e.getValue());
			}
		}
		@SuppressWarnings("unchecked")
		List<Long> itemIds = (List<Long>) filter.get(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS);
		if (itemIds == null || itemIds.isEmpty()) {
			return filter;
		}
		int distributor = 0;
		if (filter.containsKey(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ)) {
			Object eq = filter.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ);
			distributor = eq instanceof Number n ? n.intValue() : Integer.parseInt(str(eq));
		} else {
			@SuppressWarnings("unchecked")
			List<Integer> din = (List<Integer>) filter.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN);
			if (din != null && din.size() == 1) {
				distributor = din.get(0);
			}
		}
		Boolean isDef = Boolean.TRUE.equals(filter.get("is_default_true"));
		LinkedHashMap<String, Object> slim = new LinkedHashMap<>();
		slim.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		slim.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, new ArrayList<>(itemIds));
		if (isDef != null && isDef) {
			slim.put("is_default_true", Boolean.TRUE);
		}
		Object isDefObj = filter.get("is_default");
		if (isDefObj != null) {
			slim.put("is_default", isDefObj);
		}
		slim.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, distributor);
		slim.putAll(supplierPreserve);
		return slim;
	}

	private static boolean hasNonEmptyItemIdCondition(Map<String, Object> w) {
		@SuppressWarnings("unchecked")
		List<Long> a = (List<Long>) w.get(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS);
		if (a != null && !a.isEmpty()) {
			return true;
		}
		Object raw = w.get("item_id");
		if (raw instanceof List<?> l && !l.isEmpty()) {
			return true;
		}
		return false;
	}

	private static void deleteRecursively(Path root) {
		try {
			if (root == null || !Files.exists(root)) {
				return;
			}
			Files.walk(root).sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				} catch (IOException ignored) {
					// ignore
				}
			});
		} catch (IOException ignored) {
			// ignore
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
