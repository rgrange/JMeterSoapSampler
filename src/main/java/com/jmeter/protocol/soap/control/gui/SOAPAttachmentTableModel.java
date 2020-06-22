package com.jmeter.protocol.soap.control.gui;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import com.jmeter.sampler.util.SOAPMessages;

public class SOAPAttachmentTableModel implements TableModel {

	private ArrayList<AttachmentDefinition> attachmentDef = new ArrayList<>();
	private ArrayList<TableModelListener> listeners = new ArrayList<>();
	public static final String TYPE_RESOURCE = SOAPMessages.getResString("soap_resource");
	public static final String TYPE_VALUE = SOAPMessages.getResString("soap_value");

	public ArrayList<AttachmentDefinition> getAttachments() {
		ArrayList<AttachmentDefinition> copy = new ArrayList<>();

		for(int i = 0; i < this.attachmentDef.size(); ++i) {
			copy.add(this.attachmentDef.get(i).duplicate());
		}

		return copy;
	}

	public AttachmentDefinition getAttachment(int index) {
		return index >= 0 && index < this.attachmentDef.size()?(AttachmentDefinition)this.attachmentDef.get(index):null;
	}

	public void clear() {
		this.attachmentDef.clear();
		this.postTableEvent();
	}

	public void add(AttachmentDefinition atd) {
		this.attachmentDef.add(atd.duplicate());
		this.postTableEvent();
	}

	public void replace(ArrayList<AttachmentDefinition> attachments) {
		this.attachmentDef.clear();

		for(int i = 0; i < attachments.size(); ++i) {
			this.attachmentDef.add(attachments.get(i).duplicate());
		}

		this.postTableEvent();
	}

	public void add(String type, String attachment, String contentType, String contentId) {
		AttachmentDefinition def = new AttachmentDefinition();
		def.attachment = attachment;
		def.type = type.equals(TYPE_RESOURCE)?1:2;
		def.contentID = contentId;
		def.contentType = contentType;
		this.attachmentDef.add(def);
		this.postTableEvent();
	}

	private void postTableEvent() {
		TableModelEvent event = new TableModelEvent(this);
		Iterator<TableModelListener> listIt = this.listeners.iterator();

		while(listIt.hasNext()) {
			listIt.next().tableChanged(event);
		}

	}

	public void remove(int itemNumber) {
		if(itemNumber >= 0 && itemNumber < this.attachmentDef.size()) {
			this.attachmentDef.remove(itemNumber);
			this.postTableEvent();
		}
	}

	@Override
	public void addTableModelListener(TableModelListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public Class<String> getColumnClass(int col) {
		return String.class;
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public String getColumnName(int col) {
		switch(col) {
		case 0:
			return SOAPMessages.getResString("soap_colname_restype");
		case 1:
			return SOAPMessages.getResString("soap_colname_attachment");
		case 2:
			return SOAPMessages.getResString("soap_colname_contenttype");
		case 3:
			return SOAPMessages.getResString("soap_colname_contentid");
		default:
			return null;
		}
	}

	@Override
	public int getRowCount() {
		return this.attachmentDef.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		if(row >= this.attachmentDef.size()) {
			return col >= 0 && col < 4?"":null;
		} else {
			AttachmentDefinition atDef = this.attachmentDef.get(row);
			switch(col) {
			case 0:
				if(atDef.type == 1) {
					return TYPE_RESOURCE;
				}

				return TYPE_VALUE;
			case 1:
				return atDef.attachment;
			case 2:
				return atDef.contentType;
			case 3:
				return atDef.contentID;
			default:
				return null;
			}
		}
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return row < this.attachmentDef.size();
	}

	@Override
	public void removeTableModelListener(TableModelListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		if(row < this.attachmentDef.size()) {
			AttachmentDefinition atDef = this.attachmentDef.get(row);
			switch(col) {
			case 0:
				if(((String)value).equals(TYPE_RESOURCE)) {
					atDef.type = 1;
				} else {
					atDef.type = 2;
				}
				break;
			case 1:
				atDef.attachment = (String)value;
				break;
			case 2:
				atDef.contentType = (String)value;
				break;
			case 3:
				atDef.contentID = (String)value;
				break;
			default:
				return;
			}

			this.postTableEvent();
		}
	}
}
