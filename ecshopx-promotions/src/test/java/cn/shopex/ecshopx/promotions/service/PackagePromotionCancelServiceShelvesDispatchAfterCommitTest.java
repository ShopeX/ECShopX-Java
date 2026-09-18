package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.PackageItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.mapper.PackageItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class PackagePromotionCancelServiceShelvesDispatchAfterCommitTest {

	private static final long COMPANY_ID = 1L;
	private static final long PACKAGE_ID = 50L;
	private static final long ITEM_ID = 200L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PackagePromotions.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PackageItemPromotions.class);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void cancel_whenTransactionCommits_invokesSalespersonItemsShelvesJobPublishOnceAfterCommit() {
		MessageSource messageSource = mock(MessageSource.class);
		when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("err");

		PackagePromotions existing = new PackagePromotions();
		existing.setPackageId(PACKAGE_ID);
		existing.setCompanyId(COMPANY_ID);

		PackagePromotionsMapper packagePromotionsMapper = mock(PackagePromotionsMapper.class);
		when(packagePromotionsMapper.selectOne(any())).thenReturn(existing);
		when(packagePromotionsMapper.update(any(), any())).thenReturn(1);

		PackageItemPromotions itemRow = new PackageItemPromotions();
		itemRow.setPackageId(PACKAGE_ID);
		itemRow.setCompanyId(COMPANY_ID);
		itemRow.setItemId(ITEM_ID);

		PackageItemPromotionsMapper packageItemPromotionsMapper = mock(PackageItemPromotionsMapper.class);
		when(packageItemPromotionsMapper.selectList(any())).thenReturn(List.of(itemRow));
		when(packageItemPromotionsMapper.update(any(), any())).thenReturn(1);

		PackagePromotions fresh = new PackagePromotions();
		fresh.setPackageId(PACKAGE_ID);
		fresh.setCompanyId(COMPANY_ID);
		fresh.setValidGrade("");
		when(packagePromotionsMapper.selectById(PACKAGE_ID)).thenReturn(fresh);

		SalespersonItemsShelvesJobDispatchPublisher shelvesPublisher =
				mock(SalespersonItemsShelvesJobDispatchPublisher.class);

		PackagePromotionCancelService svc =
				new PackagePromotionCancelService(
						packagePromotionsMapper, packageItemPromotionsMapper, shelvesPublisher, messageSource);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any(TransactionStatus.class));

		TransactionTemplate tt = new TransactionTemplate(txMgr);

		tt.executeWithoutResult(
				st -> {
					Map<String, Object> row = svc.cancel(PACKAGE_ID, COMPANY_ID);
					assertNotNull(row);
					verifyNoInteractions(shelvesPublisher);
				});

		verify(shelvesPublisher).publish(eq(COMPANY_ID), eq(PACKAGE_ID), eq("package"));
		verifyNoMoreInteractions(shelvesPublisher);
	}
}
