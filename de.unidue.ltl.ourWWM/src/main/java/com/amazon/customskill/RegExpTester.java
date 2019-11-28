package com.amazon.customskill;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class RegExpTester {


	
	public static void main(String[] args) throws IOException {	
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		AlexaSkillSpeechlet ass = new AlexaSkillSpeechlet();
		while (true) {
			System.out.print("Pattern: ");
			String eingabe = br.readLine();
			ass.recognizeUserIntent(eingabe);
			System.out.println("erkannt: "+ass.ourUserIntent);
		}
	}










}
