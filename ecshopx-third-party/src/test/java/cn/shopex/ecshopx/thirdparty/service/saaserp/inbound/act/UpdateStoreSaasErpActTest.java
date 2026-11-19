package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpItemStoreSyncPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateStoreSaasErpActTest {

	private static final long COMPANY_ID = 7L;

	@Mock
	private SaasErpItemStoreSyncPort itemStoreSyncPort;

	private UpdateStoreSaasErpAct handler;

	@BeforeEach
	void setUp() {
		handler = new UpdateStoreSaasErpAct(itemStoreSyncPort, new ObjectMapper());
	}

	@Test
	void actIsUpdateStore() {
		assertEquals("update_store", handler.act());
	}

	@Test
	void missingDatasFailsWithoutSync() {
		SaasErpInboundResult result = handler.handle(Map.of(), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("缺少数据", result.msg());
		assertNull(result.info());
		verify(itemStoreSyncPort, never()).sync(eq(COMPANY_ID), anyList());
	}

	@Test
	void blankDatasFailsWithoutSync() {
		SaasErpInboundResult result = handler.handle(Map.of("datas", "   "), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("缺少数据", result.msg());
		verify(itemStoreSyncPort, never()).sync(eq(COMPANY_ID), anyList());
	}

	@Test
	void invalidJsonFailsWithoutSync() {
		SaasErpInboundResult result = handler.handle(Map.of("datas", "not-json"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("数据有误", result.msg());
		verify(itemStoreSyncPort, never()).sync(eq(COMPANY_ID), anyList());
	}

	@Test
	void emptyArrayFailsWithoutSync() {
		SaasErpInboundResult result = handler.handle(Map.of("datas", "[]"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("数据有误", result.msg());
		verify(itemStoreSyncPort, never()).sync(eq(COMPANY_ID), anyList());
	}

	@Test
	void stripslashesRecoveryParsesAndSyncs() {
		String escaped = "[{\\\"product_bn\\\":\\\"A\\\",\\\"product_store\\\":1}]";

		SaasErpInboundResult result = handler.handle(Map.of("datas", escaped), COMPANY_ID);

		assertEquals("true", result.status());
		assertEquals("操作成功", result.msg());
		assertNull(result.info());

		ArgumentCaptor<List<Map<String, Object>>> rowsCaptor = ArgumentCaptor.forClass(List.class);
		verify(itemStoreSyncPort).sync(eq(COMPANY_ID), rowsCaptor.capture());
		assertEquals("A", rowsCaptor.getValue().get(0).get("product_bn"));
		assertEquals(1, rowsCaptor.getValue().get(0).get("product_store"));
	}

	@Test
	void validJsonSyncsAndReturnsOkWithNullInfo() {
		String json = "[{\"product_bn\":\"B\",\"product_store\":2}]";

		SaasErpInboundResult result = handler.handle(Map.of("datas", json), COMPANY_ID);

		assertEquals("true", result.status());
		assertEquals("操作成功", result.msg());
		assertNull(result.info());
		verify(itemStoreSyncPort).sync(eq(COMPANY_ID), anyList());
	}

	@Test
	void productNotFoundFromSyncFails() {
		doThrow(new IllegalStateException("商品不存在"))
				.when(itemStoreSyncPort)
				.sync(eq(COMPANY_ID), anyList());

		SaasErpInboundResult result =
				handler.handle(Map.of("datas", "[{\"product_bn\":\"X\",\"product_store\":1}]"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("商品不存在", result.msg());
		assertNull(result.info());
	}

	@Test
	void otherSyncExceptionPropagates() {
		doThrow(new RuntimeException("boom"))
				.when(itemStoreSyncPort)
				.sync(eq(COMPANY_ID), anyList());

		assertThrows(
				RuntimeException.class,
				() -> handler.handle(Map.of("datas", "[{\"product_bn\":\"X\",\"product_store\":1}]"), COMPANY_ID));
	}

	@Test
	void illegalStateWithDifferentMessagePropagates() {
		doThrow(new IllegalStateException("other"))
				.when(itemStoreSyncPort)
				.sync(eq(COMPANY_ID), anyList());

		assertThrows(
				IllegalStateException.class,
				() -> handler.handle(Map.of("datas", "[{\"product_bn\":\"X\",\"product_store\":1}]"), COMPANY_ID));
	}
}
