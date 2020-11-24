/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.amazon.customskill;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

	static int sum;
	static String answerOption1 = "";
	static String answerOption2 = "";
	static boolean publikumUsed;
	static boolean fiftyfiftyUsed;
	static String question = "";
	static String correctAnswer = "";
	static enum RecognitionState {Answer, YesNo};
	RecognitionState recState;
	static enum UserIntent {Yes, No, A, B, C, D, Publikum, FiftyFifty, Error};
	UserIntent ourUserIntent;

	Map<String, String> utterances;

	private String buildString(String msg, String replacement1, String replacement2) {
		return msg.replace("{replacement}", replacement1).replace("{replacement2}", replacement2);
	}

	private Map<String, String> readSystemUtterances() {
		Map<String, String> utterances = new HashMap<String, String>(); 
		try {
			for (String line :Files.readAllLines(Paths.get("src/main/resources/utterances.txt"))){
				if (line.startsWith("#")){
					continue;	
				}
				String[] parts = line.split("=");
				String key = parts[0].trim();
				String utterance = parts[1].trim();
				utterances.put(key, utterance);
			}
			logger.info("Read "  +utterances.keySet().size() + "utterances");
		} catch (IOException e) {
			logger.info("Could not read utterances: "+e.getMessage());
			System.err.println("Could not read utterances: "+e.getMessage());
		}
		return utterances;
	}

	static String DBName = "AlexaBeispiel.db";
	private static Connection con = null;

	@Override
	public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope)
	{
		utterances = readSystemUtterances();
		logger.info("Alexa session begins");
		publikumUsed = false;
		fiftyfiftyUsed = false;
		sum = 0;
		recState = RecognitionState.Answer;

	}

	@Override
	public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope)
	{
		logger.info("onLaunch");
		selectQuestion();
		return askUserResponse(utterances.get("welcomeMsg")+" "+question);
	}
	
	private void selectQuestion() {
		try {
			con = DBConnection.getConnection();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt
					.executeQuery("SELECT * FROM Fragen WHERE Gewinnsumme=" + sum + "");
			question = rs.getString("Frage");
			correctAnswer = rs.getString("RichtigeAntwort");
			logger.info("Extracted question from database "+ question);
		} catch (Exception e){
			e.printStackTrace();
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
			res = response(buildString(utterances.get("sumMsg"), String.valueOf(sum), "")+" "+utterances.get("goodbyeMsg")); break;
		} default: {
			res = askUserResponse(utterances.get(""));
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
				res = askUserResponse(utterances.get("publikumUsedMsg"));
			} else {
				publikumUsed = true;
				usePublikumJoker();
				res = askUserResponse(buildString(utterances.get("publikumAnswerMsg"), answerOption1, answerOption2));
			}
		}; break; 
		case FiftyFifty: {
			if (fiftyfiftyUsed) {
				res = askUserResponse(utterances.get("fiftyfiftyUsedMsg"));
			} else {
				fiftyfiftyUsed = true;
				useFiftyFiftyJoker();
				res = askUserResponse(buildString(utterances.get("fiftyfiftyAnswerMsg"), answerOption1, answerOption2));
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
						res = response(utterances.get("correctMsg")+" "+utterances.get("congratsMsg")+" "+utterances.get("goodbyeMsg"));
					} else {
						recState = RecognitionState.YesNo;
						res = askUserResponse(utterances.get("correctMsg")+" "+utterances.get("continueMsg"));
					}
				} else {
					setfinalSum();
					res = response(utterances.get("wrongMsg")+ " "+ utterances.get("sumMsg") + " " + utterances.get("goodbyeMsg"));
				}
			} else {
				res = askUserResponse(utterances.get("errorAnswerMsg"));
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


	void recognizeUserIntent(String userRequest) {
		userRequest = userRequest.toLowerCase();
		String pattern1 = "(ich nehme )?(antwort )?(\\b[a-d]\\b)( bitte)?";
		String pattern2 = "(ich nehme )?(den )?publikumsjoker( bitte)?";
		String pattern3 = "(ich nehme )?(den )?(fiftyfifty|fünfzigfünfzig) joker( bitte)?";
		String pattern4 = "\\bnein\\b";
		String pattern5 = "\\bja\\b";

		Pattern p1 = Pattern.compile(pattern1);
		Matcher m1 = p1.matcher(userRequest);
		Pattern p2 = Pattern.compile(pattern2);
		Matcher m2 = p2.matcher(userRequest);
		Pattern p3 = Pattern.compile(pattern3);
		Matcher m3 = p3.matcher(userRequest);
		Pattern p4 = Pattern.compile(pattern4);
		Matcher m4 = p4.matcher(userRequest);
		Pattern p5 = Pattern.compile(pattern5);
		Matcher m5 = p5.matcher(userRequest);
		if (m1.find()) {
			String answer = m1.group(3);
			switch (answer) {
			case "a": ourUserIntent = UserIntent.A; break;
			case "b": ourUserIntent = UserIntent.B; break;
			case "c": ourUserIntent = UserIntent.C; break;
			case "d": ourUserIntent = UserIntent.D; break;
			}
		} else if (m2.find()) {
			ourUserIntent = UserIntent.Publikum;
		} else if (m3.find()) {
			ourUserIntent = UserIntent.FiftyFifty;
		} else if (m4.find()) {
			ourUserIntent = UserIntent.No;
		} else if (m5.find()) {
			ourUserIntent = UserIntent.Yes;
		} else {
			ourUserIntent = UserIntent.Error;
		}
		logger.info("set ourUserIntent to " +ourUserIntent);
	}


	void useFiftyFiftyJoker() {
		answerOption1 = correctAnswer;
		do { int r = (int) Math.round(Math.random()*4.0);
		switch(r){
		case 1: answerOption2="a"; break;
		case 2: answerOption2="b"; break;
		case 3: answerOption2="c"; break;		
		default:answerOption2="d";
		}
		} while(answerOption2==answerOption1);
		if (correctAnswer=="d" || answerOption2 == "a"
				|| (answerOption1 == "c" && answerOption2!="d")) {
			String temp = answerOption1;
			answerOption1 = answerOption2;
			answerOption2 = temp;
		}
	}

	void usePublikumJoker() {
		int r = (int) Math.round(Math.random()*20.0);
		if (r < 1.0) {
			answerOption1 = "a";
		} else if (r < 2.0) {
			answerOption1 = "b";
		} else if (r < 3.0) {
			answerOption1 = "c";
		} else if (r < 4.0) {
			answerOption1 = "d";
		} else {
			answerOption1 = correctAnswer;
		}
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
