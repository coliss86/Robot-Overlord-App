package com.marginallyclever.robotOverlord.swingInterface.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.UndoableEditEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.undo.AbstractUndoableEdit;

import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.swingInterface.translator.Translator;
import com.marginallyclever.robotOverlord.swingInterface.undoableEdits.StringEdit;
import com.marginallyclever.robotOverlord.uiExposedTypes.StringEntity;

/**
 * Panel to alter a file parameter.
 * @author Dan Royer
 *
 */
public class ViewElementFilename extends ViewElement implements ActionListener {
	private static String lastPath=System.getProperty("user.dir");
	private JTextField field;
	private ArrayList<FileFilter> filters = new ArrayList<FileFilter>();
	private StringEntity e;
	
	public ViewElementFilename(RobotOverlord ro,final StringEntity e) {
		super(ro);
		this.e=e;
		
		//this.setBorder(BorderFactory.createLineBorder(Color.RED));
				
		field = new JTextField(15);
		field.setEditable(false);
		field.setText(e.get());
		field.setMargin(new Insets(1,0,1,0));
		//pathAndFileName.setBorder(BorderFactory.createLoweredBevelBorder());
		
		JLabel label=new JLabel(e.getName(),JLabel.LEADING);
		label.setLabelFor(field);

		JButton choose = new JButton("...");
		choose.addActionListener(this);
		choose.setMargin(new Insets(0, 5, 0, 5));
		choose.addFocusListener(this);
		
		panel.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx=0;
		gbc.gridy=0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		//gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.insets.right=5;
		panel.add(label,gbc);
		gbc.weightx=1;
		gbc.insets.left=0;
		gbc.insets.right=0;
		panel.add(field,gbc);
		gbc.weightx=0;
		panel.add(choose,gbc);
		
		e.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				field.setText(e.get());
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		JFileChooser chooser = new JFileChooser();
		if(filters.size()==0) return;  // @TODO: fail!
		if(filters.size()==1) chooser.setFileFilter(filters.get(0));
		else {
			Iterator<FileFilter> i = filters.iterator();
			while(i.hasNext()) {
				chooser.addChoosableFileFilter(i.next());
			}
		}
		if(lastPath!=null) chooser.setCurrentDirectory(new File(lastPath));
		int returnVal = chooser.showDialog(ro.getMainFrame(), Translator.get("Select"));
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			String newFilename = chooser.getSelectedFile().getAbsolutePath();
			lastPath = chooser.getSelectedFile().getParent();

			AbstractUndoableEdit event = new StringEdit(e, newFilename);
			if(ro!=null) ro.undoableEditHappened(new UndoableEditEvent(this,event) );
		}
	}

	public void setFileFilter(FileFilter arg0) {
		filters.clear();
		filters.add(arg0);
	}
	
	public void addFileFilter(FileFilter arg0) {
		filters.add(arg0);
	}
	
	/**
	 * Plural form of {@link addChoosableFileFilter}.
	 * @param arg0 {@link ArrayList} of {@link FileFilter}.
	 */
	public void addFileFilters(ArrayList<FileFilter> arg0) {
		filters.addAll(arg0);
	}


	@Override
	public void setReadOnly(boolean arg0) {
		field.setEnabled(!arg0);
	}
}
