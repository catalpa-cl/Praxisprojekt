/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.amazon.customskill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SsmlOutputSpeech;


/*
 * This class is the actual skill. Here you receive the input and have to produce the speech output. 
 */
public class AlexaSkillSpeechlet
implements SpeechletV2
{
	static Logger logger = LoggerFactory.getLogger(AlexaSkillSpeechlet.class);
	
	public static String userRequest;
	
	private static int sum;
	private static String answerOption1 = "";
	private static String answerOption2 = "";
	private static boolean publikumUsed;
	private static boolean fiftyfiftyUsed;
	private static String question = "";
	private static String correctAnswer = "";
	private static enum RecognitionState {Answer, YesNo};
	private RecognitionState recState;
	private static enum UserIntent {Yes, No, A, B, C, D, Publikum, FiftyFifty};
	private UserIntent ourUserIntent;

	static String welcomeMsg = "Hallo, willkommen bei Wer Wird Millionär.";
	static String wrongMsg = "Das ist leider falsch.";
	static String correctMsg = "Das ist richtig";
	static String continueMsg = "Möchten Sie weiterspielen?";
	static String congratsMsg = "Herzlichen Glückwunsch! Sie haben eine Million Euro gewonnen.";
	static String goodbyeMsg = "Bella Ciao!";
	static String sumMsg = "Sie haben {replacement} Euro gewonnen";
	static String fiftyfiftyUsedMsg = "Sie haben den 50 50 Joker leider schon verbraucht. Wie ist Ihre Antwort?";
	static String fiftyfiftyAnswerMsg = "Ok, sie nehmen also den 50 50 Joker. "
			+ "Es bleiben noch die Antworten {replacement} und {replacement2} übrig";

	static String publikumUsedMsg = "Sie haben den Publikumsjoker leider schon verbraucht. Wie ist Ihre Antwort?";
	static String publikumAnswerMsg = "Ok, sie nehmen also den Publikumsjoker. "
			+ "Das Publikum ist mehrmeitlich für Antwort {replacement}.";
	static String errorYesNoMsg = "Das habe ich nicht verstanden. Sagen Sie bitte ja oder nein.";
	static String errorAnswerMsg = "Das habe ich nicht verstanden. Sagen Sie bitte a, b, c, d, Publikum oder 50:50.";

	
	private String buildString(String msg, String replacement1, String replacement2) {
		return msg.replace("{replacement}", replacement1).replace("{replacement2}", replacement2);
	}
	
	

	

	@Override
	public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope)
	{
		logger.info("Alexa session begins");
		publikumUsed = false;
		fiftyfiftyUsed = false;
		sum = 0;
		recState = RecognitionState.Answer;
	}

	@Override
	public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope)
	{
		selectQuestion();
		return askUserResponse(welcomeMsg+" "+question);
	}

	private void selectQuestion() {
		switch(sum){
		case 0: question = "Frage?"; correctAnswer = "a"; break;
		case 50: question = "Frage?"; correctAnswer = "a"; break;
		case 100: question = "Frage?"; correctAnswer = "a"; break;
		case 200: question = "Frage?"; correctAnswer = "a"; break;
		case 300: question = "Frage?"; correctAnswer = "a"; break;
		case 500: question = "Frage?"; correctAnswer = "a"; break;
		case 1000: question = "Frage?"; correctAnswer = "a"; break;
		case 2000: question = "Frage?"; correctAnswer = "a"; break;
		case 4000: question = "Frage?"; correctAnswer = "a"; break;
		case 8000: question = "Frage?"; correctAnswer = "a"; break;
		case 16000: question = "Frage?"; correctAnswer = "a"; break;
		case 32000: question = "Frage?"; correctAnswer = "a"; break;
		case 64000: question = "Frage?"; correctAnswer = "a"; break;
		case 125000: question = "Frage?"; correctAnswer = "a"; break;
		case 500000: question = "Frage?"; correctAnswer = "a"; break;
		}
	}

	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope)
	{
		IntentRequest request = requestEnvelope.getRequest();
		Intent intent = request.getIntent();
		userRequest = intent.getSlot("anything").getValue();
		logger.info("Received following text: [" + userRequest + "]");
		logger.info("recState is [" + recState + "]");
		SpeechletResponse resp = null;
		switch (recState) {
		case Answer: resp = evaluateAnswer(userRequest); break;
		case YesNo: resp = evaluateYesNo(userRequest); recState = RecognitionState.Answer; break;
		default: resp = response("Erkannter Text: " + userRequest);
		}   
		return resp;
	}

	private SpeechletResponse evaluateYesNo(String userRequest) {
		SpeechletResponse res = null;
		recognizeUserIntent(userRequest);
		switch (ourUserIntent) {
		case Yes: {
			selectQuestion();
			res = askUserResponse(question); break;
		} case No: {
			res = response(buildString(sumMsg, String.valueOf(sum), "")+" "+goodbyeMsg); break;
		} default: {
			res = askUserResponse(errorYesNoMsg);
		}
		}
		return res;
	}


	private SpeechletResponse evaluateAnswer(String userRequest) {
		SpeechletResponse res = null;
		recognizeUserIntent(userRequest);
		switch (ourUserIntent) {
		case Publikum: {
			if (publikumUsed) {
				res = askUserResponse(publikumUsedMsg);
			} else {
				publikumUsed = true;
				usePublikumJoker();
				res = askUserResponse(buildString(publikumAnswerMsg, answerOption1, answerOption2));
			}
		}; break; 
		case FiftyFifty: {
			if (fiftyfiftyUsed) {
				res = askUserResponse(fiftyfiftyUsedMsg);
			} else {
				fiftyfiftyUsed = true;
				useFiftyFiftyJoker();
				res = askUserResponse(buildString(fiftyfiftyAnswerMsg, answerOption1, answerOption2));
			}
		}; break; 
		default :{
			if (ourUserIntent.equals(UserIntent.A) 
					|| ourUserIntent.equals(UserIntent.B)
					|| ourUserIntent.equals(UserIntent.C)
					|| ourUserIntent.equals(UserIntent.D)	
					) {
				logger.info("User answer ="+ ourUserIntent.name().toLowerCase()+ "/correct answer="+correctAnswer);
				if (ourUserIntent.name().toLowerCase().equals(correctAnswer)) {
					logger.info("User answer recognized as correct.");
					increaseSum();
					if (sum == 1000000) {
						res = response(correctMsg+" "+congratsMsg+" "+goodbyeMsg);
					} else {
						recState = RecognitionState.YesNo;
						res = askUserResponse(correctMsg+" "+continueMsg);
					}
				} else {
					setfinalSum();
					res = response(wrongMsg+ " "+ sumMsg + " " +goodbyeMsg);
				}
			} else {
				res = askUserResponse(errorAnswerMsg);
			}
		}
		}
		return res;
	}

	private void setfinalSum() {
		if (sum <500){
			sum = 0;
		}else{
			if(sum <16000){
				sum = 500;
			}else{
				sum=16000;
			}
		}
		
	}

	private void increaseSum() {
		switch(sum){
		case 0: sum = 50; break;
		case 50: sum = 100; break;
		case 100: sum = 200; break;
		case 200: sum = 300; break;
		case 300: sum = 500; break;
		case 500: sum = 1000; break;
		case 1000: sum = 2000; break;
		case 2000: sum = 4000; break;
		case 4000: sum = 8000; break;
		case 8000: sum = 16000; break;
		case 16000: sum = 32000; break;
		case 32000: sum = 64000; break;
		case 64000: sum = 125000; break;
		case 125000: sum = 500000; break;
		case 500000: sum = 1000000; break;
		}
	}

	//TODO
	private void recognizeUserIntent(String userRequest) {
		switch (userRequest.toLowerCase()) {
		case "publikum": ourUserIntent = UserIntent.Publikum; break;
		case "fiftyfifty": ourUserIntent = UserIntent.FiftyFifty; break;
		case "a": ourUserIntent = UserIntent.A; break;
		case "b": ourUserIntent = UserIntent.B; break;
		case "c": ourUserIntent = UserIntent.C; break;
		case "d": ourUserIntent = UserIntent.D; break;
		case "ja": ourUserIntent = UserIntent.Yes; break;
		case "nein": ourUserIntent = UserIntent.No; break;
		}
		logger.info("set ourUserIntent to " +ourUserIntent);
	}

	//TODO
	private void useFiftyFiftyJoker() {
		answerOption1 = correctAnswer;
		answerOption2 = correctAnswer;
	}

	//TODO
	private void usePublikumJoker() {
		answerOption1 = correctAnswer;
	}

	/**
	 * formats the text in weird ways
	 * @param text
	 * @param i
	 * @return
	 */
	private SpeechletResponse responseWithFlavour(String text, int i) {

		SsmlOutputSpeech speech = new SsmlOutputSpeech();
		switch(i){ 
		case 0: 
			speech.setSsml("<speak><amazon:effect name=\"whispered\">" + text + "</amazon:effect></speak>");
			break; 
		case 1: 
			speech.setSsml("<speak><emphasis level=\"strong\">" + text + "</emphasis></speak>");
			break; 
		case 2: 
			String half1=text.split(" ")[0];
			String[] rest = Arrays.copyOfRange(text.split(" "), 1, text.split(" ").length);
			speech.setSsml("<speak>"+half1+"<break time=\"3s\"/>"+ StringUtils.join(rest," ") + "</speak>");
			break; 
		case 3: 
			String firstNoun="erstes Wort buchstabiert";
			String firstN=text.split(" ")[3];
			speech.setSsml("<speak>"+firstNoun+ "<say-as interpret-as=\"spell-out\">"+firstN+"</say-as>"+"</speak>");
			break; 
		case 4: 
			speech.setSsml("<speak><audio src='soundbank://soundlibrary/transportation/amzn_sfx_airplane_takeoff_whoosh_01'/></speak>");
			break;
		default: 
			speech.setSsml("<speak><amazon:effect name=\"whispered\">" + text + "</amazon:effect></speak>");
		} 

		return SpeechletResponse.newTellResponse(speech);
	}


	@Override
	public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope)
	{
		logger.info("Alexa session ends now");
	}



	/**
	 * Tell the user something - the Alexa session ends after a 'tell'
	 */
	private SpeechletResponse response(String text)
	{
		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(text);

		return SpeechletResponse.newTellResponse(speech);
	}

	/**
	 * A response to the original input - the session stays alive after an ask request was send.
	 *  have a look on https://developer.amazon.com/de/docs/custom-skills/speech-synthesis-markup-language-ssml-reference.html
	 * @param text
	 * @return
	 */
	private SpeechletResponse askUserResponse(String text)
	{
		SsmlOutputSpeech speech = new SsmlOutputSpeech();
		speech.setSsml("<speak>" + text + "</speak>");

		// reprompt after 8 seconds
		SsmlOutputSpeech repromptSpeech = new SsmlOutputSpeech();
		repromptSpeech.setSsml("<speak><emphasis level=\"strong\">Hey!</emphasis> Bist du noch da?</speak>");

		Reprompt rep = new Reprompt();
		rep.setOutputSpeech(repromptSpeech);

		return SpeechletResponse.newAskResponse(speech, rep);
	}


}
