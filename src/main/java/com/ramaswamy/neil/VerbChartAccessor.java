package com.ramaswamy.neil;
import java.util.ArrayList;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class VerbChartAccessor {
	
	
	private static ArrayList<String> allVerbalTenses = new ArrayList<String>(16);
	public static HashMap<String, HashMap<String, String>> getConjugations(String verb) {
		//This HashMap goes from the verbal tense to a HashMap of subject tense to appropriate conjugation.
		HashMap<String, HashMap<String, String>> verbChart = new HashMap<String, HashMap<String, String>>();
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
				
				Elements ths = table.select("th");
				Elements rows = table.select("tr");
				// assert that ths.size() and rows.size() are the same
				String verbalTense = ""; 
				HashMap<String, String> subjectConjMap = null;
				for (int i = 0; i < ths.size(); i++) {
					System.out.print("Row " + i + ": "); //  + ":" + rows.get(i).text());
					// System.out.println(rows.get(i));
					Elements thh = rows.get(i).select("th");
					Elements td = rows.get(i).select("td");
					// Format is one header row with no other cells (td's) inside
					// and then rows with one th and one td
					if (td.size() == 0) {
						verbalTense = thh.get(0).text();
						if (verbalTense.equals("pretérito anterior")) break;
						if (verbalTense.equals("futuro perfecto")) break;
						if (callType.equals("Subjuntivo") && verbalTense.equals("futuro")) break;
						if (!callType.equals("")) verbalTense = verbalTense + " " + callType;
						allVerbalTenses.add(verbalTense);
						subjectConjMap = new HashMap<String, String>();
						verbChart.put(verbalTense, subjectConjMap);
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
					for (Element ee : thh) {
						System.out.print("(th): " + ee.text() + ":");
					}
					for (Element ee: td) {
						System.out.println("(td): " + ee.text() + "|");
					}
					if (td.size() == 0) System.out.println("");
					
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return verbChart;
	}
	
	public static void main(String[] args) {
		HashMap<String, HashMap<String, String>> ponerConjs = getConjugations("poner");
		for (String tense: allVerbalTenses)  {
			HashMap<String, String> conjugationMap = ponerConjs.get(tense);
			for (String subject: conjugationMap.keySet()) {
				String conjugation = conjugationMap.get(subject);
				System.out.println(tense + ": { " + subject + ", " + conjugation + "}");
			}
		}
	}
}
