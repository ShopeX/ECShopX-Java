package cn.shopex.ecshopx.companys.service.operatorcart;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.domain.OperatorCart;
import cn.shopex.ecshopx.companys.mapper.OperatorCartMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorCartCleanupServiceTest {

	@Mock
	private OperatorCartMapper operatorCartMapper;

	@InjectMocks
	private OperatorCartCleanupService service;

	@Test
	void deleteByCompanyIdAndItemIds_deletesMatchingRows() {
		when(operatorCartMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);

		service.deleteByCompanyIdAndItemIds(38L, List.of(100L, 101L));

		verify(operatorCartMapper).delete(any(LambdaQueryWrapper.class));
	}

	@Test
	void deleteAllRows_usesCartIds() {
		OperatorCart row = new OperatorCart();
		row.setCartId(7L);
		when(operatorCartMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

		service.deleteAllRows(List.of(row));

		verify(operatorCartMapper).delete(any(LambdaQueryWrapper.class));
	}
}
