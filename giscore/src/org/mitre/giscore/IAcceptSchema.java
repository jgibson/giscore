/****************************************************************************************
 *  IAcceptSchema.java
 *
 *  Created: Mar 25, 2009
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
package org.mitre.giscore;

import org.mitre.giscore.events.Schema;

/**
 * @author DRAND
 *
 */
public interface IAcceptSchema {
	/**
	 * Determine if a schema should be processed
	 * @param schema the schema, never <code>null</code>
	 * @return <code>true</code> if the schema should be accepted
	 */
	boolean accept(Schema schema);
}
