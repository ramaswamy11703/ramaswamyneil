/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ramaswamy.neil;



import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HelloAppEngine extends HttpServlet {


	String[] keys = {"verb", "conjugation"};
	String subjArray[] = {"yo", "tu", "Ud.", "Nos.", "Vos.", "Uds."};
	enum ServerMode {Initial, VerbInput, Quiz, Score};
	private static String[] tenses = {"presente", "futuro", "imperfecto", "pretérito", "condicional", "presente perfecto", "futuro perfecto", "pluscuamperfecto", "condicional perfecto", 
			"presente subj", "imperfecto subj", "presente perfecto subj","pluscuam perfecto subj"};
	
	static String[][] getForms(String verb) {
		String formArray[][] = new String[30][6];
		try {// hello
			// http://users.ipfw.edu/JEHLE/COURSES/verbs/ACEPTAR.HTM                                      
			Document doc = Jsoup.connect("http://users.ipfw.edu/JEHLE/COURSES/verbs/" + verb + ".HTM").get();
			// https://jsoup.org/apidocs/org/jsoup/select/Selector.html is cool!                          
			Element table = doc.select("table").get(1); // second table                                   
			// header row                                                                                 
			Elements rows = table.select("tr");
			String mood = "";
			int rowIndex = 0;
			for (int i = 1; i < rows.size(); i++) { //first row is the col names so skip it.              
				Element row = rows.get(i);
				Elements cols = row.select("td");
				if (cols.size() == 1) {
					mood = cols.get(0).text();
				}
				if (cols.get(0).text().equals(" ")) continue; // header2 row                              
				String tense = cols.get(0).text();
				for (int j = 2; j < cols.size(); j++) {
					formArray[rowIndex][j - 2] = cols.get(j).text();
				}
				rowIndex++;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return formArray;
	}

	public void writeQuestionForm(String question, String key, PrintWriter out) {
		this.writeFormHeading(out);
		writeActionType("verbIn", out);
		out.println("<b>" + question + "</b>");
		out.println("<input type=\"text\" name=\"" + key + "\"><br/><br/>");
		out.println("<b>Pick your subject pronoun: </b>");
		for (int i = 0; i < subjArray.length; i++) {
			String s = "<input type=\"radio\" name=\"sTenses\" value=\"" + subjArray[i] + 
					"\"" + (i == 0 ? " checked> " : "> ") + subjArray[i];
			out.println(s);
		}
		out.println("<br/>");
		out.println("<input type=\"submit\" value=\"Submit\"></form>"); 
	}

	public void writeActionType(String a, PrintWriter out) {
		out.println("<input type=\"hidden\" name=\"a\" value=\"" + a + "\">");
	}
	
	/**
	 * Generate an array of 13 things to quiz the user on
	 * @param verb
	 * @param subject
	 * @param allForms
	 * @return
	 */
	String[] getThingsToQuizOn(String subject, String[][] allForms) {
	    String[] toReturn = new String[13];

	    int subjectIndex = 0; // yo: 0, tú: 1, Ud. : 2, Nos. : 3, Vos. : 4, Uds. : 5
	    for (int i = 0; i < 6; i++) {
	    	if (subject.equals(subjArray[i])) subjectIndex = i;
	    }

	    int index = 0;
	    for (int i = 1; i < 18; i++) {
	    	if ((i == 9) || (i == 11) || (i == 14) || (i == 16) ) continue;
	    	toReturn[index++] = allForms[i][subjectIndex];
	    }
	  return toReturn;
	}
	
	private void writeFormHeading(PrintWriter out) {
		out.println("<form action=\"hello\" method=\"post\">");
	}
	
	public void writeQuizForm(String[] answers, Map<String, String> userAnswers, 
			String subject, String verb, PrintWriter out) {
		writeFormHeading(out);
		writeActionType("score", out);
		out.println("<input type=\"hidden\" name=\"sTenses\" value=\"" + subject + "\">");
		out.println("<input type=\"hidden\" name=\"verb\" value=\"" + verb + "\">");
		boolean hasAnswers = (userAnswers != null);
		for (int i = 0; i < tenses.length; i++) {
			out.println("<b>" + tenses[i] + ":</b>&nbsp;");
			String formKey = tenses[i] + "-form";
			out.println("<input type=\"text\" name=\"" + formKey + "\"");
			if (hasAnswers) {
				String userAnswer = userAnswers.get(formKey);
				if (userAnswer == null) userAnswer = "";
				out.println("value=\"" + userAnswer + "\"");
				System.out.println("User answer: " + userAnswer + ", correct: " + answers[i]);
				if (!userAnswer.equalsIgnoreCase(answers[i])) {
					printAttribute("class", "redBorder", out);
				} else {
					printAttribute("class", "greenBorder", out);
				}
			}
			out.println("><br/>");
			out.println("<input type=\"hidden\" name=\"" + tenses[i] + "\" value=\"" +
			answers[i] + "\">");
		}
		out.println("<input type=\"submit\" value=\"Submit\"></form>");
	}
	
	private void printAttribute(String name, String value, PrintWriter out) {
		out.println(name + "=\"" + value + "\"");
	}
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		doGet(request, response);
	}
	
	public void writeStyles(PrintWriter out) {
		out.println("<style>");
		out.println("input.redBorder { border: 1px	solid red; }");
		out.println("input.greenBorder { border: 1px solid green; }");
		out.println("</style>");;
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws IOException {

		response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();

		out.println("<html>");
		writeStyles(out);
		out.println("<body bgcolor=\"white\">");

		Map<String, String[]> pMap = request.getParameterMap();
		Map<String, String> pNewMap = new HashMap<String, String>();
		for (String key : pMap.keySet()) {
			pNewMap.put(key,  pMap.get(key)[0]);
		}
	
		ServerMode sMode = getMode(pNewMap);
		String verb = pNewMap.remove("verb"); 
		String subject = pNewMap.remove("sTenses");
		
		switch (sMode) {
		case Initial:
			out.println("Welcome to Neil's Spanish Quiz.<br/>");
			writeQuestionForm("Please input verb to quiz on:", "verb", out);
			break;
		case VerbInput:
			String[] answers = getThingsToQuizOn(subject, getForms(verb));
			// Generate output form
			out.println("<b>Here is your quiz for verb: " + verb + "&nbsp;subject form: " + subject + "</b><br/>");
			writeQuizForm(answers, null, subject, verb, out);
			
			break;
		case Score:
			generateResults(pNewMap, verb, subject, out);
		}

		out.println("</body>");
		out.println("</html>");

	}
	
	private void generateResults(Map<String, String> pMap, String verb, String subject, PrintWriter out) {
		int correctAnswers = 0;
		for (String key : pMap.keySet()) {
			String answer = pMap.get(key);
			String userInput = pMap.get(key + "-form");
			if (answer.equalsIgnoreCase(userInput)) correctAnswers++;
		}
		if (correctAnswers < 13) {
			String[] answers = getThingsToQuizOn(subject, getForms(verb));
			out.println("You got " + correctAnswers + " out of 13 right. Fix the errors!");
			writeQuizForm(answers, pMap, subject, verb, out);
		} else {
			out.println("Congratulations! You got " + correctAnswers + " out of 13 right!");
		}
	}

	/**
	 * Figure out which form we should render
	 * @param pMap
	 * @return
	 */
	private ServerMode getMode(Map<String, String> pMap) {
		ServerMode sMode = ServerMode.Initial;
		if (pMap.size() > 0) {
			String action = pMap.remove("a");
			if (action != null) {
				switch (action) {
				case "verbIn":
					sMode = ServerMode.VerbInput; break;
				case "quiz":
					sMode = ServerMode.Initial; break;
				case "score":
					sMode = ServerMode.Score; break;
				default:
				}
			}
		}
		return sMode;
	}
}
