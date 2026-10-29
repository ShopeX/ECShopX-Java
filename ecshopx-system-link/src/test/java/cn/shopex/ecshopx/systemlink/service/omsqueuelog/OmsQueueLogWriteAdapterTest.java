package cn.shopex.ecshopx.systemlink.service.omsqueuelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.systemlink.domain.OmsQueueLog;
import cn.shopex.ecshopx.systemlink.mapper.OmsQueueLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class OmsQueueLogWriteAdapterTest {

	private OmsQueueLogMapper mapper;
	private OmsQueueLogWriteAdapter adapter;

	@BeforeEach
	void setUp() {
		mapper = Mockito.mock(OmsQueueLogMapper.class);
		adapter = new OmsQueueLogWriteAdapter(mapper);
		when(mapper.insert(any(OmsQueueLog.class))).thenAnswer(invocation -> {
			OmsQueueLog log = invocation.getArgument(0);
			log.setId(15L);
			return 1;
		});
	}

	@Test
	void insertResponseFailPersistsExpectedFields() {
		long id = adapter.insertResponseFail(42L, "saas.erp.callback", "{\"act\":\"x\"}");

		assertEquals(15L, id);
		ArgumentCaptor<OmsQueueLog> captor = ArgumentCaptor.forClass(OmsQueueLog.class);
		verify(mapper).insert(captor.capture());
		OmsQueueLog saved = captor.getValue();
		assertEquals(42L, saved.getCompanyId());
		assertEquals("response", saved.getApiType());
		assertEquals("saas.erp.callback", saved.getWorker());
		assertEquals("{\"act\":\"x\"}", saved.getParams());
		assertEquals("fail", saved.getStatus());
		assertEquals("0", saved.getRuntime());
		assertEquals("[]", saved.getResult());
		assertEquals(null, saved.getMsgId());
	}

	@Test
	void updateResultCallsUpdateById() {
		adapter.updateResult(15L, "success", "{\"result\":\"success\"}");

		ArgumentCaptor<OmsQueueLog> captor = ArgumentCaptor.forClass(OmsQueueLog.class);
		verify(mapper).updateById(captor.capture());
		OmsQueueLog updated = captor.getValue();
		assertEquals(15L, updated.getId());
		assertEquals("success", updated.getStatus());
		assertEquals("{\"result\":\"success\"}", updated.getResult());
	}
}
