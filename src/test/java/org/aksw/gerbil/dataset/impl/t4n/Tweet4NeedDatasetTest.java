/**
 * This file is part of General Entity Annotator Benchmark.
 *
 * General Entity Annotator Benchmark is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * General Entity Annotator Benchmark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with General Entity Annotator Benchmark.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aksw.gerbil.dataset.impl.t4n;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class Tweet4NeedDatasetTest {

	/*
	 * The test file cotains two special cases. One marking with the URI "*null*"
	 * and two markings that are overlapping.
	 */
	private static final String TEST_TEXT_DIR = "C:\\Users\\Nikit\\Desktop\\tweet4need";
	private static final String DATASET_NAME = "t4nTestDataset";

	private static final String EXPECTED_DOCUMENT_URI = "http://t4nTestDataset/1000000000582";
	private static final String EXPECTED_TEXT = "RT @superjaberwocky: KXLB: Students have been bussed to the Bozeman Public Library because of smoke blowing to Hawthorne School. #bozexplod";
	private static final Marking EXPECTED_MARKINGS[] = new Marking[] {
			(Marking) new NamedEntity(21, 4, new HashSet<String>(Arrays.asList("http://en.wikipedia.org/wiki/KXLB","http://dbpedia.org/resource/KXLB"))),
			(Marking) new NamedEntity(60, 22,
					new HashSet<String>(Arrays.asList("http://en.wikipedia.org/wiki/Bozeman_Public_Library","http://dbpedia.org/resource/Bozeman_Public_Library"))),
			(Marking) new NamedEntity(111, 16, new HashSet<String>(Arrays.asList("http://www.bsd7.org/hawthorne/"))) };

	@Test
	public void test() throws GerbilException {
		Tweet4NeedDataset dataset = new Tweet4NeedDataset(TEST_TEXT_DIR);
		dataset.setName(DATASET_NAME);
		dataset.init();
		for (Document document : dataset.getInstances()) {
			if (document.getDocumentURI().equals(EXPECTED_DOCUMENT_URI)) {
				Assert.assertEquals(EXPECTED_TEXT, document.getText());

				Set<Marking> expectedNEs = new HashSet<Marking>(Arrays.asList(EXPECTED_MARKINGS));
				for (Marking marking : document.getMarkings()) {
					Assert.assertTrue("Couldn't find " + marking.toString() + " inside " + expectedNEs.toString(),
							expectedNEs.contains(marking));
				}
				Assert.assertEquals(expectedNEs.size(), document.getMarkings().size());
			}
		}
		IOUtils.closeQuietly(dataset);
	}

	public static void main(String[] args) throws GerbilException {
		Tweet4NeedDatasetTest test = new Tweet4NeedDatasetTest();
		test.test();
	}
}
