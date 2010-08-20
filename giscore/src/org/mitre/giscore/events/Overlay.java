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
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * Abstract overlay class is a parent to the specific classes for each.
 * Note Overlays extend Feature class but in KML context they do not have a Geometry.  
 * 
 * @author DRAND
 * 
 */
public abstract class Overlay extends Feature {
	private static final long serialVersionUID = 1L;
	
	private TaggedMap icon;
	private Color color;
	private int drawOrder = 0;

	/**
     * Get Icon properties which may include href, refreshMode, refreshInterval,
     * viewRefreshMode, viewFormat, etc. all of which are optional.
     * 
	 * @return the icon property map
	 */
    @CheckForNull
	public TaggedMap getIcon() {
		return icon;
	}

	/**
     * Set Icon properties which may include href, refreshMode, refreshInterval,
     * viewRefreshMode, viewFormat, etc. all of which are optional.
     * 
	 * @param icon
	 *            the icon property map to set
	 */
	public void setIcon(TaggedMap icon) {
		this.icon = icon;
	}

	/**
	 * @return the color
	 */
    @CheckForNull
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

	/* (non-Javadoc)
	 * @see org.mitre.giscore.events.Feature#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
		boolean hascolor = in.readBoolean();
		if (hascolor) {
			int rgb = in.readInt();
			color = new Color(rgb);
		}
		drawOrder = in.readInt();
		icon = (TaggedMap) in.readObject();
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.events.Feature#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {	
		super.writeData(out);
		if (color != null) {
			out.writeBoolean(true);
			out.writeInt(color.getRGB());
		} else {
			out.writeBoolean(false);
		}
		out.writeInt(drawOrder);
		out.writeObject(icon);
	}

    @Override
    public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}
}
