package com.antbrains.crf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

public class Instance implements Serializable {
	private static final long serialVersionUID = -87276062282755946L;
	private int[] attrIds; // item*feature_num
	private int[] labelIds; // labels of each items
	private int length; // number of items

	public Instance(int[] attrIds, int length) {
		if (attrIds == null) {
			throw new NullPointerException("attrIds");
		}
		this.attrIds = attrIds;
		this.length = length;
	}

	public Instance(int[] attrIds, int[] labelIds) {
		this(attrIds, labelIds.length);
		this.labelIds = labelIds;
	}

	public int[] getAttrIds() {
		return attrIds;
	}

	public void setAttrIds(int[] attrIds) {
		this.attrIds = attrIds;
	}

	public int[] labelIds() {
		return labelIds;
	}

	public int length() {
		return length;
	}

	/**
	 * @return featrue_num
	 */
	public int rowSize() {
		return attrIds.length / length;
	}

}