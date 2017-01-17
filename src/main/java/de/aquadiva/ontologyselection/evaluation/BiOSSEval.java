package de.aquadiva.ontologyselection.evaluation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.aquadiva.ontologyselection.JoyceSymbolConstants;
import de.aquadiva.ontologyselection.base.data.ScoreType;
import de.aquadiva.ontologyselection.base.util.ErrorFromNCBORecommenderException;
import de.aquadiva.ontologyselection.evaluation.data.Setting;
import de.aquadiva.ontologyselection.evaluation.services.IBiOSSComparator;
import de.aquadiva.ontologyselection.evaluation.services.JoyceEvaluationModule;

public class BiOSSEval {

	public static void main(String[] args) throws ErrorFromNCBORecommenderException {
		System.setProperty(JoyceSymbolConstants.GAZETTEER_CONFIG, "bioportal.gazetteer.eval.properties");
		
		Registry r = null;
		try {
			r = RegistryBuilder
					.buildAndStartupRegistry(JoyceEvaluationModule.class);
			IBiOSSComparator biosComparator = r
					.getService(IBiOSSComparator.class);
			
			/*
			 * read settings file settings.json
			 */
			Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssX").create();
			BufferedReader br = null;
			Setting setting = null;
			try {
				br = new BufferedReader( new FileReader("eval/settings.json") );
				setting = gson.fromJson(br, Setting.class);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				if( br!=null ) { try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				} }
			}
			
//			biosComparator
//					.run("eval/", "biosevalsettings.txt", new int[] { 1 }, new ScoreType[] {
//							ScoreType.TERM_COVERAGE, ScoreType.CLASS_OVERHEAD,
//							ScoreType.CLASS_OVERLAP }, "biossevaloutput_size4.txt", true,
//							null);
			String apiKey = System.getProperty(JoyceSymbolConstants.BIOPORTAL_API_KEY);
			biosComparator
			.run(setting.evaluation_settings, new ScoreType[] {
					ScoreType.TERM_COVERAGE, ScoreType.CLASS_OVERHEAD,
					ScoreType.CLASS_OVERLAP }, "biossevaloutput_size4.txt", null, setting.BiOSS_parameters, "test", apiKey);
			
			
		} finally {
			r.shutdown();
		}

	}

}
