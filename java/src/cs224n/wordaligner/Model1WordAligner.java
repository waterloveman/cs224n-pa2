package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.List;
import java.util.ArrayList;

import java.io.*;

/**
   * Simple alignment baseline which maps french positions to english positions.
   * If the french sentence is longer, all final word map to null.
   */
public class Model1WordAligner extends WordAligner {
    
    private CounterMap<String,String> transProb;
    private CounterMap<String, String> transProbTemp;
    private Counter<String> englishCount;
    private Counter<String> frenchCount;
    private int totalFrenchSize;
    
    public Alignment alignSentencePair(SentencePair sentencePair) {
	Alignment alignment = new Alignment();
	
	List<String> englishWords = new ArrayList<String>(sentencePair.getEnglishWords());
	englishWords.add(0, NULL_WORD);
	List<String> frenchWords = sentencePair.getFrenchWords();

	int numFrenchWords = frenchWords.size();
	int numEnglishWords = englishWords.size();

	for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
	    int englishPosition = -1;
	    double bestProb = -1;
	    for(int posEngPosition = 0; posEngPosition < numEnglishWords; posEngPosition++){
		double mult = 1;
		mult = (posEngPosition == 0 ? .2 : .8 / numEnglishWords);
		double prob = mult * alignVal(englishWords.get(posEngPosition), 
					      frenchWords.get(frenchPosition));
		if(englishWords.get(posEngPosition).equals(frenchWords.get(frenchPosition)))
		    prob = 1;

		if(prob > bestProb){
		    englishPosition = posEngPosition;
		    bestProb = prob;
		}
	    }

	    alignment.addAlignment(englishPosition-1, frenchPosition, true);
	}
	return alignment;
    }
    
    private double alignVal(String englishStr, String frenchStr){
	double ret = transProb.getCount(englishStr, frenchStr);
	return ret;
    }
    
    
    public double getAlignmentProb(List<String> targetSentence, List<String> sourceSentence, Alignment alignment) { 
	double p = 1;

	int numEnglishWords = targetSentence.size();

	for(Pair<Integer, Integer> align : alignment.getSureAlignments()){
	    int frnAlign = align.getSecond();
	    int engAlign = align.getFirst();
	    String eng = (engAlign < 0)?NULL_WORD:targetSentence.get(engAlign);
	    String frn = (frnAlign < 0)?NULL_WORD:sourceSentence.get(frnAlign);
	    
	    double mult = (engAlign < 0) ? .2 : .8 / numEnglishWords;

	    p *= (mult * alignVal(eng, frn));
	}

	return p; 
    }
    
    
    public CounterMap<String,String> getProbSourceGivenTarget(){ 
	return transProbTemp;
    }
    
    public void train(List<SentencePair> trainingPairs_) {
	transProb = new CounterMap<String,String>();
	englishCount = new Counter<String>();
	frenchCount = new Counter<String>();
	

	List<SentencePair> trainingPairs = new ArrayList<SentencePair>();

	for(SentencePair pair : trainingPairs_){
	    List<String> engWords = new ArrayList<String>(pair.getEnglishWords());
	    engWords.add(0, NULL_WORD);
	    List<String> freWords = new ArrayList<String>(pair.getFrenchWords());
	    trainingPairs.add(new SentencePair(0, "", engWords, freWords));
	}

	//initialize t(e|f)
	for(SentencePair pair : trainingPairs){

	    List<String> engWords = pair.getEnglishWords();
	    List<String> freWords = pair.getFrenchWords();

	    for(String eng : engWords){
		englishCount.incrementCount(eng, 1.0);
	    }

	    for(String fre : freWords){
		frenchCount.incrementCount(fre, 1.0);
	    }
	}

	int frenchSize = frenchCount.keySet().size();
	totalFrenchSize = frenchSize;

	for(int i = 0; i < 10; i++){
	    System.out.println(i);
	    CounterMap<String, String> count = new CounterMap<String, String>();
	    Counter<String> total = new Counter<String>();

	    for(SentencePair pair: trainingPairs){
		Counter<String> total_s = new Counter<String>();
		List<String> engWords = pair.getEnglishWords();		
		List<String> freWords = pair.getFrenchWords();

		for(String fre : freWords){
		    total_s.setCount(fre, 0.0);
		    for(String eng : engWords)
			total_s.incrementCount(fre, transHelper(i, eng, fre, frenchSize));
		    for(String eng : engWords){
			count.incrementCount(eng, fre,  transHelper(i, eng, fre, frenchSize) / total_s.getCount(fre));
			total.incrementCount(eng, transHelper(i, eng, fre, frenchSize) / total_s.getCount(fre));
		    }			
		}		
	    }

	    transProb = new CounterMap<String, String>();
	    for(String eng : count.keySet()){
		double engTotal = total.getCount(eng);
		for(String fre : count.getCounter(eng).keySet())
		    transProb.setCount(eng, fre, count.getCount(eng, fre) / engTotal);
	    }
	}	
	transProbTemp = new CounterMap<String, String>();
	for(String eng : transProb.keySet()){
	    for(String fre: transProb.getCounter(eng).keySet()){
		transProbTemp.setCount(fre, eng, transProb.getCount(eng, fre));
	    }
	}
    }    

    private double transHelper(int iter, String eng, String fre, int frenchSize){
	if(iter == 0)
	    return 1.0 / frenchSize;
	return transProb.getCount(eng, fre);
    }

}

