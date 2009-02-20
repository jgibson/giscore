/****************************************************************************************
 *  Overlay.java
 *
 *  Created: Feb 4, 2009
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
package org.mitre.giscore.events;

import java.awt.Color;

import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * Abstract overlay class is a parent to the specific classes for each
 * 
 * @author DRAND
 * 
 */
public abstract class Overlay extends Feature {
	private static final long serialVersionUID = 1L;
	
	private TaggedMap icon = null;
	private Color color = null;
	private int drawOrder = 0;

	/**
	 * @return the icon
	 */
	public TaggedMap getIcon() {
		return icon;
	}

	/**
	 * @param icon
	 *            the icon to set
	 */
	public void setIcon(TaggedMap icon) {
		this.icon = icon;
	}

	/**
	 * @return the color
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * @param color
	 *            the color to set
	 */
	public void setColor(Color color) {
		this.color = color;
	}

	/**
	 * @return the drawOrder
	 */
	public int getDrawOrder() {
		return drawOrder;
	}

	/**
	 * @param drawOrder
	 *            the drawOrder to set
	 */
	public void setDrawOrder(int drawOrder) {
		this.drawOrder = drawOrder;
	}

	/**
	 * The approximately equals method checks all the fields for equality with
	 * the exception of the geometry.
	 * 
	 * @param tf
	 */
	public boolean approximatelyEquals(Feature tf) {
		if (!(tf instanceof Overlay))
			return false;
		if (!super.approximatelyEquals(tf))
			return false;

		Overlay other = (Overlay) tf;

		EqualsBuilder eb = new EqualsBuilder();
		return eb.append(color, other.color).append(drawOrder, other.drawOrder)
				.append(icon, other.icon).isEquals();
	}
}
