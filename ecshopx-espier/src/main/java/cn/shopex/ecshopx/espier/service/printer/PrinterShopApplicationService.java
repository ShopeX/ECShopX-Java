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

package cn.shopex.ecshopx.espier.service.printer;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import cn.shopex.ecshopx.espier.domain.Printer;
import cn.shopex.ecshopx.espier.mapper.PrinterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PrinterShopApplicationService {

	private static final List<String> SUPPORTED_PRINTER_TYPES = List.of("yilianyun");
	private static final String TABLE_LANG = "espier_printer";
	private static final String MODULE_LANG = "espier_printer";
	private static final int DELETE_PAGE_SIZE = 500;

	private final PrinterMapper printerMapper;
	private final CommonLangModReadService commonLangModReadService;
	private final CommonLangModWriteService commonLangModWriteService;
	private final LangueProperties langueProperties;

	public PrinterShopApplicationService(
			PrinterMapper printerMapper,
			CommonLangModReadService commonLangModReadService,
			CommonLangModWriteService commonLangModWriteService,
			LangueProperties langueProperties) {
		this.printerMapper = printerMapper;
		this.commonLangModReadService = commonLangModReadService;
		this.commonLangModWriteService = commonLangModWriteService;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> lists(long companyId, int pageOneBasedIndex, int pageSize, String requestLocaleTag) {
		LambdaQueryWrapper<Printer> base = new LambdaQueryWrapper<Printer>().eq(Printer::getCompanyId, companyId);
		long totalCount = printerMapper.selectCount(base);
		List<Map<String, Object>> list;
		if (totalCount == 0L) {
			list = new ArrayList<>();
		} else {
			@SuppressWarnings("unchecked")
			LambdaQueryWrapper<Printer> q = (LambdaQueryWrapper<Printer>) base.clone();
			q.orderByDesc(Printer::getId);
			if (pageSize > 0) {
				long offset = (long) pageOneBasedIndex - 1L;
				if (offset < 0L) {
					offset = 0L;
				}
				q.last("LIMIT " + pageSize + " OFFSET " + offset);
			}
			List<Printer> entities = printerMapper.selectList(q);
			list = new ArrayList<>(entities.size());
			for (Printer entity : entities) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("id", entity.getId());
				row.put("name", entity.getName());
				row.put("type", entity.getType());
				row.put("company_id", entity.getCompanyId());
				row.put("distributor_id", entity.getDistributorId());
				row.put("app_terminal", entity.getAppTerminal());
				row.put("app_key", entity.getAppKey());
				list.add(row);
			}
		}
		if (totalCount > 0L && !list.isEmpty()) {
			commonLangModReadService.mergeEspierPrinterListNameForLocale(companyId, list, requestLocaleTag);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", list);
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createPrinter(long companyId, Map<String, Object> merged) {
		String name = trimField(merged, "name");
		if (!StringUtils.hasText(name)) {
			throw new ResourceException("打印机名称");
		}

		if (companyId <= 0) {
			throw new ResourceException("公司id必填");
		}

		if (!merged.containsKey("distributor_id")) {
			throw new ResourceException("请选择所属店铺");
		}
		String distributorId = trimField(merged, "distributor_id");
		if (!StringUtils.hasText(distributorId)) {
			throw new ResourceException("请选择所属店铺");
		}

		String appTerminal = trimField(merged, "app_terminal");
		if (!StringUtils.hasText(appTerminal)) {
			throw new ResourceException("请填写终端号");
		}

		String appKey = trimField(merged, "app_key");
		if (!StringUtils.hasText(appKey)) {
			throw new ResourceException("请填写应用密钥");
		}

		String type = trimField(merged, "type");
		if (!StringUtils.hasText(type) || !SUPPORTED_PRINTER_TYPES.contains(type)) {
			throw new ResourceException("打印机配置类型错误");
		}

		LambdaQueryWrapper<Printer> w1 = new LambdaQueryWrapper<Printer>()
				.eq(Printer::getCompanyId, companyId)
				.eq(Printer::getAppTerminal, appTerminal);
		Printer existingTerminal = printerMapper.selectOne(w1);
		if (existingTerminal != null) {
			throw new ResourceException("设备已存在，请查看");
		}

		LambdaQueryWrapper<Printer> w2 = new LambdaQueryWrapper<Printer>()
				.eq(Printer::getCompanyId, companyId)
				.eq(Printer::getDistributorId, distributorId);
		Printer existingShop = printerMapper.selectOne(w2);
		if (existingShop != null) {
			throw new ResourceException("店铺已设置，请查看");
		}

		Printer entity = new Printer();
		entity.setName(name);
		entity.setType(type);
		entity.setCompanyId(companyId);
		entity.setDistributorId(distributorId);
		entity.setAppTerminal(appTerminal);
		entity.setAppKey(appKey);
		printerMapper.insert(entity);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", entity.getId());
		out.put("name", entity.getName());
		out.put("type", entity.getType());
		out.put("company_id", entity.getCompanyId());
		out.put("distributor_id", entity.getDistributorId());
		out.put("app_terminal", entity.getAppTerminal());
		out.put("app_key", entity.getAppKey());
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updatePrinter(long companyId, String idPath, Map<String, Object> merged) {
		if (idPath == null || !StringUtils.hasText(idPath.trim())) {
			throw new ResourceException("非法 id");
		}
		long printerId;
		try {
			printerId = Long.parseLong(idPath.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("非法 id");
		}

		String name = trimField(merged, "name");
		if (!StringUtils.hasText(name)) {
			throw new ResourceException("打印机名称");
		}

		if (companyId <= 0) {
			throw new ResourceException("公司id必填");
		}

		if (!merged.containsKey("distributor_id")) {
			throw new ResourceException("请选择所属店铺");
		}
		String distributorId = trimField(merged, "distributor_id");
		if (!StringUtils.hasText(distributorId)) {
			throw new ResourceException("请选择所属店铺");
		}

		String appTerminal = trimField(merged, "app_terminal");
		if (!StringUtils.hasText(appTerminal)) {
			throw new ResourceException("请填写终端号");
		}

		String appKey = trimField(merged, "app_key");
		if (!StringUtils.hasText(appKey)) {
			throw new ResourceException("请填写应用密钥");
		}

		String type = trimField(merged, "type");
		if (!StringUtils.hasText(type) || !SUPPORTED_PRINTER_TYPES.contains(type)) {
			throw new ResourceException("打印机配置类型错误");
		}

		LambdaQueryWrapper<Printer> w1 = new LambdaQueryWrapper<Printer>()
				.eq(Printer::getCompanyId, companyId)
				.eq(Printer::getAppTerminal, appTerminal);
		Printer byTerminal = printerMapper.selectOne(w1);
		if (byTerminal != null && !Objects.equals(byTerminal.getId(), printerId)) {
			throw new ResourceException("设备已存在，请查看");
		}

		LambdaQueryWrapper<Printer> w2 = new LambdaQueryWrapper<Printer>()
				.eq(Printer::getCompanyId, companyId)
				.eq(Printer::getDistributorId, distributorId);
		Printer byShop = printerMapper.selectOne(w2);
		if (byShop != null && !Objects.equals(byShop.getId(), printerId)) {
			throw new ResourceException("店铺已设置，请查看");
		}

		Printer entity = printerMapper.selectById(printerId);
		if (entity == null || !Objects.equals(entity.getCompanyId(), companyId)) {
			throw new ResourceException("未查询到更新数据");
		}

		entity.setName(name);
		entity.setType(type);
		entity.setCompanyId(companyId);
		entity.setDistributorId(distributorId);
		entity.setAppTerminal(appTerminal);
		entity.setAppKey(appKey);

		int rows = printerMapper.updateById(entity);
		if (rows != 1) {
			throw new ResourceException("未查询到更新数据");
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", entity.getId());
		out.put("name", entity.getName());
		out.put("type", entity.getType());
		out.put("company_id", entity.getCompanyId());
		out.put("distributor_id", entity.getDistributorId());
		out.put("app_terminal", entity.getAppTerminal());
		out.put("app_key", entity.getAppKey());
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deletePrinter(long companyId, String idPath) {
		if (idPath == null || !StringUtils.hasText(idPath.trim())) {
			return;
		}
		String trimmed = idPath.trim();
		Long printerId;
		try {
			printerId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return;
		}

		LambdaQueryWrapper<Printer> base = new LambdaQueryWrapper<Printer>()
				.eq(Printer::getCompanyId, companyId)
				.eq(Printer::getId, printerId);

		int pageNo = 1;
		while (true) {
			Page<Printer> page = new Page<>(pageNo, DELETE_PAGE_SIZE);
			Page<Printer> result = printerMapper.selectPage(page, base);
			if (result.getRecords().isEmpty()) {
				break;
			}
			for (Printer row : result.getRecords()) {
				Long dataId = row.getId();
				if (dataId == null) {
					continue;
				}
				List<String> langs = langueProperties.getList();
				if (langs == null) {
					continue;
				}
				for (String lang : langs) {
					if (StringUtils.hasText(lang)) {
						commonLangModWriteService.deleteLang(
								(int) companyId, TABLE_LANG, dataId.longValue(), MODULE_LANG, lang);
					}
				}
			}
			if (result.getRecords().size() < DELETE_PAGE_SIZE) {
				break;
			}
			pageNo++;
		}

		printerMapper.delete(base);
	}

	private static String trimField(Map<String, Object> merged, String key) {
		Object v = merged.get(key);
		return v == null ? "" : String.valueOf(v).trim();
	}
}
