/****************************************************************************************
 *  IGISInputStream.java
 *
 *  Created: Jan 26, 2009
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
package org.mitre.giscore.input;

import java.io.IOException;

import org.mitre.giscore.events.IGISObject;

/**
 * Read gis objects from a source. The objects can be thought of as occupying
 * a queue fed by the source object. The interface does not guarantee thread
 * safety, read the implementation classes for details. The methods here
 * are meant to be analogues of those in {@link java.io.InputStream}.
 * 
 * @author DRAND
 */
public interface IGISInputStream {
	/**
	 * @return the next gis object present in the source, or <code>null</code>
	 * if there are no more objects present.
	 * @throws IOException 
	 */
	IGISObject read() throws IOException;
	
	/**
	 * Mark the current position in the input stream so a later call to 
	 * {@link #reset()} may move the stream back to the position. 
	 * 
	 * @param readlimit the number of objects that may be read before the
	 * mark becomes invalidated.
	 */
	void mark(int readlimit);
	
	/**
	 * Reposition the stream back to the position a the time that mark 
	 * was last called.
	 */
	void reset();
	
	/**
	 * Close the input stream, freeing any resources held.
	 */
	void close();
}
