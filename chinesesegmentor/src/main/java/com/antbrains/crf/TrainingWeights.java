package com.antbrains.crf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import gnu.trove.map.hash.TObjectIntHashMap;

public class TrainingWeights implements java.io.Serializable {
	private static final long serialVersionUID = 8928028057374831674L;

	public double[] getBosTransitionWeights() {
		return bosTransitionWeights;
	}

	public void setBosTransitionWeights(double[] bosTransitionWeights) {
		this.bosTransitionWeights = bosTransitionWeights;
	}

	public double[] getEosTransitionWeights() {
		return eosTransitionWeights;
	}

	public void setEosTransitionWeights(double[] eosTransitionWeights) {
		this.eosTransitionWeights = eosTransitionWeights;
	}

	public double[] getTransitionWeights() {
		return transitionWeights;
	}

	public void setTransitionWeights(double[] transitionWeights) {
		this.transitionWeights = transitionWeights;
	}

	public double[] getAttributeWeights() {
		return attributeWeights;
	}

	public void setAttributeWeights(double[] attributeWeights) {
		this.attributeWeights = attributeWeights;
	}

	// weights of each label as start state
	private double[] bosTransitionWeights;
	// weights of each label as end state
	private double[] eosTransitionWeights;
	// weights from one label to another, to speed up, using 1d arrary to
	// represent 2d array
	private double[] transitionWeights;
	// weights from label to feature, to speed up, using 1d arrary to represent
	// 2d array
	private double[] attributeWeights;

	public Template getTemplate() {
		return template;
	}

	public void setTemplate(Template template) {
		this.template = template;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	// private TObjectIntHashMap<String> labelDict;
	// private TObjectIntHashMap<String> attributeDict;
	private TObjectIntHashMap<String> labelDict;

	public TObjectIntHashMap<String> getLabelDict() {
		return labelDict;
	}

	public void setLabelDict(TObjectIntHashMap<String> labelDict) {
		this.labelDict = labelDict;
	}

	public FeatureDict getAttributeDict() {
		return attributeDict;
	}

	public void setAttributeDict(FeatureDict attributeDict) {
		this.attributeDict = attributeDict;
	}

	private FeatureDict attributeDict;
	private Template template;

	private String[] labelTexts;

	public String[] getLabelTexts() {
		return labelTexts;
	}

	public void setLabelTexts(String[] labelTexts) {
		this.labelTexts = labelTexts;
	}

	public TrainingWeights(Template template) {
		this.template = template;
	}

	public TrainingWeights(Template template, FeatureDictEnum dictType) {
		this.template = template;
		//
		this.labelDict = new TObjectIntHashMap<String>(10, 0.75f, -1);
		// this.attributeDict=new TObjectIntHashMap<String>(10000, 0.75f, -1);
		if (dictType == FeatureDictEnum.TROVE_HASHMAP) {
			this.attributeDict = new TroveFeatureDict(102400);
		} else if (dictType == FeatureDictEnum.DOUBLE_ARRAY_TRIE) {
			this.attributeDict = new DATrieFeatureDict();
		} else if (dictType == FeatureDictEnum.COMPACT_TROVE_MAP) {
			this.attributeDict = new CompactedTroveFeatureDict(102400);
		}
	}

	private void writeDoubleArray(DataOutput out, double[] array) throws IOException {
		out.writeInt(array.length);
		for (double d : array) {
			out.writeDouble(d);
		}
	}

}
