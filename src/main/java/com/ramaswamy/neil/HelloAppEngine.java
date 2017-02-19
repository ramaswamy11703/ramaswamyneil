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
import java.security.GeneralSecurityException;
import java.util.Collections;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

public class HelloAppEngine extends HttpServlet {


	private String CLIENT_ID = "337204890997-t1llinp0166leg6h79o564sk47o33drh.apps.googleusercontent.com";

	private static final String ACTION_PARAM = "a";
	private static final String VERBIN_PARAM = "verbIn";
	private static final String QUIZ_PARAM = "quiz";
	private static final String SCORE_PARAM = "score";
	private static final String STENSE_PARAM = "sTenses";
	private static final String VERB_PARAM = "verb";

	// Represents server state and is initialized very early in the servlet (doGet() )
	// Controls the different kinds of pages that we render.
	enum ServerMode {Initial, VerbInput, Quiz, Score};

	/**
	 * Subject forms to quiz on. Should this be generated dynamically from verbMap?	
	 */
	String subjArray[] = {"yo", "tú", "él, ella, Ud.", "nosotros", "vosotros", "ellos, ellas, Uds."};




	/**
	 * This is a static map that remembers the verb charts we have seen so far. (And lazily loads new ones
	 * as needed.
	 */
	private static HashMap<String, VerbChartAccessor> verbMap = new HashMap<String, VerbChartAccessor>();

	/**
	 * These two are initialized as soon as know what verb we are dealing with. (by initializeVerbMap() )
	 */
	private VerbChartAccessor verbChart = null;
	private String[] tenses = null;

	/**
	 * Helper classes for generating HTML with a little less pain
	 *
	 */
	private class HtmlElement {
		String type;
		HashMap<String, String> attributes = new HashMap<String, String>();
		HtmlElement(String type) {
			this.type = type;
		}
		void addAttribute(String name, String val) {
			attributes.put(name, val);
		}
		void setClass(String className) {
			addAttribute("class", className);
		}
		void setID(String id) {
			addAttribute("id", id);
		}
		public String toString() {
			String attrs = "";
			for (String key: attributes.keySet()) {
				attrs = attrs + " " + key + "=\"" + attributes.get(key) + "\"";
			}
			return "<" + type + attrs + ">";
		}
	}

	private class HtmlInput extends HtmlElement {
		HtmlInput(String name, String type)
		{
			super("input"); 
			this.addAttribute("name", name);
			this.addAttribute("type", type);
		}
		HtmlInput(String type) {
			super("input");
			this.addAttribute("type", type);
		}
		void addValue(String val) {
			addAttribute("value", val);
		}
	}

	private class HtmlTextInput extends HtmlInput {
		HtmlTextInput(String name, String value, String className) {
			super(name, "text");
			addValue(value);
			setClass(className);
		}
		HtmlTextInput(String name, String value) {
			super(name, "text");
			addValue(value);
		}
		HtmlTextInput(String name) {
			super(name, "text");
		}
	}

	private class HtmlHidden extends HtmlInput {
		HtmlHidden(String name, String value) {
			super(name, "hidden");
			this.addAttribute("value", value);
		}
	}

	private class HtmlSubmit extends HtmlInput {

		HtmlSubmit(String value, String className) {
			super("submit");
			this.addValue(value);
			this.setClass(className);
		}

	}

	// *********************************************************************************
	// Section of helper methods for generating fancier HTML widgets.
	/**
	 * fancy radio button
	 */
	private String generateRadioButton(String optionNumber, String value, boolean checked) {
		String checkedString = (checked ? "checked" : "");
		return "<label class=\"mdl-radio mdl-js-radio\" for=\"" + optionNumber + "\"> "
		+ "<input type=\"radio\" id=\"" + optionNumber + "\" value=\"" + value + 
		"\"name=\"" + STENSE_PARAM + "\" class=\"mdl-radio__button\"" +  checkedString + "> "
		+ "<span class=\"mdl-radio__label\">" + value + "</span> </label>";
	}

	private void writeFormHeading(PrintWriter out) {
		out.println("<form action=\"quiz\" method=\"post\">");
	}

	private String getResetButton() {
		return "<a href=\"./quiz\" class=\"" + getSubmitClass() + "\">Reset</a>";
	}
	private String getSubmitClass() {
		return "mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent";
	}
	private void generateHeader(String text, PrintWriter out) {
		out.println("<div class=\"mdl-layout mdl-js-layout mdl-layout--fixed-header\">");
		out.println("<header class=\"mdl-layout__header\">");
		out.println("<div class=\"mdl-layout__header-row\">");
		out.println("<span class=\"mdl-layout-title\">");
		out.println("<h3 style=\"font-family: 'Josefin Slab', serif\";>" + text + "</h3>");
		out.println("</span></div></header></div><br/><br/><br/>");	
	}
	
