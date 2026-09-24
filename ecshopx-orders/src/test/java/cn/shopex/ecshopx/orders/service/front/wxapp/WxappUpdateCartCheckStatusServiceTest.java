package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 全选/全不选一次提交多个 cart_id，必须一次 UPDATE 覆盖全部行；曾因只取单个 cart_id 触发 422「购物车参数错误」。
 */
@ExtendWith(MockitoExtension.class)
class WxappUpdateCartCheckStatusServiceTest {

	@Mock
	private CartMapper cartMapper;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Cart.class);
	}

	@Test
	@DisplayName("批量 cart_id：一条 UPDATE 带 IN，全部行一起改")
	void batchUpdatesAllCartIdsInOneStatement() {
		when(cartMapper.update(any(), any())).thenReturn(6);
		int rows = new WxappUpdateCartCheckStatusService(cartMapper)
				.updateCartCheckStatus(38L, 7L, List.of(3202L, 3201L, 3200L, 3195L, 3186L, 3173L), true);

		assertThat(rows).isEqualTo(6);
		ArgumentCaptor<LambdaUpdateWrapper<Cart>> cap = wrapperCaptor();
		verify(cartMapper).update(any(), cap.capture());
		String sql = cap.getValue().getSqlSet();
		assertThat(sql).contains("is_checked");
		assertThat(cap.getValue().getTargetSql()).contains("cart_id IN");
	}

	@Test
	@DisplayName("单值重载仍走同一路径")
	void singleIdDelegatesToBatch() {
		when(cartMapper.update(any(), any())).thenReturn(1);
		assertThat(new WxappUpdateCartCheckStatusService(cartMapper)
				.updateCartCheckStatus(38L, 7L, 3202L, false)).isEqualTo(1);
	}

	@Test
	@DisplayName("空集合不触发 UPDATE")
	void emptyListSkipsUpdate() {
		assertThat(new WxappUpdateCartCheckStatusService(cartMapper)
				.updateCartCheckStatus(38L, 7L, List.of(), true)).isZero();
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<LambdaUpdateWrapper<Cart>> wrapperCaptor() {
		return ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
	}
}
