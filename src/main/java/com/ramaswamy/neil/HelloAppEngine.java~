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
	static String[][] getForms(String verb) {
		String formArray[][] = new String[30][6];
		try {
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
	
	public void writeQuestion(String question, String key, PrintWriter out) {
		out.println(question + "</br>");
		out.println("<form action=\"hello\">");
		out.println("<input type=\"text\" name=\"" + key + "\"><br/>");
		out.println("<input type=\"submit\" value=\"Submit\"></form>");
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		
		response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<body bgcolor=\"white\">");

		Map<String, String[]> pMap = request.getParameterMap();
		if (pMap.size() != 0) { // we got some parameters
			String key = (String)pMap.keySet().toArray()[0];
			String verb = pMap.get(key)[0];
			String[][] foo = getForms(verb);
			out.println("<b>Hello, Neil! How goes it?!<br/>");
			for (int i = 0; i < foo[0].length; i++) {
				out.println("<br/>" + foo[1][i]);
			}
		} else {
			writeQuestion("Please input verb to quiz on:", "verb", out);
		}
		
		/*
		*/
		out.println("</body>");
        out.println("</html>");

	}
}
