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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendStarter implements ServletContextListener
{
    static Logger logger = LoggerFactory.getLogger(LinguisticPreprocessor.class);

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        logger.info("Context is starting up");
        
        try {
            //trigger initialization
            NlpSingleton.getInstance();
        }
        catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        logger.info("Context is shutting down");
    }

}
