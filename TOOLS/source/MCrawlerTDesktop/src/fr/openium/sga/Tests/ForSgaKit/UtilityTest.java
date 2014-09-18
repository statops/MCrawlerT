package fr.openium.sga.Tests.ForSgaKit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import kit.Inj.Table;
import kit.RandomValue.RandomValue;
import kit.RandomValue.RandomValueData;
import kit.RandomValue.RandomValueParser;
import kit.Utils.SgUtils;

import org.junit.Test;

public class UtilityTest {
	@Test
	public void testStringTokinezer() {
		String id = "1,2,3";
		StringTokenizer stk = new StringTokenizer(id, ",");
		String value = id;
		while (stk.hasMoreTokens()) {
			value = stk.nextToken();
		}
		assertEquals("3", value);
		id = "1";
		stk = new StringTokenizer(id, ",");
		value = id;
		while (stk.hasMoreTokens()) {
			value = stk.nextToken();
		}
		assertEquals("1", value);
	}

	@Test
	public void testSort() {
		String id = "1,2,3,10,100,50";
		StringTokenizer stk = new StringTokenizer(id, ",");
		String value = id;
		ArrayList<Integer> mLines = new ArrayList<Integer>();
		while (stk.hasMoreTokens()) {
			value = stk.nextToken();
			mLines.add(Integer.parseInt(value));
		}

		Collections.sort(mLines);
		for (int test : mLines) {
			System.out.println(test);
		}
		assertEquals("" + 1, "" + mLines.get(0));
		assertEquals("" + 3, "" + mLines.get(2));
		assertEquals("" + 11, "" + (mLines.get(3) + 1));
		assertEquals("" + 51, "" + (mLines.get(4) + 1));
		assertEquals("" + 101, "" + (mLines.get(5) + 1));
		assertEquals("" + 101, "" + (mLines.get(mLines.size() - 1) + 1));

	}

	protected File getTestDataFile() {
		return new File("/Users/Stassia/Documents/Scen-genWorkSpace/sgd/TestData/testData.xml");
	}

	@Test
	public void testInjGeneration() {
		ArrayList<String> result = buildInjectionSet();
		assertTrue(result != null);
		for (String inj : result) {

			System.out.println(inj);
			System.out.println("=================");
		}
	}

	private ArrayList<String> buildInjectionSet() {

		Table table = initTable();
		HashSet<RandomValueData> data = new HashSet<RandomValueData>();
		RandomValueParser parser = new RandomValueParser();
		data = parser.parse(getTestDataFile());
		ArrayList<String> injValues = SgUtils.getTestDataSet(data, RandomValue.INJ);
		ArrayList<String> testData = SgUtils.getTestDataSet(data, RandomValue.TEXT);
		ArrayList<String> injectionResult = new ArrayList<String>();
		String accRegx = "\\{data[ 0-9]*\\}|\\{values[ 0-9]*\\}|\\{table[ 0-9]*\\}|\\{column[ 0-9]*\\}";
		// Complete injection from Table

		for (String inj : injValues) {
			String result = inj;
			// detection de type table values ou data
			System.out.println("=================");
			System.out.println(inj);

			try {
				Pattern p = Pattern.compile(accRegx);
				Matcher m = p.matcher(inj);
				while (m.find()) {
					String initfound = m.group();
					System.out.println("Found a " + initfound);
					String found = initfound.replaceAll("\\{|\\}", "");
					System.out.println("Found a " + found);
					// detection du nombre d'element
					int number = 1;
					StringTokenizer stk = new StringTokenizer(found);
					String type = stk.nextToken();
					if (stk.hasMoreTokens()) {
						number = Integer.parseInt(stk.nextToken());
					}
					System.out.println("type :" + type);
					System.out.println("Number of element :" + number);

					// remplacemnet regex
					if (type.equalsIgnoreCase("table")) {
						result = inj.replaceAll("\\{" + found + "\\}", table.mName);
						inj = result;
						// continue;
					}
					if (type.equalsIgnoreCase("data")) {
						result = inj.replaceAll("\\{" + found + "\\}",
								SgUtils.get_a_random_testData(testData));
						inj = result;
						// continue;
					}
					if (type.equalsIgnoreCase("column")) {
						result = inj.replaceAll("\\{" + found + "\\}", table.getColumns(number));
						inj = result;
						// continue;
					}

					if (type.equalsIgnoreCase("values")) {
						result = inj.replaceAll("\\{" + found + "\\}", table.getValues(number));
						inj = result;
						// continue;
					}

				}
				System.out.println("Result :" + result);
				injectionResult.add(result);
				System.out.println("=================");
			} catch (PatternSyntaxException e) {
				System.out.println(e.getMessage());
			}

		}
		return injectionResult;
	}

	/**
	 * @param table
	 * @return
	 */
	private Table initTable() {
		Table ttable = new Table("TestTable");
		ttable.createColumn("_id", "Integer");
		ttable.createColumn("_note", "Text");
		ttable.createColumn("_title", "Text");
		ttable.addValue("_id", "1", "1");
		ttable.addValue("_note", "hello", "1");
		ttable.addValue("_title", "title", "1");
		return ttable;
	}

	@Test
	public void testCollect() {
		ArrayList<Object> content1 = new ArrayList<Object>();
		ArrayList<Object> content2 = new ArrayList<Object>();
		assertEquals(true, content1.size() == content2.size());
		content1.add(1);
		content1.add("b");
		content1.add("c");
		content1.add("d");
		// =================
		content2.add("d");
		content2.add("b");
		content2.add("c");
		content2.add("d");
		assertEquals(true, content1.size() == content2.size());
		assertEquals(true, SgUtils.in(content2, content1));
		assertEquals(false, SgUtils.in(content1, content2));
		assertEquals(false, SgUtils.equal(content1, content2));
		content2.add(1);
		assertEquals(false, SgUtils.equal(content1, content2));
		content1.add("d");
		assertEquals(true, SgUtils.equal(content1, content2));
	}

}
