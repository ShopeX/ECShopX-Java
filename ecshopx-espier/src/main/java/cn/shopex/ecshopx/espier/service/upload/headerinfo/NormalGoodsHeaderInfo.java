/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.espier.service.upload.headerinfo;

import cn.shopex.ecshopx.espier.service.upload.UploadHeaderColumnInfo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NormalGoodsHeaderInfo {
	private NormalGoodsHeaderInfo() {}

	public static Map<String, UploadHeaderColumnInfo> map() {
		Map<String, UploadHeaderColumnInfo> map = new LinkedHashMap<>();
		map.put("管理分类", new UploadHeaderColumnInfo(255, "类目名称，一级类目->二级类目->三级类目", true));
		map.put("商品名称", new UploadHeaderColumnInfo(255, "", true));
		map.put("SPU编码", new UploadHeaderColumnInfo(32, "平台唯一自动生成", false));
		map.put("SKU编码", new UploadHeaderColumnInfo(32, "平台唯一自动生成\t", false));
		map.put("简介", new UploadHeaderColumnInfo(20, "", false));
		map.put("销售价", new UploadHeaderColumnInfo(255, "单位为(元)，最多两位小数", true));
		map.put("市场价", new UploadHeaderColumnInfo(255, "单位为(元)，最多两位小数", false));
		map.put("成本价", new UploadHeaderColumnInfo(255, "单位为(元)，最多两位小数", false));
		map.put("会员价", new UploadHeaderColumnInfo(255, "单位为(元)，最多两位小数", false));
		map.put("起订量", new UploadHeaderColumnInfo(255, "起订量", false));
		map.put("审核状态", new UploadHeaderColumnInfo(20, "待提交、待审核、已通过、已拒绝", false));
		map.put("库存", new UploadHeaderColumnInfo(255, "库存为0-999999999的整数", true));
		map.put("头图", new UploadHeaderColumnInfo(255, "多个图片使用英文逗号隔开，最多上传9个", false));
		map.put("详情", new UploadHeaderColumnInfo(255, "多个图片使用英文逗号隔开", false));
		map.put("规格图", new UploadHeaderColumnInfo(255, "多个图片使用英文逗号隔开，最多上传5个", false));
		map.put("视频", new UploadHeaderColumnInfo(255, "在视频素材复制对应的ID", false));
		map.put("品牌", new UploadHeaderColumnInfo(255, "已有的品牌名称", false));
		map.put("运费模板", new UploadHeaderColumnInfo(255, "运费模板名称", true));
		map.put("销售分类", new UploadHeaderColumnInfo(255, "分类名称，一级分类->二级分类|一级分类->二级分类>三级分类 多个二级三级分类使用|隔开", true));
		map.put("重量", new UploadHeaderColumnInfo(255, "商品重量，单位KG", false));
		map.put("条形码", new UploadHeaderColumnInfo(255, "条形码", false));
		map.put("单位", new UploadHeaderColumnInfo(255, "单位", false));
		map.put("规格值", new UploadHeaderColumnInfo(255, "例如：颜色:红色|尺码:20cm，必须和管理分类一起导入", false));
		map.put("参数值", new UploadHeaderColumnInfo(255, "例如：系列:生机展颜|功效:美白提亮", false));
		map.put("发货时间", new UploadHeaderColumnInfo(255, "发货时间按天计算", true));
		map.put("是否支持分润", new UploadHeaderColumnInfo(255, "是否支持: 0不支持分润 1支持分润", false));
		map.put("分润类型", new UploadHeaderColumnInfo(255, "分润类型:0,1或2, 0默认分润 1固定比例分润 2固定金额分润", false));
		map.put("拉新分润", new UploadHeaderColumnInfo(255, "1:按照比例分润 1-100, 2:按照固定金额分润(元)，最多两位小数", false));
		map.put("推广分润", new UploadHeaderColumnInfo(255, "1:按照比例分润 1-100, 2:按照固定金额分润(元)，最多两位小数", false));
		map.put("商品状态", new UploadHeaderColumnInfo(30, "前台可销售，前端不展示，不可销售, 前台仅展示", false));
		return Collections.unmodifiableMap(map);
	}
}
