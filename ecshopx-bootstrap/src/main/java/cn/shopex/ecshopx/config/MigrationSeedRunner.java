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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.shopmenuborder.service.ShopMenuUploadService;
import cn.shopex.ecshopx.superadmin.domain.Logistics;
import cn.shopex.ecshopx.superadmin.domain.ShopMenu;
import cn.shopex.ecshopx.superadmin.domain.ShopMenuRelType;
import cn.shopex.ecshopx.superadmin.mapper.LogisticsMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuRelTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * 迁移后初始化：对齐 PHP {@code EspierBundle\Listeners\UpdateMenuListener}。
 *
 * <p>在 Spring 上下文就绪、Flyway 迁移完成后运行一次：物流公司仅在空表时导入；系统菜单在
 * {@code common.use-system-menu=true} 时先删除全部 {@code company_id=0} 菜单及类型关联，再逐文件重导入。
 */
@Component
public class MigrationSeedRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MigrationSeedRunner.class);

	private static final int BORDER_COMPANY_ID = 0;
	private static final String KUAIDI_RESOURCE = "classpath:seed/logistics/kuaidi.json";
	private static final List<String> MENU_RESOURCES = List.of(
			"classpath:seed/menu/platform_menu.json",
			"classpath:seed/menu/it_menu.json",
			"classpath:seed/menu/shop_menu.json",
			"classpath:seed/menu/dealer_menu.json",
			"classpath:seed/menu/merchant_menu.json",
			"classpath:seed/menu/supplier_menu.json");

	private final LogisticsMapper logisticsMapper;
	private final ShopMenuMapper shopMenuMapper;
	private final ShopMenuRelTypeMapper shopMenuRelTypeMapper;
	private final ShopMenuUploadService shopMenuUploadService;
	private final ObjectMapper objectMapper;
	private final ResourceLoader resourceLoader;
	private final TransactionTemplate transactionTemplate;

	@Value("${common.use-system-menu:true}")
	private boolean useSystemMenu;

	public MigrationSeedRunner(
			LogisticsMapper logisticsMapper,
			ShopMenuMapper shopMenuMapper,
			ShopMenuRelTypeMapper shopMenuRelTypeMapper,
			ShopMenuUploadService shopMenuUploadService,
			ObjectMapper objectMapper,
			ResourceLoader resourceLoader,
			TransactionTemplate transactionTemplate) {
		this.logisticsMapper = logisticsMapper;
		this.shopMenuMapper = shopMenuMapper;
		this.shopMenuRelTypeMapper = shopMenuRelTypeMapper;
		this.shopMenuUploadService = shopMenuUploadService;
		this.objectMapper = objectMapper;
		this.resourceLoader = resourceLoader;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		initLogistics();
		updateSystemMenus();
	}

	/** 物流公司初始化：仅当 {@code logistics} 表为空时导入。失败记日志但不阻断启动。 */
	void initLogistics() {
		try {
			Long count = logisticsMapper.selectCount(null);
			if (count != null && count > 0) {
				return;
			}
			List<JsonNode> rows = readJsonArray(KUAIDI_RESOURCE);
			int now = (int) (System.currentTimeMillis() / 1000L);
			int inserted = 0;
			for (JsonNode row : rows) {
				String corpCode = textOrEmpty(row, "corp_code");
				String corpName = textOrEmpty(row, "corp_name");
				if (corpCode.isEmpty() || corpName.isEmpty()) {
					continue;
				}
				Logistics entity = new Logistics();
				entity.setCorpCode(corpCode);
				entity.setKuaidiCode(textOrNull(row, "kuaidi_code"));
				entity.setFullName(corpName);
				entity.setCorpName(corpName);
				entity.setCustom(false);
				entity.setLogo(textOrNull(row, "logo"));
				entity.setPhone(textOrNull(row, "phone"));
				entity.setCreated(now);
				logisticsMapper.insert(entity);
				inserted++;
			}
			log.info("init logistics success, inserted={}", inserted);
		} catch (Exception e) {
			log.warn("init logistics failed, skip: {}", e.getMessage(), e);
		}
	}

	/**
	 * 系统菜单重导入：{@code common.use-system-menu=false} 时跳过；否则先删全部 {@code company_id=0}
	 * 菜单及类型关联，再按固定顺序逐文件调用 {@link ShopMenuUploadService#uploadMenus(List)}。失败向上抛出。
	 */
	void updateSystemMenus() {
		if (!useSystemMenu) {
			return;
		}
		// 删除 + 6 个文件导入必须原子化：任一文件失败时整体回滚，避免 company_id=0 菜单被破坏性半填充。
		// updateSystemMenus() 由本类 run(...) 自调用，代理式 @Transactional 会被绕过，故使用编程式事务。
		transactionTemplate.executeWithoutResult(status -> {
			LambdaQueryWrapper<ShopMenu> delMenu = new LambdaQueryWrapper<>();
			delMenu.eq(ShopMenu::getCompanyId, BORDER_COMPANY_ID);
			shopMenuMapper.delete(delMenu);

			LambdaQueryWrapper<ShopMenuRelType> delRel = new LambdaQueryWrapper<>();
			delRel.eq(ShopMenuRelType::getCompanyId, BORDER_COMPANY_ID);
			shopMenuRelTypeMapper.delete(delRel);

			for (String resource : MENU_RESOURCES) {
				List<JsonNode> rows = readJsonArray(resource);
				shopMenuUploadService.uploadMenus(rows);
			}
		});
		log.info("update shop menus success");
	}

	private List<JsonNode> readJsonArray(String location) {
		Resource resource = resourceLoader.getResource(location);
		try (InputStream in = resource.getInputStream()) {
			JsonNode root = objectMapper.readTree(in);
			List<JsonNode> rows = new ArrayList<>();
			if (root != null && root.isArray()) {
				for (JsonNode node : root) {
					rows.add(node);
				}
			}
			return rows;
		} catch (Exception e) {
			throw new IllegalStateException("读取种子数据失败: " + location, e);
		}
	}

	private static String textOrEmpty(JsonNode row, String field) {
		JsonNode n = row.get(field);
		if (n == null || n.isNull()) {
			return "";
		}
		return n.asText("");
	}

	private static String textOrNull(JsonNode row, String field) {
		JsonNode n = row.get(field);
		if (n == null || n.isNull()) {
			return null;
		}
		String s = n.asText(null);
		return StringUtils.hasText(s) ? s : null;
	}
}
