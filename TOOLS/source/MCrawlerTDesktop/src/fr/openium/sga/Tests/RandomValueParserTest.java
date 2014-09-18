package fr.openium.sga.Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import kit.Config.Config;
import kit.RandomValue.RandomValue;
import kit.RandomValue.RandomValueData;
import kit.RandomValue.RandomValueParser;
import kit.Utils.SgUtils;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

public class RandomValueParserTest {

	@Test
	public void test2() throws SAXException, IOException,
			ParserConfigurationException {
		HashSet<RandomValueData> rv = new HashSet<RandomValueData>();
		RandomValueParser parser = new RandomValueParser();
		rv = parser.parse(new File(Config.CURRENT_DIRECTORY
				+ "/TEST/TestData/testData.xml"));
		assertTrue(!rv.isEmpty());
		//assertEquals(6, rv.size());
		
		assertEquals(2, SgUtils.getTestDataSet(rv, RandomValue.TEXT).size());
		assertEquals(2, SgUtils.getTestDataSet(rv, RandomValue.INTEGER).size());
		assertEquals(8, SgUtils.getTestDataSet(rv, RandomValue.PASSWORD).size());
		assertEquals(7, SgUtils.getTestDataSet(rv, RandomValue.STRESS).size());
		assertEquals(7, SgUtils.getTestDataSet(rv, RandomValue.LOG).size());
		assertEquals(10, SgUtils.getTestDataSet(rv, RandomValue.MAIL).size());
	}

	@Test
	public void test() throws SAXException, IOException,
			ParserConfigurationException {
		HashSet<RandomValueData> rv = new HashSet<RandomValueData>();
		RandomValueParser parser = new RandomValueParser();
		rv = parser.parse(new File(Config.CURRENT_DIRECTORY
				+ "/TEST/TestData/rv.xml"));
		assertTrue(!rv.isEmpty());
		assertTrue(rv.size() == 4);

		Iterator<RandomValueData> it = rv.iterator();
		while (it.hasNext()) {
			RandomValueData rvd = it.next();
			if (rvd.getType().equals("string")) {
				assertTrue(rvd.getValue().contains("test")
						|| rvd.getValue().contains("0"));
			}
			if (rvd.getType().equals("int")) {
				assertTrue(rvd.getValue().contains("1")
						|| rvd.getValue().contains("2"));
			}
			if (rvd.getType().equals("mail")) {
				assertTrue(rvd.getValue().contains("st@op.fr"));
			}
		}

		Iterator<RandomValueData> itt = rv.iterator();
		while (itt.hasNext()) {
			RandomValueData rvd = itt.next();
			if (rvd.getType().equals(RandomValue.MAIL)) {
				ArrayList<String> list = new ArrayList<String>(rvd.getValue());
				System.out.print(list.get(0));
			}

		}

		String ext = FilenameUtils.getExtension(Config.CURRENT_DIRECTORY
				+ "/TEST/TestData/rv.xml");
		assertTrue(ext.equalsIgnoreCase(Config.XMLEXTENSION));
		/*
		 * kit.RandomValue.RandomValueParser ranpar = new
		 * kit.RandomValue.RandomValueParser();
		 * 
		 * ranpar.parse(new File(Config.CURRENT_DIRECTORY +
		 * "/TEST/TestData/testData.xml"));
		 */
	}

}
