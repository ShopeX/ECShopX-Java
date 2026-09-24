package cn.shopex.ecshopx.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.shopmenuborder.service.ShopMenuUploadService;
import cn.shopex.ecshopx.superadmin.domain.Logistics;
import cn.shopex.ecshopx.superadmin.mapper.LogisticsMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuRelTypeMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("MigrationSeedRunner: 物流空表导入 + 菜单开关与顺序")
class MigrationSeedRunnerTest {

	/** 同步内联执行回调的事务模板，便于单测校验删除/上传的调用顺序与次数。 */
	private static TransactionTemplate syncTransactionTemplate() {
		return new TransactionTemplate() {
			@Override
			public <T> T execute(TransactionCallback<T> action) {
				return action.doInTransaction(null);
			}
		};
	}

	private MigrationSeedRunner newRunner(
			LogisticsMapper logisticsMapper,
			ShopMenuMapper shopMenuMapper,
			ShopMenuRelTypeMapper shopMenuRelTypeMapper,
			ShopMenuUploadService uploadService,
			boolean useSystemMenu) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		MigrationSeedRunner runner =
				new MigrationSeedRunner(
						logisticsMapper,
						shopMenuMapper,
						shopMenuRelTypeMapper,
						uploadService,
						new ObjectMapper(),
						resourceLoader,
						syncTransactionTemplate());
		ReflectionTestUtils.setField(runner, "useSystemMenu", useSystemMenu);
		return runner;
	}

	@Test
	void initLogistics_emptyTable_insertsRowsAndSkipsBlank() {
		LogisticsMapper logisticsMapper = mock(LogisticsMapper.class);
		when(logisticsMapper.selectCount(any())).thenReturn(0L);

		MigrationSeedRunner runner =
				newRunner(
						logisticsMapper,
						mock(ShopMenuMapper.class),
						mock(ShopMenuRelTypeMapper.class),
						mock(ShopMenuUploadService.class),
						true);

		runner.initLogistics();

		// kuaidi.json 中 corp_code/corp_name 均非空的行才写入
		ArgumentCaptor<Logistics> captor = ArgumentCaptor.forClass(Logistics.class);
		verify(logisticsMapper, org.mockito.Mockito.atLeastOnce()).insert(captor.capture());
		for (Logistics l : captor.getAllValues()) {
			org.junit.jupiter.api.Assertions.assertNotNull(l.getCorpCode());
			org.junit.jupiter.api.Assertions.assertNotNull(l.getCorpName());
			org.junit.jupiter.api.Assertions.assertFalse(
					l.getCorpCode().isEmpty(), "corp_code 不应为空");
			org.junit.jupiter.api.Assertions.assertFalse(
					l.getCorpName().isEmpty(), "corp_name 不应为空");
			// fullName 对齐 corp_name
			org.junit.jupiter.api.Assertions.assertEquals(l.getCorpName(), l.getFullName());
			org.junit.jupiter.api.Assertions.assertNotNull(l.getCreated());
		}
	}

	@Test
	void initLogistics_skipsRowsWithBlankCorpCodeOrName() {
		LogisticsMapper logisticsMapper = mock(LogisticsMapper.class);
		when(logisticsMapper.selectCount(any())).thenReturn(0L);

		// 内存 JSON：1 条有效 + corp_code 空 + corp_name 空，只有有效行应写入
		String json =
				"["
						+ "{\"corp_code\":\"SF\",\"corp_name\":\"顺丰速运\",\"kuaidi_code\":\"shunfeng\","
						+ "\"logo\":\"sf.png\",\"phone\":\"95338\"},"
						+ "{\"corp_code\":\"\",\"corp_name\":\"缺编码\"},"
						+ "{\"corp_code\":\"ZTO\",\"corp_name\":\"\"}"
						+ "]";
		ResourceLoader resourceLoader =
				new ResourceLoader() {
					@Override
					public Resource getResource(String location) {
						return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
					}

					@Override
					public ClassLoader getClassLoader() {
						return getClass().getClassLoader();
					}
				};

		MigrationSeedRunner runner =
				new MigrationSeedRunner(
						logisticsMapper,
						mock(ShopMenuMapper.class),
						mock(ShopMenuRelTypeMapper.class),
						mock(ShopMenuUploadService.class),
						new ObjectMapper(),
						resourceLoader,
						syncTransactionTemplate());
		ReflectionTestUtils.setField(runner, "useSystemMenu", true);

		runner.initLogistics();

		// 仅有效行写入一次
		ArgumentCaptor<Logistics> captor = ArgumentCaptor.forClass(Logistics.class);
		verify(logisticsMapper, times(1)).insert(captor.capture());
		Logistics inserted = captor.getValue();
		Assertions.assertEquals("SF", inserted.getCorpCode());
		Assertions.assertEquals("顺丰速运", inserted.getCorpName());
		Assertions.assertEquals("顺丰速运", inserted.getFullName());
		Assertions.assertEquals("shunfeng", inserted.getKuaidiCode());
		Assertions.assertNotNull(inserted.getCreated());
	}

	@Test
	void initLogistics_nonEmptyTable_doesNothing() {
		LogisticsMapper logisticsMapper = mock(LogisticsMapper.class);
		when(logisticsMapper.selectCount(any())).thenReturn(5L);

		MigrationSeedRunner runner =
				newRunner(
						logisticsMapper,
						mock(ShopMenuMapper.class),
						mock(ShopMenuRelTypeMapper.class),
						mock(ShopMenuUploadService.class),
						true);

		runner.initLogistics();

		verify(logisticsMapper, never()).insert(any(Logistics.class));
	}

	@Test
	void updateSystemMenus_toggleOff_doesNothing() {
		ShopMenuMapper shopMenuMapper = mock(ShopMenuMapper.class);
		ShopMenuRelTypeMapper relMapper = mock(ShopMenuRelTypeMapper.class);
		ShopMenuUploadService uploadService = mock(ShopMenuUploadService.class);

		MigrationSeedRunner runner =
				newRunner(
						mock(LogisticsMapper.class),
						shopMenuMapper,
						relMapper,
						uploadService,
						false);

		runner.updateSystemMenus();

		verify(shopMenuMapper, never()).delete(any());
		verify(relMapper, never()).delete(any());
		verify(uploadService, never()).uploadMenus(anyList());
	}

	@Test
	void updateSystemMenus_toggleOn_deletesThenUploadsSixFilesInOrder() {
		ShopMenuMapper shopMenuMapper = mock(ShopMenuMapper.class);
		ShopMenuRelTypeMapper relMapper = mock(ShopMenuRelTypeMapper.class);
		ShopMenuUploadService uploadService = mock(ShopMenuUploadService.class);

		MigrationSeedRunner runner =
				newRunner(
						mock(LogisticsMapper.class),
						shopMenuMapper,
						relMapper,
						uploadService,
						true);

		runner.updateSystemMenus();

		// 先删两张表，再导入 6 个文件
		InOrder order = inOrder(shopMenuMapper, relMapper, uploadService);
		order.verify(shopMenuMapper).delete(any());
		order.verify(relMapper).delete(any());
		order.verify(uploadService, times(6)).uploadMenus(anyList());
	}
}
