/****************************************************************************************
 *  TestCSVOutputStream.java
 *
 *  Created: Apr 13, 2009
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantibility and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.test.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.output.IGISOutputStream;


/**
 * @author DRAND
 *
 */
public class TestCSVOutputStream {
	@Test
	public void testStreamOutput1() throws Exception {
		doTest(null, "csv_example.csv", "\r\n");
	}
	
	@Test
	public void testStreamOutput2() throws Exception {
		Schema schema = new Schema();
		schema.put(new SimpleField("package"));
		schema.put(new SimpleField("version"));
		schema.put(new SimpleField("type"));
		schema.put(new SimpleField("website"));
		schema.put(new SimpleField("notes"));
		doTest(schema, "Lab software with versions.csv", null);
	}	

	private void doTest(Schema s, String testfile, String lineDel) throws FileNotFoundException, IOException {
		InputStream stream = getStream(testfile);
		IGISInputStream is = GISFactory
				.getInputStream(DocumentType.CSV, stream, s, lineDel);
		File temp = File.createTempFile("test", ".csv");
		OutputStream outputStream = new FileOutputStream(temp);
		IGISOutputStream os = GISFactory.getOutputStream(DocumentType.CSV, 
				outputStream, lineDel);
		List<IGISObject> data = new ArrayList<IGISObject>();
		while (true) {
			IGISObject obj = is.read();
			if (obj == null)
				break;
			os.write(obj);
			data.add(obj);
		}
		os.close();
		IOUtils.closeQuietly(outputStream);
		
		stream = new FileInputStream(temp);
		is = GISFactory
			.getInputStream(DocumentType.CSV, stream, s, lineDel);
		Iterator<IGISObject> iter = data.iterator();
		while (true) {
			IGISObject obj = is.read();
			if (obj == null)
				break;
			if (obj instanceof Schema) {
				Schema a = (Schema) iter.next();
				// review logic here: instanceof will always return true
				//if (obj instanceof Schema) {
					Schema b = (Schema) obj;
					assertEquals(a.getKeys(), b.getKeys());
				//} else {
					//is.read(); // Need to even off the stream - we had a schema in only one
				//}
			} else {
				Row a = (Row) iter.next();
				Row b = (Row) obj;
				Collection<SimpleField> afields = a.getFields();
				Collection<SimpleField> bfields = b.getFields();
				Iterator<SimpleField> aiter = afields.iterator();
				Iterator<SimpleField> biter = bfields.iterator();
				while(aiter.hasNext()) {
					SimpleField af = aiter.next();
					SimpleField bf = biter.next();
					assertNotNull(af);
					assertNotNull(bf);
					Object av = a.getData(af);
					Object bv = b.getData(bf);
					assertEquals(av, bv);
				}
			}
		}
		
		temp.delete();
	}
	
	private InputStream getStream(String filename) throws FileNotFoundException {
		File file = new File("test/org/mitre/giscore/test/input/" + filename);
		if (file.exists())
			return new FileInputStream(file);
		System.out.println("File does not exist: " + file);
		return getClass().getResourceAsStream(filename);
	}	
}
