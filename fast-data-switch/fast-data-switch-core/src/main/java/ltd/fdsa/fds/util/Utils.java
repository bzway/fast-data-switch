package ltd.fdsa.fds.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Throwables;

public class Utils {

	/**
	 * 线程休眠
	 *
	 * @param millis
	 */
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}

	public static List<String> getColumns(String[] columns, String[] excludeColumns) {
		if (excludeColumns == null || excludeColumns.length < 1) {
			return columns == null ? null : Arrays.asList(columns);
		}

		List<String> list = new ArrayList<String>();
		for (String column : columns) {
			if (!ArrayUtils.contains(excludeColumns, column)) {
				list.add(column);
			}
		}
		return list;
	}

	public static List<String> getColumns(List<String> columns, String[] excludeColumns) {
		return getColumns(columns.toArray(new String[columns.size()]), excludeColumns);
	}

	/**
	 * 解析分区值
	 *
	 * @param partitions
	 * @return
	 */
	public static List<String> parsePartitionValue(String partitions) {
		List<String> partitionValues = new ArrayList<String>();
		String[] partitionKeyValue = partitions.split("\\s*,\\s*");
		for (String kv : partitionKeyValue) {
			String[] tokens = StringUtils.splitPreserveAllTokens(kv, "=");
			partitionValues.add(tokens[1]);
		}
		return partitionValues;
	}

	/**
	 * 获取配置目录
	 *
	 * @return
	 */
	public static String getConfigDir() {
		return System.getProperty("hdata.conf.dir") + System.getProperty("file.separator");
	}
}