	private void generateSimpleHeader(String text, PrintWriter out) {
		out.println("<h4>" + text + "</h4>");
	}
	private void writeTextInput(String key, PrintWriter out) {
		out.println("<div class=\"mdl-textfield mdl-js-textfield mdl-textfield--floating-label\">");
		HtmlTextInput ht = new HtmlTextInput(key); ht.setClass("mdl-textfield__input"); ht.setID(key);
		out.println(ht.toString());

		out.println("<label class=\"mdl-textfield__label\" for=\"" + key + "\">Verb...</label>");
		out.println("</div>");
	}

	public void writeActionType(String a, PrintWriter out) {
		out.println("<input type=\"hidden\" name=\"" + ACTION_PARAM + "\" value=\"" + a + "\">");
	}

	private void setGoogleLogin(PrintWriter out) {
		out.println("<script src=\"https://apis.google.com/js/platform.js\" async defer></script>");
		out.println("<meta name=\"google-signin-client_id\" content=\"" + CLIENT_ID + "\">");
		out.println("<script>function onSignIn(googleUser) " + 
				"{var id_token = googleUser.getAuthResponse().id_token; " + 
				"var idParam = document.getElementById(\"loginId\"); idParam.value = id_token;};</script>");
	}

	/**
	 * Renders the page that asks the user to input a verb and pick a subject tense
	 */
	public void writeQuestionForm(PrintWriter out) {
		String question = "Please input verb to quiz on:";
		this.writeFormHeading(out);
		writeActionType(VERBIN_PARAM, out);
		out.println("<table><tr>");
		out.println("<td><h3>Verb:</h3></td>");
		
		out.println("<td>");
		writeTextInput(VERB_PARAM, out);
		out.println("</td></tr>");
		out.println("<tr><td><h3>Subject:</h3></td>");
		out.println("<td>");
		for (int i = 0; i < subjArray.length; i++) {
			String optionNumber = "option" + String.valueOf(i);
			out.println(generateRadioButton(optionNumber, subjArray[i], subjArray[i].equalsIgnoreCase("yo")));
		}
		out.println("</td></tr>");
		
		out.println("<tr><td style=\"align:center\" conSpan=\"2\">");
		HtmlSubmit hs = new HtmlSubmit("¡Vamos!", getSubmitClass());
		out.println(hs.toString());
		out.println("</td></tr></table>");
		out.println("</form>"); 
	}

	
	String[] getThingsToQuizOn(String subject, String verb) {
		HashMap<String, HashMap<String, String>> chart = verbChart.getVerbChartMap();
		if (chart.isEmpty()) return null;

		String[] toReturn = new String[tenses.length];
		for (int i = 0; i < tenses.length; i++) {
			String currentVerbalTense = tenses[i];
			System.out.println(currentVerbalTense);
			HashMap<String, String> subjectMap = chart.get(currentVerbalTense);
			System.out.println(subjectMap.size());
			String toAdd = subjectMap.get(subject);
			System.out.println(toAdd);
			toReturn[i] = toAdd;
		}
		for (String s: toReturn) {
			System.out.println("$$" + s);
		}
		return toReturn;
	}

	/**
	 * Renders the page that shows the actual quiz for a verb. And in case the user
	 * inputs incorrect answers, shows the same page with error boxes for inputs.
	 */
	public void writeQuizForm(String[] answers, Map<String, String> userAnswers, 
			String subject, String verb, PrintWriter out) {
		writeFormHeading(out);
		writeActionType(SCORE_PARAM, out);
		out.println(new HtmlHidden(STENSE_PARAM, subject).toString());
		out.println(new HtmlHidden(VERB_PARAM, verb).toString());

		boolean hasAnswers = (userAnswers != null);
		out.println("<table class=\"mdl-data-table mdl-js-data-table\">");
		for (int i = 0; i < tenses.length; i++) {
			out.println("<tr>");

			out.println("<td><b>" + tenses[i] + ":</b></td>");
			String formKey = tenses[i] + "-form";
			out.println("<td>");


			HtmlTextInput hti = new HtmlTextInput(formKey);
			if (hasAnswers) {
				String userAnswer = userAnswers.get(formKey);
				if (userAnswer == null) userAnswer = "";
				System.out.println("User answer: " + userAnswer + ", correct: " + answers[i]);
				hti.addAttribute("value", userAnswer);
				if (!userAnswer.equalsIgnoreCase(answers[i])) {
					hti.setClass("mdl-textfield__input redBorder");
				} else {
					hti.setClass("mdl-textfield__input greenBorder");
				}
			} else {
				hti.setClass("mdl-textfield__input");
			}
			out.println(hti.toString());
			out.println("</td>");
			out.println(new HtmlHidden(tenses[i], answers[i]).toString());
			out.println("</tr>");
		}
		out.println("</table>");
		HtmlSubmit hs = new HtmlSubmit("Submit", getSubmitClass());
		out.println(hs.toString() + "&nbsp;");
		out.println(getResetButton());
		out.println("</form>");
	}


	// *********************************************************************************
	// Helper methods called early on in servlet initialization
	
