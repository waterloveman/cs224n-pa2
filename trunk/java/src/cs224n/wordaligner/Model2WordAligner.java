package cs224n.wordaligner;  
import cs224n.util.*;
import java.util.List;
import java.util.ArrayList;

/**
   * Simple alignment baseline which maps french positions to english positions.
   * If the french sentence is longer, all final word map to null.
   */
public class Model2WordAligner extends WordAligner {
    
    private CounterMap<String,String> transProb;
    private Counter<String> englishCount;
    private Counter<String> frenchCount;
    private Counter<Integer> dparam;
    private int LOW = -100;
    private int HIGH = 100;
    private double NULL_PROB = .2;

    
    public Alignment alignSentencePair(SentencePair sentencePair) {
	Alignment alignment = new Alignment();
	
	List<String> englishWords = new ArrayList<String>(sentencePair.getEnglishWords());
	englishWords.add(0, NULL_WORD);
	List<String> frenchWords = sentencePair.getFrenchWords();

	int I = frenchWords.size();
	int J = englishWords.size();

	for (int i = 0; i < I; i++) {
	    String fre = frenchWords.get(i);
	    double tprob = 1.0;
	    int bestj = -1;
	    double bestProb = -1;
	    for(int j = 0; j < J; j++){
		String eng = englishWords.get(j);
		double mult = posProb(i, j, I, J);
		double prob = mult * alignVal(eng, fre);
		prob *= Math.exp(-Math.abs(eng.length() - fre.length()));
		if(fre.equals(eng))
		    prob = 1;

		if(prob > bestProb){
		    bestj = j;
		    bestProb = prob;
		}
	    }

	    alignment.addAlignment(bestj-1, i, true);
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

    private double posProb(int i, int j, int I, int J){
	if(j == 0)
	    return NULL_PROB;
	return dparam.getCount(bucket(i, j-1, I, J));
    }
    
    public void train(List<SentencePair> trainingPairs_) {
	//things to look at - giving NULL a different alignment prob
	// - lowering the probability of two words mapping to the same word
	transProb = new CounterMap<String,String>();
	englishCount = new Counter<String>();
	frenchCount = new Counter<String>();
	dparam = new Counter<Integer>();
	

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

	//itialize dparam
	for(int i = LOW; i <= HIGH; i++)
	    dparam.setCount(i, 1.0/(HIGH - LOW + 1));


	int frenchSize = frenchCount.keySet().size();

	for(int ii = 0; ii < 10; ii++){
	    System.out.println(ii);
	    CounterMap<String, String> count = new CounterMap<String, String>();
	    Counter<Integer> dcount = new Counter<Integer>();
	    Counter<String> total = new Counter<String>();
	    double dtotal = 0;

	    for(SentencePair pair: trainingPairs){
		Counter<String> total_s = new Counter<String>();
		List<String> engWords = pair.getEnglishWords();		
		List<String> freWords = pair.getFrenchWords();
		
		int I = freWords.size();
		int J = engWords.size();

		for(int i = 0; i < I; i++){
		    String fre = freWords.get(i);
		    for(int j = 0; j < J; j++){
			String eng = engWords.get(j);
			total_s.incrementCount(fre, transHelper(ii, eng, fre, frenchSize) * 
					       posProb(i, j, I, J));
		    }
		    for(int j = 0; j < J; j++){
			String eng = engWords.get(j);
			double c = transHelper(ii, eng, fre, frenchSize) * 
			    posProb(i, j, I, J) / total_s.getCount(fre);
			count.incrementCount(eng, fre, c);
			total.incrementCount(eng, c);
			dcount.incrementCount(bucket(i, j, I, J), c);
			dtotal += c;
		    }
		}
	    }

	    transProb = new CounterMap<String, String>();
	    for(String eng : count.keySet()){
		double engTotal = total.getCount(eng);
		for(String fre : count.getCounter(eng).keySet())
		    transProb.setCount(eng, fre, count.getCount(eng, fre) / engTotal);
	    }
	    for(int i = LOW; i <= HIGH; i++){
		dparam.setCount(i, dcount.getCount(i) / dtotal);
	    }
	}	
    }    

    private int bucket(int i, int j, int I, int J){
	double val = j - (double)i * J / I;
	int ret = Math.min(HIGH, Math.max(LOW, (int) val));
	return ret;
    }

    private double transHelper(int iter, String eng, String fre, int frenchSize){
	if(iter == 0)
	    return 1.0 / frenchSize;//1.0 / frenchCount.keySet().size();
	return transProb.getCount(eng, fre);
    }

}

