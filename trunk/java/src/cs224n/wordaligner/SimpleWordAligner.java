package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.List;

/**
   * Simple alignment baseline which maps french positions to english positions.
   * If the french sentence is longer, all final word map to null.
   */
public class SimpleWordAligner extends WordAligner {

    private CounterMap<String,String> totalCount;
    private Counter<String> englishCount;
    private Counter<String> frenchCount;
    List<SentencePair> trainingSet;

    int totalTotal, englishTotal, frenchTotal;

    public SimpleWordAligner(){
	totalCount = new CounterMap<String, String>();
	englishCount = new Counter<String>();
	frenchCount = new Counter<String>();

	totalTotal = englishTotal = frenchTotal = 0;
    }
    
    public Alignment alignSentencePair(SentencePair sentencePair) {
	Alignment alignment = new Alignment();
	
	List<String> englishWords = sentencePair.getEnglishWords();
	List<String> frenchWords = sentencePair.getFrenchWords();

	int numFrenchWords = frenchWords.size();
	int numEnglishWords = englishWords.size();

	for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
	    int englishPosition = -1;
	    double bestProb = -1;
	    for(int posEngPosition = 0; posEngPosition < numEnglishWords; posEngPosition++){
		double prob = alignVal(englishWords.get(posEngPosition), 
				       frenchWords.get(frenchPosition));
		if(prob > bestProb){
		    englishPosition = posEngPosition;
		    bestProb = prob;
		}
	    }

	    alignment.addAlignment(englishPosition, frenchPosition, true);
	}
	return alignment;
    }
    
    private double alignVal(String englishStr, String frenchStr){
	double probBoth = totalCount.getCount(frenchStr, englishStr) / trainingSet.size();
	double probEng = englishCount.getCount(englishStr) / englishTotal;
	double probFre = frenchCount.getCount(frenchStr) / frenchTotal;
	//System.out.println(totalCount.getCount(frenchStr, englishStr) + " " + englishStr + " " + frenchStr + " " + probBoth + " " + probEng + " " + probFre);
	return probBoth / (probEng * probFre);
    }
    
    
    public double getAlignmentProb(List<String> targetSentence, List<String> sourceSentence, Alignment alignment) { 
	return 0; 
    }
    
    public CounterMap<String,String> getProbSourceGivenTarget(){ 
	return totalCount; 
    }
    
    public void train(List<SentencePair> trainingPairs) {
	trainingSet = trainingPairs;
	totalCount = new CounterMap<String,String>();
	for(SentencePair pair : trainingPairs){
	    List<String> targetWords = pair.getEnglishWords();
	    List<String> sourceWords = pair.getFrenchWords();

	    for(String eng : targetWords){
		englishCount.incrementCount(eng, 1.0);
		englishTotal++;
	    }
	    
	    for(String fre : sourceWords){
		frenchCount.incrementCount(fre, 1.0);
		frenchTotal ++;
	    }

	    for(String source : sourceWords){
		for(String target : targetWords){
		    totalCount.incrementCount(source, target, 1.0);
		    totalTotal++;
		}
	    }
	}
    }
    
}
