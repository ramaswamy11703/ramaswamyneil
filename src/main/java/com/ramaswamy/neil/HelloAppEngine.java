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
	String subjArray[] = {"Yo", "Tú", "Él / Ella / Ud.", "Nos.", "Vos.", "Uds."};
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
	
	private class HElement {
		String type;
		HashMap<String, String> attributes = new HashMap<String, String>();
		HElement(String type) {
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

	private class HInput extends HElement {
		HInput(String name, String type)
		{
			super("input"); 
			this.addAttribute("name", name);
			this.addAttribute("type", type);
		}
		HInput(String type) {
			super("input");
			this.addAttribute("type", type);
		}
		void addValue(String val) {
			addAttribute("value", val);
		}
	}

	private class HTextInput extends HInput {
		HTextInput(String name, String value, String className) {
			super(name, "text");
			addValue(value);
			setClass(className);
		}
		HTextInput(String name, String value) {
			super(name, "text");
			addValue(value);
		}
		HTextInput(String name) {
			super(name, "text");
		}
	}

	private class HHidden extends HInput {
		HHidden(String name, String value) {
			super(name, "hidden");
			this.addAttribute("value", value);
		}
	}
	
	private class HSubmit extends HInput {

		HSubmit(String value, String className) {
			super("submit");
			this.addValue(value);
			this.setClass(className);
		}

	}

	public void writeQuestionForm(String question, String key, PrintWriter out) {
		this.writeFormHeading(out);
		writeActionType("verbIn", out);
		out.println(question);
		writeTextInput(key, out);
		out.println("Pick your subject pronoun: ");
		for (int i = 0; i < subjArray.length; i++) {
			String optionNumber = "option" + String.valueOf(i);
			String checkedOrNot = "";
			if (subjArray[i].equalsIgnoreCase("yo")) checkedOrNot = "checked";
			String s = "<label class=\"mdl-radio mdl-js-radio\" for=\"" + optionNumber + "\"> "
					+ "<input type=\"radio\" id=\"" + optionNumber + "\" value=\"" + subjArray[i] + "\"name=\"sTenses\" class=\"mdl-radio__button\"" +  checkedOrNot + "> "
							+ "<span class=\"mdl-radio__label\">" + subjArray[i]+ "</span> </label>";
			out.println(s);
		}
		out.println("<br/>");
		HSubmit hs = new HSubmit("Let's go!", getSubmitClass());
		out.println(hs.toString());
		out.println("</form>"); 
	}

	private void writeTextInput(String key, PrintWriter out) {
		out.println("<div class=\"mdl-textfield mdl-js-textfield mdl-textfield--floating-label\">");
		HTextInput ht = new HTextInput(key); ht.setClass("mdl-textfield__input"); ht.setID(key);
		out.println(ht.toString());
		
	    out.println("<label class=\"mdl-textfield__label\" for=\"" + key + "\">Verb...</label>");
	    out.println("</div><br/>");
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
	    
	    if (allForms[1][0] == null) return null;

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
	
	private void generateHeader(String text, PrintWriter out) {
		out.println("<div class=\"mdl-layout mdl-js-layout mdl-layout--fixed-header\">");
		out.println("<header class=\"mdl-layout__header\">");
		out.println("<div class=\"mdl-layout__header-row\">");
		out.println("<span class=\"mdl-layout-title\">");
		out.println("<h3 style=\"font-family: 'Josefin Slab', serif\";>" + text + "</h3>");
		out.println("</span></div></header></div><br/><br/><br/>");	}
	
	private void writeFormHeading(PrintWriter out) {
		out.println("<form action=\"hello\" method=\"post\">");
	}
	
	public void writeQuizForm(String[] answers, Map<String, String> userAnswers, 
			String subject, String verb, PrintWriter out) {
		writeFormHeading(out);
		writeActionType("score", out);
		out.println(new HHidden("sTenses", subject).toString());
		out.println(new HHidden("verb", verb).toString());
		
		boolean hasAnswers = (userAnswers != null);
		out.println("<table class=\"mdl-data-table mdl-js-data-table\">");
		for (int i = 0; i < tenses.length; i++) {
			out.println("<tr>");
			
			out.println("<td><b>" + tenses[i] + ":</b></td>");
			String formKey = tenses[i] + "-form";
			out.println("<td>");
			
			
			HTextInput hti = new HTextInput(formKey);
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
			out.println(new HHidden(tenses[i], answers[i]).toString());
			out.println("</tr>");
		}
		out.println("</table>");
		HSubmit hs = new HSubmit("Submit", getSubmitClass());
		out.println(hs.toString() + "&nbsp;");
		out.println("<a href=\"./hello\" class=\"" + getSubmitClass() + "\">Reset</a>");
		out.println("</form>");
	}
	
	private String getSubmitClass() {
		return "mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent";
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		doGet(request, response);
	}
	
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
	

		response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();

		out.println("<html>");
		
		
		out.println("<link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/icon?family=Material+Icons\">");
		out.println("<link rel=\"stylesheet\" href=\"https://code.getmdl.io/1.3.0/material.cyan-pink.min.css\">");
		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"http://fonts.googleapis.com/css?family=Josefine+Slab\">");
		out.println("<script defer src=\"https://code.getmdl.io/1.3.0/material.min.js\"></script>");
		out.println("<style> body { font-family: 'Josefin Slab', serif; margin-left: 20px; margin-top: 20px; } </style>");
		out.println("<style> header { width: 100%; top: 0; position: fixed; left: 0; }");
		out.println("<style> h3 { font-family: 'Josefin Slab', serif; } ");
		out.println("<style> input { outline:none } </style>");
		out.println("<style>input.redBorder { border-bottom: 2px	solid red; border-radius: 8px; }</style>");
		out.println("<style>input.greenBorder { border-bottom: 2px solid green;  border-radius: 8px; }</style>");
		out.println("<body>");
		
		// out.println("<style> body { text-align:center; } </style>");
		

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
			generateHeader("Welcome to project <i> Tres Leches Mañana.</i>", out);
			
			writeQuestionForm("Please input verb to quiz on:", "verb", out);
			break;
		case VerbInput:
			String[] answers = getThingsToQuizOn(subject, getForms(verb));
			if (answers == null) {
				generateHeader("Welcome to project <i> Tres Leches Mañana.</i>", out);
				out.println("</br>The verb " + verb + " is not currently supported.");
				writeQuestionForm("Please input verb to quiz on:", "verb", out);
			} else {
			// Generate output form
			out.println("<h3 style=\"font-family: 'Josefin Slab', serif;\">Here is your quiz for " + verb + " in the <i> " + subject.toLowerCase() + "</i> form:</h3>");
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
