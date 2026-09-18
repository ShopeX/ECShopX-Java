package cn.shopex.ecshopx.employeepurchase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityGoodsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.espier.service.upload.UploadHeaderTitle;
import cn.shopex.ecshopx.espier.service.upload.headerinfo.EmployeePurchaseActivityItemsHeaderInfo;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeePurchaseActivityItemsImportRowServiceTest {

	@Mock
	private ActivitiesMapper activitiesMapper;
	@Mock
	private ItemsMapper itemsMapper;
	@Mock
	private ActivityItemsMapper activityItemsMapper;
	@Mock
	private ActivityGoodsMapper activityGoodsMapper;
	@Mock
	private DistributorItemsRepository distributorItemsRepository;
	@Mock
	private EmployeePurchaseActivityItemsCategoryRedisService categoryRedisService;

	private EmployeePurchaseActivityItemsImportRowService service;

	@BeforeAll
	static void initMybatisTableInfo() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), ActivityItems.class);
	}

	@BeforeEach
	void setUp() {
		service = new EmployeePurchaseActivityItemsImportRowService(
				activitiesMapper,
				itemsMapper,
				activityItemsMapper,
				activityGoodsMapper,
				distributorItemsRepository,
				categoryRedisService);
	}

	@Test
	void header_whenShareStore_omitsActivityStore() {
		UploadHeaderTitle title = EmployeePurchaseActivityItemsHeaderInfo.title(true);
		assertThat(title.all().keySet())
				.containsExactly("SKU编码", "活动价格", "限购数量", "限购金额", "状态");
		assertThat(title.isNeed().keySet()).containsExactly("SKU编码", "活动价格");
		assertThat(title.all()).doesNotContainKey("活动库存");
	}

	@Test
	void header_whenNotShareStore_includesRequiredActivityStore() {
		UploadHeaderTitle title = EmployeePurchaseActivityItemsHeaderInfo.title(false);
		assertThat(title.all().keySet())
				.containsExactly("SKU编码", "活动价格", "活动库存", "限购数量", "限购金额", "状态");
		assertThat(title.isNeed().keySet()).containsExactly("SKU编码", "活动价格", "活动库存");
	}

	@Test
	void acceptRow_shareStore_insertsWithoutActivityStoreColumn() {
		Activities activity = activity(10L, true);
		Items item = platformItem(100L, 200L, "SKU-1");
		when(activitiesMapper.selectOne(any(Wrapper.class))).thenReturn(activity);
		when(itemsMapper.selectOne(any(Wrapper.class))).thenReturn(item);
		when(activityItemsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		when(activityGoodsMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		Map<String, Object> row = baseRow("SKU-1", "12.5", null);
		row.put("relation_id", 10L);
		row.put("shelf_status", "");
		service.acceptRow(1L, 0L, row);

		ArgumentCaptor<ActivityItems> captor = ArgumentCaptor.forClass(ActivityItems.class);
		verify(activityItemsMapper).insert(captor.capture());
		ActivityItems saved = captor.getValue();
		assertThat(saved.getActivityPrice()).isEqualTo(1250);
		assertThat(saved.getActivityStore()).isEqualTo(0);
		assertThat(saved.getShelfStatus()).isEqualTo(1);
		assertThat(saved.getLimitFee()).isEqualTo(0);
		verify(categoryRedisService).store(1L, 10L, 0L);
	}

	@Test
	void acceptRow_notShareStore_requiresActivityStore() {
		Activities activity = activity(10L, false);
		Items item = platformItem(100L, 200L, "SKU-1");
		when(activitiesMapper.selectOne(any(Wrapper.class))).thenReturn(activity);
		when(itemsMapper.selectOne(any(Wrapper.class))).thenReturn(item);

		Map<String, Object> row = baseRow("SKU-1", "10", null);
		row.put("relation_id", 10L);
		assertThatThrownBy(() -> service.acceptRow(1L, 0L, row))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("活动库存不能为空");
	}

	@Test
	void acceptRow_notShareStore_persistsStoreAndShelf() {
		Activities activity = activity(10L, false);
		Items item = platformItem(100L, 200L, "SKU-1");
		when(activitiesMapper.selectOne(any(Wrapper.class))).thenReturn(activity);
		when(itemsMapper.selectOne(any(Wrapper.class))).thenReturn(item);
		when(activityItemsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		when(activityGoodsMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		Map<String, Object> row = baseRow("SKU-1", "10", "5");
		row.put("relation_id", 10L);
		row.put("limit_fee", "20");
		row.put("limit_num", "3");
		row.put("shelf_status", "0");
		service.acceptRow(1L, 0L, row);

		ArgumentCaptor<ActivityItems> captor = ArgumentCaptor.forClass(ActivityItems.class);
		verify(activityItemsMapper).insert(captor.capture());
		assertThat(captor.getValue().getActivityStore()).isEqualTo(5);
		assertThat(captor.getValue().getLimitFee()).isEqualTo(2000);
		assertThat(captor.getValue().getLimitNum()).isEqualTo(3);
		assertThat(captor.getValue().getShelfStatus()).isEqualTo(0);
	}

	@Test
	void acceptRow_shareStore_updateDoesNotTouchActivityStore() {
		Activities activity = activity(10L, true);
		Items item = platformItem(100L, 200L, "SKU-1");
		ActivityItems existing = new ActivityItems();
		existing.setItemId(100L);
		when(activitiesMapper.selectOne(any(Wrapper.class))).thenReturn(activity);
		when(itemsMapper.selectOne(any(Wrapper.class))).thenReturn(item);
		when(activityItemsMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
		when(activityGoodsMapper.selectOne(any(Wrapper.class))).thenReturn(new cn.shopex.ecshopx.employeepurchase.domain.ActivityGoods());

		Map<String, Object> row = baseRow("SKU-1", "9", "999");
		row.put("relation_id", 10L);
		service.acceptRow(1L, 0L, row);

		verify(activityItemsMapper).update(eq(null), any());
		verify(activityItemsMapper, never()).insert(any(ActivityItems.class));
	}

	@Test
	void acceptRow_invalidShelfStatus_rejected() {
		Activities activity = activity(10L, true);
		Items item = platformItem(100L, 200L, "SKU-1");
		when(activitiesMapper.selectOne(any(Wrapper.class))).thenReturn(activity);
		when(itemsMapper.selectOne(any(Wrapper.class))).thenReturn(item);

		Map<String, Object> row = baseRow("SKU-1", "10", null);
		row.put("relation_id", 10L);
		row.put("shelf_status", "2");
		assertThatThrownBy(() -> service.acceptRow(1L, 0L, row))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("状态只能填写0或1");
	}

	@Test
	void acceptRow_shopItem_requiresDistributorSale() {
		Activities activity = activity(10L, true);
		Items item = platformItem(100L, 200L, "SKU-1");
		when(activitiesMapper.selectOne(any(Wrapper.class))).thenReturn(activity);
		when(itemsMapper.selectOne(any(Wrapper.class))).thenReturn(item);
		when(distributorItemsRepository.findByDistributorIdAndCompanyIdAndItemId(88L, 1L, 100L))
				.thenReturn(Optional.empty());

		Map<String, Object> row = baseRow("SKU-1", "10", null);
		row.put("relation_id", 10L);
		row.put("distributor_id", 88L);
		assertThatThrownBy(() -> service.acceptRow(1L, 88L, row))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("店铺商品不存在:SKU-1");
	}

	private static Map<String, Object> baseRow(String bn, String price, String store) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_bn", bn);
		row.put("activity_price", price);
		if (store != null) {
			row.put("activity_store", store);
		}
		return row;
	}

	private static Activities activity(long id, boolean share) {
		Activities a = new Activities();
		a.setId(id);
		a.setCompanyId(1L);
		a.setIfShareStore(share);
		return a;
	}

	private static Items platformItem(long itemId, long goodsId, String bn) {
		Items item = new Items();
		item.setItemId(itemId);
		item.setGoodsId(goodsId);
		item.setItemBn(bn);
		item.setCompanyId(1L);
		item.setDistributorId(0);
		return item;
	}
}
