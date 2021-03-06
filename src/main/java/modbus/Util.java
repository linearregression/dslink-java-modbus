package modbus;

import java.util.Arrays;

import org.dsa.iot.dslink.util.json.JsonArray;

import com.serotonin.modbus4j.locator.NumericLocator;
import com.serotonin.modbus4j.locator.StringLocator;

public class Util {

	public static <E> String[] enumNames(Class<E> enumData) {
		String valuesStr = Arrays.toString(enumData.getEnumConstants());
		return valuesStr.substring(1, valuesStr.length() - 1).replace(" ", "").split(",");
	}

	private static short[] concatArrays(short[] arr1, short[] arr2) {
		int len1 = arr1.length;
		int len2 = arr2.length;
		short[] retval = new short[len1 + len2];
		System.arraycopy(arr1, 0, retval, 0, len1);
		System.arraycopy(arr2, 0, retval, len1, len2);
		return retval;
	}

	protected static boolean[] makeBoolArr(JsonArray jarr) throws Exception {
		boolean[] retval = new boolean[jarr.size()];
		for (int i = 0; i < jarr.size(); i++) {
			Object o = jarr.get(i);
			if (!(o instanceof Boolean))
				throw new Exception("not a boolean array");
			else
				retval[i] = (Boolean) o;
		}
		return retval;
	}

	protected static short[] makeShortArr(JsonArray jarr, DataType dt, double scaling, double addscaling, PointType pt,
			int slaveid, int offset, int numRegisters, int bitnum) throws Exception {
		short[] retval = {};
		Integer dtint = DataType.getDataTypeInt(dt);
		if (dtint != null) {
			if (!dt.isString()) {
				NumericLocator nloc = new NumericLocator(slaveid, PointType.getPointTypeInt(pt), offset, dtint);
				for (int i = 0; i < jarr.size(); i++) {
					Object o = jarr.get(i);
					if (!(o instanceof Number))
						throw new Exception("not a numeric array");
					Number n = ((Number) o).doubleValue() * scaling - addscaling;
					retval = concatArrays(retval, nloc.valueToShorts(n));
				}
			} else {
				Object o = jarr.get(0);
				if (!(o instanceof String))
					throw new Exception("not a string");
				String str = (String) o;
				StringLocator sloc = new StringLocator(slaveid, PointType.getPointTypeInt(pt), offset, dtint,
						numRegisters);
				retval = sloc.valueToShorts(str);
			}
		} else if (dt == DataType.BOOLEAN) {
			retval = new short[(int) Math.ceil((double) jarr.size() / 16)];
			for (int i = 0; i < retval.length; i++) {
				short element = 0;
				for (int j = 0; j < 16; j++) {
					int bit = 0;
					if (j == bitnum) {
						Object o = jarr.get(i);
						if (!(o instanceof Boolean))
							throw new Exception("not a boolean array");
						if ((Boolean) o)
							bit = 1;
					} else if (bitnum == -1 && i + j < jarr.size()) {
						Object o = jarr.get(i + j);
						if (!(o instanceof Boolean))
							throw new Exception("not a boolean array");
						if ((Boolean) o)
							bit = 1;
					}
					element = (short) (element & (bit << (15 - j)));
					jarr.get(i + j);
				}
				retval[i] = element;
			}
			return retval;
		} else if (dt == DataType.INT32M10K || dt == DataType.UINT32M10K || dt == DataType.INT32M10KSWAP
				|| dt == DataType.UINT32M10KSWAP) {
			retval = new short[2 * jarr.size()];
			for (int i = 0; i < jarr.size(); i++) {
				Object o = jarr.get(i);
				if (!(o instanceof Number))
					throw new Exception("not an int array");
				Number n = ((Number) o).doubleValue() * scaling - addscaling;
				long aslong = n.longValue();
				if (dt == DataType.INT32M10K || dt == DataType.UINT32M10K) {
					retval[i * 2] = (short) (aslong / 10000);
					retval[(i * 2) + 1] = (short) (aslong % 10000);
				} else {
					retval[i * 2] = (short) (aslong % 10000);
					retval[(i * 2) + 1] = (short) (aslong / 10000);
				}

			}
		}
		return retval;
	}

	static int toUnsignedInt(short x) {
		return ((int) x) & 0xffff;
	}

	static long toUnsignedLong(int x) {
		return ((long) x) & 0xffffffffL;
	}

}
