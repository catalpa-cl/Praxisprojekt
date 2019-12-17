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

//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NlpSingleton
{
//    static Logger logger = LoggerFactory.getLogger(NlpSingleton.class);
    
    static LinguisticPreprocessor lp;

    public static LinguisticPreprocessor getInstance() 
    {

        try {
            if (lp != null) {
//                logger.info("NLP backend already initialized");
                return lp;
            }
//            logger.info("Initializing NLP backend");
            lp = new LinguisticPreprocessor();
            return lp;
        }
        catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

}
