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

package cn.shopex.ecshopx.common.dispatch;

public final class EspierDispatchJobNames {

	public static final String EXPORT_FILE_JOB = "job:10:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_REGISTRATION_RECORD = "job:14:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_EPIDEMIC_REGISTER = "job:81:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_EXPORT_ITEMS_DATA = "job:85:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_EXPORT_ITEMS_TAG_DATA = "job:86:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_EXPORT_ITEMS_CODE_DATA = "job:87:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_MEMBER_POINT_LOGS = "job:91:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_ADMIN_MEMBER_EXPORT = "job:190:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_POPULARIZE_PROMOTER_LIST =
			"job:212:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_POPULARIZE_ORDER =
			"job:213:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_POPULARIZE_STATIC =
			"job:214:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_BSPAY_TRADE_DATA = "job:94:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_BSPAY_WITHDRAW_DATA = "job:95:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_ADAPAY_TRADE_DATA = "job:110:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_OFFLINE_PAYMENT = "job:130:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_ORDER_LIST = "job:131:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_STATEMENTS_SUMMARIZED = "job:132:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_STATEMENTS_DETAIL = "job:133:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_LUCKDRAW_LOG = "job:159:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_DELIVERY_STAFF_DATA = "job:165:EspierBundle\\Jobs\\ExportFileJob";

	public static final String EXPORT_FILE_JOB_CHINAUMS_DIVISION = "job:166:EspierBundle\\Jobs\\ExportFileJob";

	public static final String UPLOAD_FILE_JOB = "job:168:EspierBundle\\Jobs\\UploadFileJob";

	public static final String IMPORT_DATA_JOB = "job:169:EspierBundle\\Jobs\\ImportDataJob";

	private EspierDispatchJobNames() {
	}
}
