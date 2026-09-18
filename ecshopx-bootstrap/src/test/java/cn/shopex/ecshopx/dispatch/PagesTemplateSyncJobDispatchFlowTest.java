package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.ThemeDispatchJobNames;
import cn.shopex.ecshopx.distribution.service.PagesTemplateSyncDistributorQueryService;
import cn.shopex.ecshopx.theme.dispatch.PagesTemplateSyncJobHandler;
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateSetMapper;
import cn.shopex.ecshopx.theme.service.PagesTemplateStoreSyncApplyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PagesTemplateSyncJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesSyncApplyOrEquivalent() throws Exception {
		PagesTemplateMapper pagesTemplateMapper = mock(PagesTemplateMapper.class);
		PagesTemplateSetMapper pagesTemplateSetMapper = mock(PagesTemplateSetMapper.class);
		PagesTemplateSyncDistributorQueryService distributorQueryService =
				mock(PagesTemplateSyncDistributorQueryService.class);
		PagesTemplateStoreSyncApplyService applyService = mock(PagesTemplateStoreSyncApplyService.class);

		PagesTemplate hq = new PagesTemplate();
		hq.setCompanyId(9L);
		hq.setPagesTemplateId(100L);
		hq.setTemplateType(0);
		hq.setTemplateTitle("t");
		hq.setTemplatePic("p");
		hq.setElementEditStatus(2);
		hq.setTemplateName("n");
		hq.setRegionauthId(0L);
		when(pagesTemplateMapper.selectOne(any())).thenReturn(hq);
		when(pagesTemplateSetMapper.selectOne(any())).thenReturn(null);

		PagesTemplateSyncJobHandler handler =
				new PagesTemplateSyncJobHandler(
						pagesTemplateMapper,
						pagesTemplateSetMapper,
						distributorQueryService,
						applyService,
						new ObjectMapper());

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(ThemeDispatchJobNames.PAGES_TEMPLATE_SYNC_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("pages_template_id", 100L);
		payload.put("is_all_distributor", 2);
		payload.put("distributor_ids", "[]");
		payload.put("locale_tag", "zh-CN");
		facade.dispatchJob(
				ThemeDispatchJobNames.PAGES_TEMPLATE_SYNC_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(ThemeDispatchJobNames.PAGES_TEMPLATE_SYNC_JOB, msg.messageName());
		assertEquals(9L, msg.payload().get("company_id"));
		assertEquals(100L, msg.payload().get("pages_template_id"));
		assertEquals(2, msg.payload().get("is_all_distributor"));
		assertEquals("zh-CN", msg.payload().get("locale_tag"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(applyService, never())
				.applyOneDistributor(
						anyLong(),
						anyLong(),
						anyInt(),
						anyString(),
						anyString(),
						anyString(),
						any(),
						anyLong(),
						anyString(),
						anyString());
		verify(distributorQueryService, never()).listDistributorIds(anyLong(), anyLong());
	}
}
