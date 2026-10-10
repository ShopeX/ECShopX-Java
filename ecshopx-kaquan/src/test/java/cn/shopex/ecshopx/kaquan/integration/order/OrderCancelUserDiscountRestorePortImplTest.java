package cn.shopex.ecshopx.kaquan.integration.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.mybatis.metadata.MpMetadataAnnotationHandler;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountLogs;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountLogsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCancelUserDiscountRestorePortImplTest {

	@Mock
	private UserDiscountMapper userDiscountMapper;

	@Mock
	private UserDiscountLogsMapper userDiscountLogsMapper;

	@InjectMocks
	private OrderCancelUserDiscountRestorePortImpl port;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		Configuration configuration = new MybatisConfiguration();
		GlobalConfigUtils.getGlobalConfig(configuration).setAnnotationHandler(new MpMetadataAnnotationHandler());
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserDiscount.class);
	}

	@Test
	@DisplayName("status=1 时不更新、不插入 callback 日志")
	void callbackUserCard_statusUnused_skipsWrite() {
		UserDiscount row = new UserDiscount();
		row.setId(8L);
		row.setStatus(1);
		row.setUserId(3L);
		row.setCompanyId(1L);
		when(userDiscountMapper.selectOne(any())).thenReturn(row);

		port.callbackUserCard("C1", 99L);

		verify(userDiscountMapper, never()).update(any(), any());
		verify(userDiscountLogsMapper, never()).insert(any(UserDiscountLogs.class));
	}

	@Test
	@DisplayName("status=2 时改回 1 并插入 callback")
	void callbackUserCard_statusUsed_setsUnusedAndLogs() {
		UserDiscount row = new UserDiscount();
		row.setId(8L);
		row.setStatus(2);
		row.setUserId(3L);
		row.setCompanyId(1L);
		row.setCardId(4L);
		row.setCardType("cash");
		when(userDiscountMapper.selectOne(any())).thenReturn(row);

		port.callbackUserCard("C1", 99L);

		verify(userDiscountMapper).update(isNull(), any());
		ArgumentCaptor<UserDiscountLogs> captor = ArgumentCaptor.forClass(UserDiscountLogs.class);
		verify(userDiscountLogsMapper).insert(captor.capture());
		assertThat(captor.getValue().getUsedStatus()).isEqualTo("callback");
		assertThat(captor.getValue().getUsedOrder()).isEqualTo("99");
	}
}
