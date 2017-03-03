package com.mobvoi.knowledgegraph.scui.jsp;

import java.io.FileInputStream;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.github.fancyerii.review.sentimentanalysis.classifier.BowFeatureExtractor;
import com.github.fancyerii.review.sentimentanalysis.classifier.ClassifierResult;
import com.github.fancyerii.review.sentimentanalysis.classifier.FeatureExtractor;
import com.github.fancyerii.review.sentimentanalysis.classifier.IntentionClassifier;
import com.github.fancyerii.review.sentimentanalysis.classifier.LogisticRegressionIntentionClassifier;
import com.github.fancyerii.review.sentimentanalysis.classifier.TrainingData;

public class ReviewSentimentAnaysis {
	protected static Logger logger=Logger.getLogger(ReviewSentimentAnaysis.class);
	private ReviewSentimentAnaysis() {
		feaExt = new BowFeatureExtractor();
		classifier = new LogisticRegressionIntentionClassifier();
		try {
			String model=ConfigReader.getProps().getProperty("bow_fea_model");
			logger.info("model: "+model);
			feaExt.loadModel(new FileInputStream(model));
			model=ConfigReader.getProps().getProperty("lr_model");
			logger.info("model: "+model);
			classifier.loadModel(new FileInputStream(model));
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
	private IntentionClassifier classifier;
	private FeatureExtractor feaExt;
	private static final ReviewSentimentAnaysis instance = new ReviewSentimentAnaysis();

	public static ReviewSentimentAnaysis getInstance() {
		return instance;
	}

	
	public String classify(String q){
		TreeMap<Integer, Double> fea=feaExt.extractSparseFeature(q);
		TrainingData data=new TrainingData(fea, -1, feaExt.getFeatureCount());
		ClassifierResult result=classifier.classify(data);
		boolean positive=result.probs[1]>0.5;
		if(positive){
			return "正向";
		}else{
			return "负向";
		}
	}
}
