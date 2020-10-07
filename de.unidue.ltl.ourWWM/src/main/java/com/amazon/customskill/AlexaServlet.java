package com.amazon.customskill;

public class AlexaServlet extends com.amazon.speech.speechlet.servlet.SpeechletServlet {
	
	/**
     * Diese Klasse NICHT ändern!
     */
    private static final long serialVersionUID = 1L;

    public AlexaServlet() {
	    this.setSpeechlet(new AlexaSkillSpeechlet());
	}
	
}
