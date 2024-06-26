package bndtools.editor.workspace;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.model.BndEditModelHeaderClause;
import bndtools.Plugin;

public class PluginClauseLabelProvider extends StyledCellLabelProvider {

	private final Map<String, IConfigurationElement>	configElements;
	private final Map<String, Image>					images	= new HashMap<>();

	public PluginClauseLabelProvider(Map<String, IConfigurationElement> configElements) {
		this.configElements = configElements;
	}

	@Override
	public void update(ViewerCell cell) {
		BndEditModelHeaderClause header = (BndEditModelHeaderClause) cell.getElement();

		String className = header.getName();
		String displayTitle = header.displayTitle();

		StyledString label = new StyledString(displayTitle,
			header.isLocal() ? null : StyledString.QUALIFIER_STYLER);

		label.append(" (" + header.key() + ": " + className + ")", StyledString.QUALIFIER_STYLER);

		cell.setText(label.toString());
		cell.setStyleRanges(label.getStyleRanges());

		Image image = images.get(className);
		if (image == null) {
			IConfigurationElement configElem = configElements.get(className);
			if (configElem != null) {
				String iconPath = configElem.getAttribute("icon");
				if (iconPath != null) {
					ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(configElem.getContributor()
						.getName(), iconPath);
					if (descriptor != null) {
						image = descriptor.createImage();
						images.put(className, image);
					}
				}
			}
		}
		if (image == null) {
			image = images.get("__DEFAULT__");
			if (image == null) {
				image = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/plugin.png")
					.createImage();
				images.put("__DEFAULT__", image);
			}
		}
		cell.setImage(image);
	}

	@Override
	public String getToolTipText(Object element) {

		if (element instanceof BndEditModelHeaderClause header) {
			StringBuilder sb = new StringBuilder();

			Map<String, String> attribs = header.getAttribs();
			for (Iterator<Entry<String, String>> iter = attribs.entrySet()
				.iterator(); iter.hasNext();) {
				Entry<String, String> entry = iter.next();
				sb.append(entry.getKey());
				sb.append("=");
				sb.append(entry.getValue());

				if (iter.hasNext())
					sb.append("\n");
			}

			if (!header.isLocal()) {
				return "This inherited plugin can only be edited in the included .bnd file. \n\n" + sb.toString();
			}

			return sb.toString();
		}
		return super.getToolTipText(element);
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Image image : images.values()) {
			if (!image.isDisposed())
				image.dispose();
		}
	}
}
