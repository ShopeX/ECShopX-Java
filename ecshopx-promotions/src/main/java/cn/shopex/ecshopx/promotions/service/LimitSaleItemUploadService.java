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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.promotions.domain.LimitItemPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LimitSaleItemUploadService {

	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;
	private final LimitPromotionsMapper limitPromotionsMapper;
	private final LimitItemPromotionsMapper limitItemPromotionsMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final DistributorListQueryService distributorListQueryService;

	public LimitSaleItemUploadService(
			MessageSource messageSource,
			ObjectMapper objectMapper,
			LimitPromotionsMapper limitPromotionsMapper,
			LimitItemPromotionsMapper limitItemPromotionsMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			DistributorListQueryService distributorListQueryService) {
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
		this.limitPromotionsMapper = limitPromotionsMapper;
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.distributorListQueryService = distributorListQueryService;
	}

	public byte[] exportErrorDesc(long companyId, String limitIdRaw, String fileName, Locale locale) {
		LambdaQueryWrapper<LimitPromotions> w =
				new LambdaQueryWrapper<LimitPromotions>().eq(LimitPromotions::getCompanyId, companyId);
		if (limitIdRaw != null && StringUtils.hasText(limitIdRaw.trim())) {
			w.eq(LimitPromotions::getLimitId, LeadingNumberParser.parseAsLong(limitIdRaw.trim()));
		} else {
			w.isNull(LimitPromotions::getLimitId);
		}
		w.last("LIMIT 1");
		LimitPromotions row = limitPromotionsMapper.selectOne(w);

		List<List<String>> demoDataList = new ArrayList<>();
		if (row != null) {
			String errorDesc = row.getErrorDesc();
			if (errorDesc != null && StringUtils.hasText(errorDesc.trim())) {
				String[] parts = errorDesc.split(";", -1);
				for (int k = 0; k < parts.length; k++) {
					if (k >= 100) {
						demoDataList.add(
								Collections.singletonList(
										messageSource.getMessage(
												"promotions.limit.error_rows_too_many_show_100", null, locale)));
						break;
					}
					String segment = parts[k];
					if (!StringUtils.hasText(segment == null ? "" : segment.trim())) {
						continue;
					}
					demoDataList.add(Collections.singletonList(segment));
				}
			}
		}

		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(fileName));
			XSSFRow headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("错误描述");
			int rowIdx = 1;
			for (List<String> line : demoDataList) {
				XSSFRow r = sheet.createRow(rowIdx++);
				r.createCell(0).setCellValue(line.isEmpty() ? "" : line.get(0));
			}
			wb.write(out);
			return out.toByteArray();
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	public Map<String, Object> uploadLimitItems(MultipartFile file) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		if (file == null || file.isEmpty()) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.limit.upload_xlsx_required", null, "请上传 xlsx 文件", locale));
		}
		String original = file.getOriginalFilename();
		String extension = "";
		if (original != null) {
			int dot = original.lastIndexOf('.');
			if (dot >= 0 && dot < original.length() - 1) {
				extension = original.substring(dot + 1);
			}
		}
		if (!"xlsx".equals(extension)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.limit.upload_xlsx_required", null, "请上传 xlsx 文件", locale));
		}
		try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
			Sheet sheet = wb.getSheetAt(0);
			List<List<Object>> rawRows = new ArrayList<>();
			int first = sheet.getFirstRowNum();
			int last = sheet.getLastRowNum();
			for (int r = first; r <= last; r++) {
				Row row = sheet.getRow(r);
				if (row == null) {
					rawRows.add(new ArrayList<>());
				} else if (row.getFirstCellNum() == -1) {
					rawRows.add(new ArrayList<>());
				} else {
					List<Object> line = new ArrayList<>();
					for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
						Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
						line.add(cellValue(cell));
					}
					rawRows.add(line);
				}
			}
			int totalWithHeader = rawRows.size();
			if (totalWithHeader < 2) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.limit.import_file_unrecognizable", null, "导入文件无法识别", locale));
			}
			if (totalWithHeader > 20001) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.limit.import_max_20000_rows", null, "一次最多导入 20000 行", locale));
			}
			List<List<Object>> dataRows = new ArrayList<>(rawRows.subList(1, totalWithHeader));
			Map<String, Object> out = new java.util.LinkedHashMap<>();
			out.put("total_count", dataRows.size());
			out.put("list", dataRows);
			return out;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public List<String> saveLimitItems(Map<String, Object> params) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		try {
			long companyId = readCompanyId(params);
			long limitId = readRequiredLimitId(params, locale);
			String itemDataJson = readItemDataJsonString(params, locale);
			int totalCount =
					readRequiredPositiveInt(
							params,
							"total_count",
							"promotions.limit.total_count_required",
							"总数必须大于等于1",
							locale);
			int page =
					readRequiredPositiveInt(params, "page", "promotions.limit.page_required", "页码必须大于等于1", locale);
			int pageSize =
					readRequiredPositiveInt(
							params,
							"page_size",
							"promotions.limit.page_size_required",
							"分页大小必须大于等于1",
							locale);

			List<List<Object>> itemDataRows = parseItemDataRows(itemDataJson);
			if (itemDataRows.isEmpty()) {
				throw new BadRequestException("限购数量解析错误！");
			}

			LimitPromotions limitRow =
					limitPromotionsMapper.selectOne(
							new LambdaQueryWrapper<LimitPromotions>()
									.eq(LimitPromotions::getLimitId, limitId)
									.eq(LimitPromotions::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (limitRow == null) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.limit.no_update_data_found", null, "未查询到更新数据", locale));
			}

			List<String> errorDesc = new ArrayList<>();
			if (page > 1) {
				String prior = limitRow.getErrorDesc();
				if (StringUtils.hasText(prior)) {
					Arrays.stream(prior.split(";"))
							.map(String::trim)
							.filter(StringUtils::hasText)
							.forEach(errorDesc::add);
				}
			}

			LinkedHashSet<String> shopCodesUnique = new LinkedHashSet<>();
			LinkedHashSet<String> itemBnsUnique = new LinkedHashSet<>();
			for (List<Object> line : itemDataRows) {
				shopCodesUnique.add(cellText(line, 0));
				itemBnsUnique.add(cellText(line, 1));
			}
			if (shopCodesUnique.size() > 300) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.limit.import_max_300_stores", null, "一次最多导入300个店铺", locale));
			}

			Map<String, Long> distributorIds =
					distributorListQueryService.mapShopCodeToDistributorId(companyId, shopCodesUnique);
			Map<String, Long> itemIds =
					marketingActivityCatalogAccess.mapItemBnToItemIdForCompany(companyId, itemBnsUnique);

			if (limitRow.getStartTime() == null || limitRow.getEndTime() == null) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.limit.activity_time_required", null, "活动时间必填", locale));
			}
			int actStart = limitRow.getStartTime();
			int actEnd = limitRow.getEndTime();

			String itemType = Objects.toString(params.get("use_bound"), "normal");

			for (int k = 0; k < itemDataRows.size(); k++) {
				List<Object> line = itemDataRows.get(k);
				int currLine = k + (page - 1) * pageSize + 1;
				String v0 = cellText(line, 0);
				Long distributorId = distributorIds.get(v0);
				if (distributorId == null) {
					appendErrorDesc(errorDesc, "第" + currLine + "行错误, 无法识别的店铺码：" + v0);
					continue;
				}
				String v1 = cellText(line, 1);
				Long itemId = itemIds.get(v1);
				if (itemId == null) {
					appendErrorDesc(errorDesc, "第" + currLine + "行错误, 无法识别的商品货号：" + v1);
					continue;
				}
				Object v2Raw = colObj(line, 2);
				String v2RawStr = Objects.toString(v2Raw, "");
				int limitNum;
				try {
					limitNum = Integer.parseInt(v2RawStr.trim());
				} catch (NumberFormatException ex) {
					appendErrorDesc(
							errorDesc,
							"第" + currLine + "行错误, 限购数量必须在1-9999之间，输入的值：" + v2RawStr);
					continue;
				}
				if (limitNum <= 0 || limitNum > 9999) {
					appendErrorDesc(
							errorDesc,
							"第" + currLine + "行错误, 限购数量必须在1-9999之间，输入的值：" + v2RawStr);
					continue;
				}

				limitItemPromotionsMapper.delete(
						new LambdaQueryWrapper<LimitItemPromotions>()
								.eq(LimitItemPromotions::getCompanyId, companyId)
								.eq(LimitItemPromotions::getDistributorId, distributorId)
								.eq(LimitItemPromotions::getItemId, itemId)
								.eq(LimitItemPromotions::getLimitId, limitId));

				long overlap =
						limitItemPromotionsMapper.selectCount(
								new LambdaQueryWrapper<LimitItemPromotions>()
										.eq(LimitItemPromotions::getCompanyId, companyId)
										.eq(LimitItemPromotions::getDistributorId, distributorId)
										.eq(LimitItemPromotions::getItemId, itemId)
										.le(LimitItemPromotions::getStartTime, actEnd)
										.ge(LimitItemPromotions::getEndTime, actStart));
				if (overlap > 0) {
					appendErrorDesc(errorDesc, "第" + currLine + "行错误, 商品已经存在限购：" + v1);
					continue;
				}

				int ts = (int) (System.currentTimeMillis() / 1000L);
				LimitItemPromotions ins = new LimitItemPromotions();
				ins.setLimitId(limitId);
				ins.setDistributorId(distributorId);
				ins.setItemId(itemId);
				ins.setLimitNum((long) limitNum);
				ins.setCompanyId(companyId);
				ins.setItemType(itemType);
				ins.setItemName("");
				ins.setItemSpecDesc("");
				ins.setPics("");
				ins.setPrice(0);
				ins.setStartTime(actStart);
				ins.setEndTime(actEnd);
				ins.setCreated(ts);
				ins.setUpdated(ts);
				limitItemPromotionsMapper.insert(ins);
			}

			String errorDescJoined = String.join(";", errorDesc);
			int validItemNum = (page - 1) * pageSize + itemDataRows.size();
			limitRow.setErrorDesc(errorDescJoined);
			limitRow.setValidItemNum(validItemNum);
			if (page == 1) {
				limitRow.setTotalItemNum(totalCount);
			}
			limitRow.setUpdated((int) (System.currentTimeMillis() / 1000L));
			int n = limitPromotionsMapper.updateById(limitRow);
			if (n != 1) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.limit.no_update_data_found", null, "未查询到更新数据", locale));
			}
			return List.of("success");
		} catch (BadRequestException | ResourceException e) {
			throw e;
		} catch (Exception e) {
			String m = e.getMessage();
			throw new ResourceException(m == null || m.isEmpty() ? "error" : m);
		}
	}

	private long readCompanyId(Map<String, Object> params) {
		Object v = params.get("company_id");
		if (v == null) {
			throw new BadRequestException("参数错误");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new BadRequestException("参数错误");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
	}

	private long readRequiredLimitId(Map<String, Object> params, Locale locale) {
		Object v = params.get("limit_id");
		String s = Objects.toString(v, "").trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.limit.activity_id_required", null, "活动ID不能为空", locale));
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.limit.activity_id_required", null, "活动ID不能为空", locale));
		}
	}

	private String readItemDataJsonString(Map<String, Object> params, Locale locale) {
		Object rawObj = params.get("item_data");
		if (rawObj == null) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.limit.item_data_required", null, "限购数量数据不能为空", locale));
		}
		String raw;
		if (rawObj instanceof String str) {
			raw = str;
		} else {
			try {
				raw = objectMapper.writeValueAsString(rawObj);
			} catch (Exception e) {
				throw new BadRequestException("限购数量解析错误！");
			}
		}
		if (!StringUtils.hasText(raw.trim())) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.limit.item_data_required", null, "限购数量数据不能为空", locale));
		}
		return raw;
	}

	private List<List<Object>> parseItemDataRows(String json) {
		List<List<Object>> parsed;
		try {
			parsed = objectMapper.readValue(json, new TypeReference<List<List<Object>>>() {});
		} catch (Exception e) {
			throw new BadRequestException("限购数量解析错误！");
		}
		if (parsed == null) {
			throw new BadRequestException("限购数量解析错误！");
		}
		List<List<Object>> out = new ArrayList<>();
		for (Object row : parsed) {
			if (row instanceof List<?> rl) {
				List<Object> line = new ArrayList<>();
				for (Object c : rl) {
					line.add(c);
				}
				out.add(line);
			} else {
				out.add(new ArrayList<>());
			}
		}
		return out;
	}

	private int readRequiredPositiveInt(
			Map<String, Object> params, String key, String messageKey, String defaultZh, Locale locale) {
		Object v = params.get(key);
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, defaultZh, locale));
		}
		if (v instanceof Number n) {
			if (n instanceof Double d && d != Math.rint(d)) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, defaultZh, locale));
			}
			if (n instanceof Float f && f != Math.rint(f)) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, defaultZh, locale));
			}
			long lv = n.longValue();
			if (lv < 1L || lv > Integer.MAX_VALUE) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, defaultZh, locale));
			}
			return (int) lv;
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, defaultZh, locale));
		}
		try {
			int x = Integer.parseInt(s);
			if (x < 1) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, defaultZh, locale));
			}
			return x;
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, defaultZh, locale));
		}
	}

	private static void appendErrorDesc(List<String> errorDesc, String errMsg) {
		if (errorDesc.size() < 101) {
			errorDesc.add(errMsg);
		}
	}

	private static Object colObj(List<?> row, int idx) {
		if (row == null || idx < 0 || idx >= row.size()) {
			return "";
		}
		return row.get(idx);
	}

	private static String cellText(List<?> row, int idx) {
		return Objects.toString(colObj(row, idx), "").trim();
	}

	private Object cellValue(Cell cell) {
		if (cell == null) {
			return "";
		}
		CellType cellType = cell.getCellType();
		if (cellType == CellType.BLANK) {
			return "";
		}
		if (cellType == CellType.ERROR) {
			throw new ResourceException(FormulaError.forInt(cell.getErrorCellValue()).getString());
		}
		if (cellType == CellType.FORMULA) {
			CellType cached = cell.getCachedFormulaResultType();
			if (cached == CellType.ERROR) {
				throw new ResourceException(FormulaError.forInt(cell.getErrorCellValue()).getString());
			}
			return cellValueByDataType(cell, cached);
		}
		return cellValueByDataType(cell, cellType);
	}

	private Object cellValueByDataType(Cell cell, CellType type) {
		return switch (type) {
			case STRING -> cell.getStringCellValue();
			case BOOLEAN -> cell.getBooleanCellValue();
			case NUMERIC -> {
				if (DateUtil.isCellDateFormatted(cell)) {
					yield cell.getLocalDateTimeCellValue().toString();
				}
				yield narrowNumericCellValue(cell.getNumericCellValue());
			}
			case BLANK -> "";
			case ERROR -> throw new ResourceException(FormulaError.forInt(cell.getErrorCellValue()).getString());
			case FORMULA -> throw new IllegalStateException("nested formula");
			default -> "";
		};
	}

	/**
	 * When the cell value is a whole number within int/long range, returns {@link Integer} or {@link Long} so JSON
	 * serialization emits a number without a fractional part; otherwise returns the value as {@code double}.
	 */
	private static Object narrowNumericCellValue(double d) {
		if (!Double.isFinite(d)) {
			return d;
		}
		boolean whole = d == Math.rint(d) || Math.abs(d - Math.round(d)) < 1e-9;
		if (!whole) {
			return d;
		}
		double r = Math.rint(d);
		if (r >= Integer.MIN_VALUE && r <= Integer.MAX_VALUE) {
			return (int) r;
		}
		if (r >= Long.MIN_VALUE && r <= Long.MAX_VALUE) {
			return (long) r;
		}
		return d;
	}
}
