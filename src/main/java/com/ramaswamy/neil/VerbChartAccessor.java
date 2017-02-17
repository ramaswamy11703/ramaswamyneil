package com.ramaswamy.neil;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class VerbChartAccessor {
	
	
	
	public static String[][] getConjugations(String verb) {
		try {                               
			Document doc = Jsoup.connect("http://www.wordreference.com/conj/ESverbs.aspx?v=" + verb).get();
			// https://jsoup.org/apidocs/org/jsoup/select/Selector.html is cool! table[class^=\"neoConj\"]
			
			Elements tables = doc.select("table[class^=\"neoConj\"");
			for (Element table: tables) {
				Elements ths = table.select("th");
				Elements rows = table.select("tr");
				// assert that ths.size() and rows.size() are the same
				for (int i = 0; i < ths.size(); i++) {
					System.out.print("Row " + i + ": "); //  + ":" + rows.get(i).text());
					// System.out.println(rows.get(i));
					Elements the = rows.get(i).select("th");
					for (Element ee : the) {
						System.out.print(ee.text() + ":");
					}
					Elements thr = rows.get(i).select("td");
					for (Element ee: thr) {
						System.out.println(ee.text());
					}
					if (thr.size() == 0) System.out.println("");
					
				}
				
				/*
				for (Element th : table.select("th")) {
					System.out.println(th.text());
				}
				for (Element row : table.select("tr")) {
					for (Element cell : row.select("td")) {
						System.out.println("Row " + i + ": " + cell.text());
					}
					i++;
				}
				*/
				
				
			}
			
		
		
		
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) {
		getConjugations("poner");
	}
}