	private String verifyGoogleCredentials(String idTokenString) throws IOException {
		try {
			System.out.println("id token string = " + idTokenString);
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
					.setAudience(Collections.singletonList(CLIENT_ID))
					// Or, if multiple clients access the backend:
					//.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
					.build();

			// (Receive idTokenString by HTTPS POST)

			GoogleIdToken idToken = verifier.verify(idTokenString);
			if (idToken != null) {
				Payload payload = idToken.getPayload();

				// Print user identifier
				String userId = payload.getSubject();
				System.out.println("User ID: " + userId);

				// Get profile information from payload
				String email = payload.getEmail();
				boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
				String name = (String) payload.get("given_name");
				System.out.println("login email: " + email);
				return name;

				// Use or store profile information
				// ...

			} else {
				System.out.println("Invalid ID token.");
				return "";
			}
		} catch (GeneralSecurityException gse) {
			throw new IOException(gse.getMessage());
		}
	}

	/**
	 * Makes sure that we have the verb tense -> <subject tense -> conjugation> map
	 */
	void initializeVerbMap(String verb) {
		VerbChartAccessor vac = verbMap.get(verb);
		if (vac == null) {
			vac = VerbChartAccessor.getConjugations(verb);
			verbMap.put(verb, vac);
		}
		this.verbChart = vac;
		tenses = vac.getAllVerbalTenses().toArray(new String[vac.getAllVerbalTenses().size()]);

	}

	// Servlet map is from string to array of strings, which is annoying. Plus, it
	// immutable.
	Map<String, String> getCleanParamMap(Map<String, String[]> paramMap) {
		Map<String, String[]> pMap = paramMap;
		Map<String, String> pNewMap = new HashMap<String, String>();
		for (String key : pMap.keySet()) {
			pNewMap.put(key,  pMap.get(key)[0]);
		}
		return pNewMap;
	}

	/**
	 * Figure out what state we are in.
	 */
	private ServerMode getMode(Map<String, String> pMap) {
		ServerMode sMode = ServerMode.Initial;
		if (pMap.size() > 0) {
			String action = pMap.remove(ACTION_PARAM);
			if (action != null) {
				switch (action) {
				case VERBIN_PARAM:
					sMode = ServerMode.VerbInput; break;
				case QUIZ_PARAM:
					sMode = ServerMode.Initial; break;
				case SCORE_PARAM:
					sMode = ServerMode.Score; break;
				default:
				}
			}
		}
		return sMode;
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws IOException, ServletException {
		doGet(request, response);
	}


	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws IOException, ServletException {
		response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();

		out.println("<html>");
		out.println("<meta charset=\"utf-8\">");

		
		// setGoogleLogin(out);
		out.println("<body>");
		// All CSS styling is included here. (File is in src/main/webapp/heading.html)
		request.setCharacterEncoding("UTF-8");
		RequestDispatcher req = request.getRequestDispatcher("/heading.html");
		if (req != null) req.include(request, response);
				
		
		// key initialization steps that lets us decide what to do.
		Map<String, String> pNewMap = getCleanParamMap(request.getParameterMap());

		ServerMode sMode = getMode(pNewMap);
		String verb = pNewMap.remove(VERB_PARAM); 
		String subject = pNewMap.remove(STENSE_PARAM);

		String loginId = pNewMap.get("loginId"); 
		if (loginId == null) loginId = "";
		System.out.println(loginId);
		if (!loginId.equals("")) {
			loginId = verifyGoogleCredentials(loginId);
		}

		if (verb != null) {
			// get the hashmap of all the appropriate tenses and their conjugations
			// This HashMap goes from the verbal tense to a HashMap of subject tense to appropriate conjugation.
			// get the right ones and put them in a string array
			// return said string array
			this.initializeVerbMap(verb);
		}
		
		switch (sMode) {
		case Initial:
			// generateHeader("Welcome to project <i> Tres Leches Mañana.</i>", out);
			writeQuestionForm(out);
			break;

		case VerbInput:
			String[] answers = getThingsToQuizOn(subject, verb);
			if (answers == null) {
				// generateHeader("Welcome to project <i> Tres Leches Mañana.</i>", out);
				generateSimpleHeader("The verb " + verb + " is not currently supported.", out);
				writeQuestionForm(out);
			} else {
				//generateHeader("Here is your quiz for " + verb + " in the <i> " + 
				//		subject.toLowerCase() + "</i> form, amigo " + loginId + "!", out);
				writeQuizForm(answers, null, subject, verb, out);
			}
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
		if (correctAnswers < tenses.length) {
			String[] answers = getThingsToQuizOn(subject, verb);
			generateSimpleHeader("You got " + correctAnswers + " out of " + tenses.length + " right. Fix the errors!", out);
			writeQuizForm(answers, pMap, subject, verb, out);
		} else {
			generateSimpleHeader("Congratulations! You got " + correctAnswers + " out of " + tenses.length + " right!", out);
			out.println(getResetButton());
		}

	}
}
