/*******************************************************************************
 * Copyright 2018
 * Language Technology Lab
 * University of Duisburg-Essen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package nlp.dkpro.backend;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.io.text.StringReader;
import de.tudarmstadt.ukp.dkpro.core.matetools.MateLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpChunker;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class LinguisticPreprocessor
{
    private AnalysisEngine pipeline;
    private File output;
    
    static Logger logger = LoggerFactory.getLogger(LinguisticPreprocessor.class);

    public LinguisticPreprocessor() throws Exception
    {
        init();
    }

    /**
     * Alexa waits only some time for a response. Heavy and slow initialization will easily lead to
     * timeouts because Alexa did not receive an answer within the expected time frame. We
     * initialize and load everything here. This code is executed when the Tomcat starts to ensure
     * everything is setup once the Alexa skill is triggered.
     */
    public void init() throws Exception
    {
        if (pipeline != null) {
            System.err.println("Pipeline already initialized");
            return;
        }

        pipeline = assemblePipeline();
        
        dummyRun();

//        System.out.println("init completed()");
        logger.info("init completed()");
    }

    private void dummyRun() throws Exception
    {
        CollectionReader reader = CollectionReaderFactory.createReader(StringReader.class,
                StringReader.PARAM_DOCUMENT_TEXT, "test", StringReader.PARAM_DOCUMENT_ID, "1234",
                StringReader.PARAM_LANGUAGE, "en");

        JCasIterator iterator = new JCasIterator(reader);
        JCas next = iterator.next();
        pipeline.process(next);        
    }

    private AnalysisEngine assemblePipeline() throws Exception
    {
        output = File.createTempFile("tmp", ".txt");
        
        /* This splits up text into sentences and single words */
        AnalysisEngineDescription segmenter = AnalysisEngineFactory
                .createEngineDescription(BreakIteratorSegmenter.class);

        AnalysisEngineDescription lemma = AnalysisEngineFactory
                .createEngineDescription(StanfordLemmatizer.class);
        
        AnalysisEngineDescription pos = AnalysisEngineFactory.createEngineDescription(
                StanfordPosTagger.class, StanfordPosTagger.PARAM_LANGUAGE,
                "en");
        
       AnalysisEngineDescription chunker = AnalysisEngineFactory.createEngineDescription(
    		  OpenNlpChunker.class );

        AggregateBuilder builder = new AggregateBuilder();
        builder.add(segmenter);
        builder.add(pos);
        builder.add(lemma);
        builder.add(chunker);
        pipeline = builder.createAggregate();
        
        return pipeline;
    }

    /**
     *  A main class to see what this class does without having to run the skill 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
       
        LinguisticPreprocessor n = new LinguisticPreprocessor();
        List<String> located = n.findNPs("I would like three red tomatoes.");
        located.forEach(x -> System.out.println("Found noun: "+x));

        located = n.findNPs("Give me one kilo of strawberries, please.");
        located.forEach(x -> System.out.println("Found noun: "+x));
    }

    public List<String> findNPs(String text) throws Exception
    {
        List<String> nouns = new ArrayList<>();

        try {

            CollectionReader reader = CollectionReaderFactory.createReader(StringReader.class,
                    StringReader.PARAM_DOCUMENT_TEXT, text, StringReader.PARAM_DOCUMENT_ID, "1234",
                    StringReader.PARAM_LANGUAGE, "en");
            

            JCasIterator iterator = new JCasIterator(reader);
            JCas jcas = iterator.next();
            pipeline.process(jcas);
                        
            for(Token t: JCasUtil.select(jcas, Token.class)){
            	System.out.println(t.getCoveredText() + " "+t.getLemmaValue()+ " "+t.getPosValue());
            	POS pos = t.getPos();
            	if(pos.getPosValue().startsWith("N")) {
                    nouns.add(pos.getCoveredText());
                }
            }
            
            for (Chunk c : JCasUtil.select(jcas, Chunk.class)) {
            	System.out.println(c.getChunkValue()+": "+c.getCoveredText());
            }

        }
        catch (Exception e) {
            throw new UnsupportedOperationException();
        }
        return nouns;
    }

}
