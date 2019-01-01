package olga.ba;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import dkpro.toolbox.corpus.CorpusException;
import dkpro.toolbox.corpus.analyzed.AnalyzedCorpus;
import dkpro.toolbox.corpus.analyzed.CorpusManager;
import dkpro.toolbox.corpus.analyzed.CorpusManager.CorpusName;
import dkpro.toolbox.core.*;

public class PreliminaryAnalyser {
	
	private AnalyzedCorpus  corpus;
	
	public PreliminaryAnalyser() {	
				
	}
	
	public void InitializeCorpus(dkpro.toolbox.corpus.analyzed.CorpusManager.CorpusName corpusName) throws CorpusException {
		this.corpus = CorpusManager.getCorpus(corpusName);
		OpenNlpSegmenter nlp = new OpenNlpSegmenter();
	
		
		List<Sentence> list = corpus.getSentenceList(); 
		for (String token : corpus.getTokenList()) {
			
		}
		
	}
}