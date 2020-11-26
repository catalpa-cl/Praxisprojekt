package com.amazon.customskill;

public class AlexaServlet extends com.amazon.speech.speechlet.servlet.SpeechletServlet {
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AlexaServlet() {
	    this.setSpeechlet(new AlexaSkillSpeechlet());
	}
	
}
