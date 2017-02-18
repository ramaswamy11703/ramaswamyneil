package com.ramaswamy.neil;
import java.util.ArrayList;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class VerbChartAccessor {
	
	
	
	private ArrayList<String> allVerbalTenses = new ArrayList<String>(16);
	//This HashMap goes from the verbal tense to a HashMap of subject tense to appropriate conjugation.
	private HashMap<String, HashMap<String, String>> verbChartMap = new HashMap<String, HashMap<String, String>>();
	
	private VerbChartAccessor() {
	}
	

	public ArrayList<String> getAllVerbalTenses() {
		return allVerbalTenses;
	}


	public HashMap<String, HashMap<String, String>> getVerbChartMap() {
		return verbChartMap;
	}


	public static VerbChartAccessor getConjugations(String verb) {
		VerbChartAccessor vac = new VerbChartAccessor();
		try {
			
			Document doc = Jsoup.connect("http://www.wordreference.com/conj/ESverbs.aspx?v=" + verb).get();
			// https://jsoup.org/apidocs/org/jsoup/select/Selector.html is cool! table[class^=\"neoConj\"]
			
			String callType = "";
			Elements tables = doc.select("table[class^=\"neoConj\"");
			for (Element table: tables) {
				Element tableSibling = table.previousElementSibling();
				String tagName = tableSibling.tagName();
				if (tagName.equals("h4")) callType = tableSibling.text();
				if (callType.equals("Tiempos compuestos del subjuntivo")) callType = "subjuntivo";
				if (callType.equals("Formas compuestas comunes")) callType = "";
				if (callType.equals("Indicativo")) callType = "";
				if (callType.equalsIgnoreCase("Imperativo")) continue;
				callType = callType.toLowerCase();
				
				Elements ths = table.select("th");
				Elements rows = table.select("tr");
				// assert that ths.size() and rows.size() are the same
				String verbalTense = ""; 
				HashMap<String, String> subjectConjMap = null;
				for (int i = 0; i < ths.size(); i++) {
					// System.out.println(rows.get(i));
					Elements thh = rows.get(i).select("th");
					Elements td = rows.get(i).select("td");
					// Format is one header row with no other cells (td's) inside
					// and then rows with one th and one td
					if (td.size() == 0) {
						verbalTense = thh.get(0).text();
						if (verbalTense.equals("pretérito anterior")) break;
						// if (verbalTense.equals("futuro")) break;
						if (callType.equalsIgnoreCase("Subjuntivo") && verbalTense.startsWith("futuro")) break;
						if (!callType.equals("")) verbalTense = verbalTense + " " + callType;
						vac.allVerbalTenses.add(verbalTense);
						subjectConjMap = new HashMap<String, String>();
						vac.verbChartMap.put(verbalTense, subjectConjMap);
					} else {
						String subjectTense = thh.get(0).text();
						if (subjectTense.equals("vos")) continue;	
						String conjugation = td.get(0).text();
						String[] conjPieces = conjugation.split(" ");
						if (conjPieces.length == 3) {
							conjugation = conjPieces[0];
						} else if (conjPieces.length == 4) {
							conjugation = conjPieces[0] + " " + conjPieces[conjPieces.length -1];
						}
						subjectConjMap.put(subjectTense, conjugation);	
					}
					
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return vac;
	}
	
	public static void main(String[] args) {
		VerbChartAccessor vac = getConjugations("poner");
		HashMap<String, HashMap<String, String>> ponerConjs = vac.verbChartMap;
		for (String tense: vac.allVerbalTenses) {
			HashMap<String, String> conjugationMap = ponerConjs.get(tense);
			for (String subject: conjugationMap.keySet()) {
				String conjugation = conjugationMap.get(subject);
				System.out.println(tense + ": { " + subject + ", " + conjugation + "}");
			}
		}
	}
	
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
	
}
