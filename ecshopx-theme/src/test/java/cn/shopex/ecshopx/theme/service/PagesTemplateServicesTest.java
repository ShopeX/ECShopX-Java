package cn.shopex.ecshopx.theme.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagesTemplateServicesTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(new MybatisConfiguration(), ""),
				PagesTemplate.class);
	}

	@Mock
	private PagesTemplateMapper pagesTemplateMapper;

	@Mock
	private PlatformTransactionManager transactionManager;

	private PagesTemplateServices service;

	@BeforeEach
	void setUp() {
		when(transactionManager.getTransaction(any(TransactionDefinition.class)))
				.thenReturn(new SimpleTransactionStatus());
		service = new PagesTemplateServices(pagesTemplateMapper, transactionManager);
	}

	@Test
	@DisplayName("分析 §3 步骤1-2：构造 count 条件后 count<1 早退，不查页不写库")
	// 分支: §3 步骤1-2
	void scheduleEnableTemplate_countZero_skipsPageAndUpdates() {
		when(pagesTemplateMapper.selectCount(any())).thenReturn(0L);
		service.scheduleEnableTemplate();
		verify(pagesTemplateMapper).selectCount(any());
		verify(pagesTemplateMapper, never()).selectPage(any(), any());
		verify(pagesTemplateMapper, never()).update(any(), any());
		verify(transactionManager, never()).getTransaction(any());
	}

	@Test
	@DisplayName("分析 §3 步骤3：lists 空列表早退，不写库")
	// 分支: §3 步骤3
	void scheduleEnableTemplate_emptyRecords_skipsUpdates() {
		when(pagesTemplateMapper.selectCount(any())).thenReturn(1L);
		Page<PagesTemplate> emptyResult = new Page<>(1, 1000);
		emptyResult.setRecords(Collections.emptyList());
		when(pagesTemplateMapper.selectPage(any(Page.class), any())).thenReturn(emptyResult);
		service.scheduleEnableTemplate();
		verify(pagesTemplateMapper, never()).update(any(), any());
		verify(transactionManager, never()).getTransaction(any());
	}

	@Test
	@DisplayName("分析 §3 步骤4-A：timer_time > 当前时间则 continue")
	// 分支: §3 步骤4-A
	void scheduleEnableTemplate_timerNotDue_skipsRow() {
		when(pagesTemplateMapper.selectCount(any())).thenReturn(1L);
		PagesTemplate row = templateRow();
		row.setTimerTime(Integer.MAX_VALUE);
		Page<PagesTemplate> pg = new Page<>(1, 1000);
		pg.setRecords(List.of(row));
		when(pagesTemplateMapper.selectPage(any(Page.class), any())).thenReturn(pg);
		service.scheduleEnableTemplate();
		verify(pagesTemplateMapper, never()).update(any(), any());
		verify(transactionManager, never()).getTransaction(any());
	}

	@Test
	@DisplayName("分析 §3 步骤4-B/5-8/10：到时行 批量关旧启 + 单条开新、事务 commit、正常结束")
	// 分支: §3 步骤4-B、5-8、10
	void scheduleEnableTemplate_dueRow_runsBulkThenSingleInTxn() {
		when(pagesTemplateMapper.selectCount(any())).thenReturn(1L);
		PagesTemplate row = templateRow();
		row.setTimerTime(0);
		Page<PagesTemplate> pg = new Page<>(1, 1000);
		pg.setRecords(List.of(row));
		when(pagesTemplateMapper.selectPage(any(Page.class), any())).thenReturn(pg);
		when(pagesTemplateMapper.update(isNull(), any())).thenReturn(1);
		service.scheduleEnableTemplate();
		verify(pagesTemplateMapper, times(2)).update(isNull(), any());
		verify(transactionManager).getTransaction(any(TransactionDefinition.class));
		verify(transactionManager).commit(any());
		verify(transactionManager, never()).rollback(any());
	}

	@Test
	@DisplayName("分析 §3 步骤9：update 失败 catch 后 rollback 并重抛")
	// 分支: §3 步骤9
	void scheduleEnableTemplate_secondUpdateFails_triggersRollback() {
		when(pagesTemplateMapper.selectCount(any())).thenReturn(1L);
		PagesTemplate row = templateRow();
		row.setTimerTime(0);
		Page<PagesTemplate> pg = new Page<>(1, 1000);
		pg.setRecords(List.of(row));
		when(pagesTemplateMapper.selectPage(any(Page.class), any())).thenReturn(pg);
		when(pagesTemplateMapper.update(isNull(), any()))
				.thenReturn(1)
				.thenThrow(new RuntimeException("db"));
		assertThrows(RuntimeException.class, () -> service.scheduleEnableTemplate());
		verify(transactionManager).rollback(any());
	}

	@Test
	@DisplayName("分析 §8：timer_time 为 null 时行级比较与更新路径")
	// 分支: analysis §8（timer_time 为 null）
	void scheduleEnableTemplate_nullTimerTime_processesRow() {
		when(pagesTemplateMapper.selectCount(any())).thenReturn(1L);
		PagesTemplate row = templateRow();
		row.setTimerTime(null);
		Page<PagesTemplate> pg = new Page<>(1, 1000);
		pg.setRecords(List.of(row));
		when(pagesTemplateMapper.selectPage(any(Page.class), any())).thenReturn(pg);
		when(pagesTemplateMapper.update(isNull(), any())).thenReturn(1);
		service.scheduleEnableTemplate();
		verify(pagesTemplateMapper, times(2)).update(isNull(), any());
	}

	private static PagesTemplate templateRow() {
		PagesTemplate row = new PagesTemplate();
		row.setPagesTemplateId(100L);
		row.setCompanyId(1L);
		row.setRegionauthId(0L);
		row.setDistributorId(0);
		row.setWeappPages("index");
		row.setTimerStatus(1);
		return row;
	}
}
