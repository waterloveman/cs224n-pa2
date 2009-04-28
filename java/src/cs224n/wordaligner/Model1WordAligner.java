package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.List;
import java.util.ArrayList;

/**
   * Simple alignment baseline which maps french positions to english positions.
   * If the french sentence is longer, all final word map to null.
   */
public class Model1WordAligner extends WordAligner {
    
    private CounterMap<String,String> transProb;
    private Counter<String> englishCount;
    private Counter<String> frenchCount;
    private String NULL_STRING = "<NULL>";

    
    public Alignment alignSentencePair(SentencePair sentencePair) {
	Alignment alignment = new Alignment();
	
	List<String> englishWords = new ArrayList<String>(sentencePair.getEnglishWords());
	englishWords.add(0, NULL_STRING);
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

	    alignment.addAlignment(englishPosition-1, frenchPosition, true);
	}
	return alignment;
    }
    
    private double alignVal(String englishStr, String frenchStr){
	return transProb.getCount(englishStr, frenchStr);
    }
    
    
    public double getAlignmentProb(List<String> targetSentence, List<String> sourceSentence, Alignment alignment) { 
	return 0; 
    }
    
    
    public CounterMap<String,String> getProbSourceGivenTarget(){ 
	return transProb;
    }
    
    public void train(List<SentencePair> trainingPairs_) {
	//things to look at - giving NULL a different alignment prob
	// - lowering the probability of two words mapping to the same word
	transProb = new CounterMap<String,String>();
	englishCount = new Counter<String>();
	frenchCount = new Counter<String>();
	
	CounterMap<String, String> count = new CounterMap<String, String>();
	Counter<String> total = new Counter<String>();

	List<SentencePair> trainingPairs = new ArrayList<SentencePair>();

	for(SentencePair pair : trainingPairs_){
	    List<String> engWords = new ArrayList<String>(pair.getEnglishWords());
	    engWords.add(0, NULL_STRING);
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
 	for(String eng : englishCount.keySet()){
 	    for(String fre : frenchCount.keySet()){
 		transProb.setCount(eng, fre, 1.0 / frenchSize);
 	    }
 	}

	for(int i = 0; i < 10; i++){
	    System.out.println(i);
	    for(String eng : englishCount.keySet()){
		total.setCount(eng, 0);
		for(String fre : frenchCount.keySet()){
		    count.setCount(eng, fre, 0);
		}
	    }

	    for(SentencePair pair: trainingPairs){
		Counter<String> total_s = new Counter<String>();
		List<String> engWords = pair.getEnglishWords();
		
		List<String> freWords = pair.getFrenchWords();

		for(String fre : freWords){
		    total_s.setCount(fre, 0.0);
		    for(String eng : engWords)
			total_s.incrementCount(fre, transProb.getCount(eng, fre));
		}
		
		for(String fre : freWords)
		    for(String eng : engWords){
			count.incrementCount(eng, fre, transProb.getCount(eng, fre) / total_s.getCount(fre));
			total.incrementCount(eng, transProb.getCount(eng, fre) / total_s.getCount(fre));
		    }			 
	    }

	    for(String eng : englishCount.keySet()){
		double engTotal = total.getCount(eng);
		for(String fre : frenchCount.keySet())
		    transProb.setCount(eng, fre, count.getCount(eng, fre) / engTotal);
	    }
	}	
	//for(String fre : frenchCount.keySet())
	//  for(String eng : englishCount.keySet())
	//System.out.println(fre + " " + eng + " " + transProb.getCount(eng, fre));

    }
    
}

