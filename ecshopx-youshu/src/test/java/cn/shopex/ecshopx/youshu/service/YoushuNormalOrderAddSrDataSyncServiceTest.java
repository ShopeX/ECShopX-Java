package cn.shopex.ecshopx.youshu.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.youshu.YoushuDataSourceApiPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.youshu.domain.YoushuSetting;
import cn.shopex.ecshopx.youshu.integration.YoushuOrderPushPort;
import cn.shopex.ecshopx.youshu.mapper.YoushuSettingMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YoushuNormalOrderAddSrDataSyncServiceTest {

	@Mock
	private YoushuSettingMapper youshuSettingMapper;

	@Mock
	private YoushuDataSourceApiPort youshuDataSourceApiPort;

	@Mock
	private YoushuOrderPushPort youshuOrderPushPort;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@InjectMocks
	private YoushuNormalOrderAddSrDataSyncService service;

	@Test
	void whenNoSetting_thenNoPush() {
		when(youshuSettingMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		service.syncOrderAfterNormalAdd(1L, 10L);

		verify(youshuDataSourceApiPort, never()).getOrCreateDataSourceId(anyString(), anyInt(), any());
		verify(youshuOrderPushPort, never()).pushOrder(anyString(), any(), any());
	}

	@Test
	void whenSetting_thenResolvesDataSourceAndPushes() {
		YoushuSetting setting = new YoushuSetting();
		setting.setCompanyId(1L);
		setting.setMerchantId("m1");
		setting.setApiUrl("https://api.example");
		setting.setAppId("aid");
		setting.setAppSecret("sec");

		when(youshuSettingMapper.selectOne(any(Wrapper.class))).thenReturn(setting);

		NormalOrders order = new NormalOrders();
		order.setOrderId(10L);
		order.setCompanyId(1L);
		order.setTotalFee("100");
		order.setTitle("t");
		order.setPayType("wxpay");
		when(normalOrdersMapper.selectOne(any(Wrapper.class))).thenReturn(order);

		when(youshuDataSourceApiPort.getOrCreateDataSourceId(eq("m1"), eq(0), any(YoushuOpenApiCredentials.class)))
				.thenReturn("ds-1");

		service.syncOrderAfterNormalAdd(1L, "10");

		verify(youshuOrderPushPort)
				.pushOrder(
						eq("ds-1"),
						argThat(
								m ->
										Long.valueOf(10L).equals(m.get("order_id"))
												&& Long.valueOf(1L).equals(m.get("company_id"))),
						any(YoushuOpenApiCredentials.class));
	}
}
