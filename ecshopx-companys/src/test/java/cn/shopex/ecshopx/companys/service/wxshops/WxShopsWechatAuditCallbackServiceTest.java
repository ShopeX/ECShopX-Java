package cn.shopex.ecshopx.companys.service.wxshops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxShopsWechatAuditCallbackServiceTest {

	@Mock
	private WxShopsMapper wxShopsMapper;

	private WxShopsWechatAuditCallbackService service;

	@BeforeEach
	void setUp() {
		service = new WxShopsWechatAuditCallbackService(wxShopsMapper);
	}

	@Test
	void whenNoShopRowForAuditId_thenNoUpdateAndReturnsTrue() {
		when(wxShopsMapper.selectOne(any())).thenReturn(null);
		Map<String, Object> data = new HashMap<>();
		data.put("audit_id", "missing");
		data.put("status", 2);
		data.put("errmsg", "x");
		assertTrue(service.applyWxShopsAddEvent(data));
		verify(wxShopsMapper, never()).updateById(any(WxShops.class));
	}

	@Test
	void whenUpdateEventNoShopRow_thenNoUpdateAndReturnsTrue() {
		when(wxShopsMapper.selectOne(any())).thenReturn(null);
		Map<String, Object> data = new HashMap<>();
		data.put("audit_id", "missing-up");
		data.put("status", 2);
		data.put("errmsg", "x");
		assertTrue(service.applyWxShopsUpdateEvent(data));
		verify(wxShopsMapper, never()).updateById(any(WxShops.class));
	}

	@Test
	void whenUpdateEventShopRowExists_thenUpdatesStatusAndErrmsgSameAsAdd() {
		WxShops row = new WxShops();
		row.setWxShopId(200L);
		row.setAuditId("aud-up");
		when(wxShopsMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> data = new HashMap<>();
		data.put("audit_id", "aud-up");
		data.put("status", "5");
		data.put("errmsg", "update msg");

		assertTrue(service.applyWxShopsUpdateEvent(data));

		ArgumentCaptor<WxShops> cap = ArgumentCaptor.forClass(WxShops.class);
		verify(wxShopsMapper).updateById(cap.capture());
		assertEquals(5, cap.getValue().getStatus());
		assertEquals("update msg", cap.getValue().getErrmsg());
	}

	@Test
	void whenShopRowExists_thenUpdatesStatusAndErrmsg() {
		WxShops row = new WxShops();
		row.setWxShopId(100L);
		row.setAuditId("aud1");
		when(wxShopsMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> data = new HashMap<>();
		data.put("audit_id", "aud1");
		data.put("status", "3");
		data.put("errmsg", "failed reason");

		assertTrue(service.applyWxShopsAddEvent(data));

		ArgumentCaptor<WxShops> cap = ArgumentCaptor.forClass(WxShops.class);
		verify(wxShopsMapper).updateById(cap.capture());
		assertEquals(3, cap.getValue().getStatus());
		assertEquals("failed reason", cap.getValue().getErrmsg());
	}
}
