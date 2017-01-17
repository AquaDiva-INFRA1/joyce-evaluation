package de.aquadiva.ontologyselection.evaluation.bioss;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.json.JSONObject;
import org.semanticweb.owlapi.util.EscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.aquadiva.ontologyselection.JoyceSymbolConstants;
import de.aquadiva.ontologyselection.base.services.BioPortalUtil;
import de.aquadiva.ontologyselection.evaluation.bioss.data.bioportal.RecommendationResult;
import de.aquadiva.ontologyselection.evaluation.data.BiOSSParameters;
import de.aquadiva.ontologyselection.util.NoResultFromNCBORecommenderException;

/**
 * A class providing access methods for the BiOSS recommender hosted at
 * BioPortal.
 * 
 * @author friederike
 * 
 */
public class BiOSSConnector {
	public static final String REST_URL_BIOPORTAL = "http://data.bioontology.org";
	// removed by EF prior to GitHub switch
	public static final String API_KEY = "";
	public static final int INPUT_TYPE = 2; // 1 - text, 2 - comma separated
											// keywords
	public static final int OUTPUT_TYPE = 2; // 1 - ranked list of individual
												// ontologies, 2 - ranked list
												// of ontology sets
	public static final int MAXIMUM_NUMBER_OF_ELEMENTS_PER_SET = 4; // maximum
																	// number of
																	// ontologies
																	// per set
																	// (just for
																	// OUTPUT_TYPE
																	// = 2)
	public static final double COVERAGE_WEIGHT = 0.55; // weight assigned to the
														// BiOSS ontology
														// coverage criterion,
														// value in [0,1]
	public static final double SPECIALIZATION_CRITERION = 0.15; // weight
																// assigned to
																// the BiOSS
																// ontology
																// specialization
																// criterion,
																// value in
																// [0,1]
	public static final double ACCEPTANCE_WEIGHT = 0.15; // weight assigned to
															// the BiOSS
															// ontology
															// acceptance
															// criterion, value
															// in [0,1]
	public static final double KNOWLEDGE_DETAIL_WEIGHT = 0.15; // weight
																// assigned to
																// the BiOSS
																// ontology
																// knowledge
																// detail
																// criterion,
																// value in
																// [0,1]
	// ontologies={ontology_id1, ontology_id2, …, ontology_idN} // default =
	// (empty) (all BioPortal ontologies will be evaluated).
	// private static String ontologies =
	// "ontologies=CCON,GRO-CPD,MHC,MIRO,ANCESTRO,VHOG,SDO,CMO,EMAP,PEDTERM,ONL-MSA,MWLA,PSDS,ECO,CANCO,FHHO,VSO,NMOSP,SAO,INO,FB-CV,GFO-BIO,AEO,EFO,LHN,MIXSCV,ONTOMA,TRIAGE,MCCL,BAO-GPCR,ONLIRA,BCTEO,OBI_BCGO,MEDO,TRON,SPD,MCCV,MIRNAO,ELIG,OPB,DERMLEX,STUFF,VARIO,OBI,TGMA,HOM,UBERON,BT,ZEA,VSAO,PTO,EPILONT,PTRANS,MAT,MF,TOP-MENELAS,AERO,VIVO,CO-WHEAT,SCHEMA,RSA,BHO,IDODEN,MPATH,NTDO,VIVO-ISF,ECG,XEO,suicideo,FB-BT,FYPO,EXO,OOEVV,REPO,GALEN,NCRO,CTCAE,CMPO,BAO,XCO,MMO,PMA,DIAGONT,DOID,BNO,NIHSS,SSO,PEAO,EMO,BRIDG,RH-MESH,PPIO,GCO,CHEMINF,SEDI,PDO_CAS,PORO,NBO,BIM,ONSTR,CNO,NMR,ZFA,BSPO,SWO,CSEO,CSSO,CANONT,FB-SP,MEGO,HIVO004,TYPON,NGSONTO,GBM,PROVO,EHDAA,CN,OGMS,BHN,NEOMARK3,NCCO,EDDA,RADLEX,NIFDYS,SITBAC,PECO,GLYCO,IFAR,DERMO,GFVO,HUPSON,pseudo,FAO,PMR,XAO,FIX,CBO,BIOMO,EHDA,ASDPTO,SOY,MFOEM,SYMP,BMT,OBIB,RXNO,OGR,ABA-AMB,GRO-CPGA,BRO,SIO,NEOMARK4,GO-PLUS,GO,TRAK,ODNAE,IXNO,WB-PHENOTYPE,NIGO,ONTOKBCF,ONTODT,APO,SPO,ICD11-BODYSYSTEM,CARELEX,ERO,RNAO,WB-BT,CAO,LPT,ALLERGYDETECTOR,SBO,HPIO,FB-DV,UNITSONT,TM-CONST,SHR,MFO,EDAM,CARO,BP,OMIT,EP,EOL,TM-MER,QUDT,NATPRO,PW,GO-EXT,PDON,DCO-DEBUGIT,SEP,MEO,CDAO,ADO,BDO,PAE,MP,SBOL,OGDI,SPTO,FBbi,TM-SIGNS-AND-SYMPTS,RNPRIO,COGPO,InterNano,FIRE,TADS,ICO,ONTOPNEUMO,HRDO,PATHLEX,NIFSUBCELL,WSIO,PHYLONT,GEXO,CARRE,TAXRANK,PIERO,PR,TOK,MIXS,ATOL,EDAMTO,OBIWS,OPL,TAO,OGMD,ONL-DP,ONTOAD,BSAO,BIRNLEX,VT,RS,GPML,FLOPO,CSO,SO,MO,TMO,ENVO,NPO,OBOREL,HP,CHMO,ADAR,PROPREO,MS,REX,AMINO-ACID,ORTHO,HEIO,AURA,MAMO,TEDDY,ROO,MA,PCO,CU-VO,TM-OTHER-FACTORS,KISAO,PVONTO,IDQA,ICF,PHENX,PSIMOD,LDA,OBCS,PO,GFO,HAO,EHDAA2,BOF,MCBCC,ICECI,BCO,PAV,FO,WB-LS,COGAT,CCONT,DIAB,CCO,NMOBR,NIFSTD,ENM,JERM,ADW,PHARE,NONRCTO,RPO,ATO,GENE-CDS,MERA,ONTOTOXNUC,SMASH,ONTODM-CORE,SWEET,ONTODM-KDD,RETO,ADMIN,GLYCORDF,REXO,STATO,PDO,IMGT-ONTOLOGY,HINO,HUGO,SSE,SD3,WIKIPATHWAYS,VTO,CABRO,CYTO,ORDO,MSV";
	// private static String ontologies =
	// "ontologies=PW,BOF,suicideo,AMINO-ACID,SBOL,BSPO,ODNAE,PDO,SHR,VARIO,PHYLONT,ABA-AMB,WSIO,MCCV,ONTOTOXNUC,CANCO,MP,SPO,LDA,CARRE,NMR,VTO,DERMLEX,SBO,CN,MAT,HUPSON,ICO,BHO,EHDA,HPIO,EHDAA2,HIVO004,NEOMARK3,PORO,TM-MER,TRIAGE,BIRNLEX,GO-PLUS,BCO,EMO,GEXO,CSO,MF,CTCAE,TM-OTHER-FACTORS,CYTO,ZEA,TOK,HP,OBIWS,ICF,RPO,PHARE,STATO,BP,COGAT,PATHLEX,XEO,MEGO,RETO,CDAO,ONTOKBCF,MHC,GO-EXT,XAO,DERMO,PHENX,OGMD,CBO,CSSO,IFAR,TMO,MCCL,DCO-DEBUGIT,FB-SP,GLYCO,PO,OMIT,AURA,CCONT,PIERO,PAE,MFOEM,MEO,SYMP,NPO,PSDS,NIFSUBCELL,RXNO,CANONT,WB-LS,TM-CONST,OBCS,NIGO,LPT,BIM,BIOMO,MFO,PVONTO,BHN,FIRE,CARO,OBIB,MERA,ASDPTO,SSO,MS,VSO,ADMIN,OBI,MWLA,NONRCTO,CAO,SITBAC,REX,TYPON,GLYCORDF,WB-BT,NIFDYS,InterNano,KISAO,MSV,STUFF,PDO_CAS,ADO,SIO,GENE-CDS,OGMS,BMT,ICD11-BODYSYSTEM,RH-MESH,ONTODT,SCHEMA,GFVO,ICECI,SDO,CHMO,VT,NIHSS,TGMA,SEDI,PEAO,DIAB,FAO,TRON,EPILONT,BSAO,IMGT-ONTOLOGY,ERO,OBI_BCGO,CSEO,GO,ONTOAD,ORTHO,CO-WHEAT,EHDAA,FB-DV,MMO,IDQA,MO,CMPO,ANCESTRO,UNITSONT,PCO,GRO-CPGA,AEO,CABRO,PR,MAMO,BNO,WIKIPATHWAYS,CNO,NBO,PSIMOD,EXO,NTDO,EOL,PMR,TOP-MENELAS,EP,NMOSP,NGSONTO,RNAO,ROO,PAV,JERM,OPB,VIVO-ISF,SWO,SPD,GCO,NATPRO,RADLEX,SOY,FBbi,FB-CV,ENVO,APO,MIRO,TM-SIGNS-AND-SYMPTS,PPIO,ORDO,ONLIRA,CCON,FO,ATO,REXO,NCCO,SPTO,QUDT,HAO,COGPO,NEOMARK4,VHOG,ONL-MSA,LHN,ECO,OPL,BT,HUGO,CCO,BAO,FYPO,SEP,MIRNAO,SAO,SO,ATOL,FB-BT,GRO-CPD,ELIG,GBM,HINO,UBERON,HEIO,SSE,BAO-GPCR,MIXSCV,PROPREO,MCBCC,MIXS,EMAP,EFO,ONL-DP,PDON,RNPRIO,MEDO,EDAM,TRAK,BRIDG,pseudo,XCO,REPO,GPML,TADS,PEDTERM,GFO,SD3,PTRANS,GFO-BIO,IDODEN,RS,FHHO,ADAR,EDDA,NCRO,INO,GALEN,FIX,ONSTR,PMA,PROVO,WB-PHENOTYPE,CMO,HOM,OOEVV,VSAO,ECG,ONTODM-CORE,DIAGONT,IXNO,DOID,ONTOMA,ONTODM-KDD,OGDI,HRDO,BRO,AERO,TAXRANK,TEDDY,ADW,VIVO,RSA,BCTEO,CARELEX,MPATH,BDO,PECO,OGR,MA,PTO,ALLERGYDETECTOR,TAO,ONTOPNEUMO,ZFA,EDAMTO,FLOPO,CHEMINF,CU-VO";
	private static String ontologies = "ontologies=COGAT,RXNO,REPO,VSAO,PROVO,GFO-BIO,GPML,MEDO,LBO,AERO,TOK,GRO-CPD,TGMA,TYPON,HRDO,MEGO,HFO,XAO,SPO,PTO,DERMO,BT,SP,OPL,OAE,TMO,NIGO,GBM,RNRMU,AEO,BAO,ISO-ANNOTATIONS,FB-SP,MINERAL,TM-OTHER-FACTORS,NIFCELL,CSEO,BHN,OOEVV,DDANAT,BIM,MIRO,CSSO,SEDI,TRAK,MNR,UNITSONT,PAE,VIVO-ISF,TAO,ONTOPNEUMO,GRO,ORTH,CARELEX,ICECI,RNAO,MMO,ABA-AMB,SITBAC,BTO,ONL-MR-DA,PW,DCO-DEBUGIT,OBCS,IDODEN,GFVO,TOP-MENELAS,ONL-MSA,CAO,PHARE,ADO,SBOL,FB-DV,ICD11-BODYSYSTEM,LDA,ASDPTO,DIAGONT,CADSRVS,DOID,BCO,EDAMTO,NIFSUBCELL,BP,CO-WHEAT,MWLA,KISAO,RNPRIO,ECO,PVONTO,CPTAC,OBIWS,SYMP,FIX,PAV,BOF,CU-VO,LHN,CBO,CANCO,EHDAA,HAO,IFAR,IDQA,SOY,MPATH,NIFDYS,DERMLEX,CHEMBIO,INSECTH,OGMS,PEDTERM,ELIG,ENVO,MFOEM,ROO,MCBCC,MIXS,ZEA,TM-CONST,GMO,FAO,ONSTR,PORO,ONTOAD,OBIB,PATHLEX,NATPRO,FBbi,MIRNAO,MO,pseudo,TEO,CANONT,ADAR,SBO,SO,PPIO,InterNano,CHMO,OMRSE,ATO,ANCESTRO,AMINO-ACID,VHOG,FB-CV,IDOMAL,NPO,SNPO,ENTITYCANDIDATES,SPD,OGDI,BMT,TADS,OCHV,FO,WB-PHENOTYPE,PHENX,VO,ONLIRA,CABRO,PROPREO,PMR,DLORO,EHDA,NGSONTO,TM-MER,RS,RAO,HP,CCONT,PECO,MA,CL,OMIT,ERO,WB-LS,BRO,EDAM,SYN,FIRE,MHC,CMO,WB-BT,XCO,BCTT,APO,MIXSCV,PCO,IXNO,DDI,EPILONT,FYPO,MP,ICO,NTDO,FB-BT,OPB,CTCAE,SPTO,BNO,NMOBR,HOM,SWO,EXO,PSIMOD,VT,OGMD,OF,GFO,MCCV,CN,OBI,ECG,CMPO,OBI_BCGO,EHDAA2,CARO,TM-SIGNS-AND-SYMPTS,MFO,SAO,PTRANS,BSPO,EP,VSO,CNO,CPRO,QUDT,ONTOKBCF,OGR,suicideo,TRON,ACGT-MO,BCTEO,ONTODT,SHR,MAT,ALLERGYDETECTOR,NBO,PMA,WSIO,BIRNLEX,FHHO,INO,HUPSON,PHYLONT,IMGT-ONTOLOGY,SIO,JERM,PDON,AO,TRANS,GENE-CDS,RH-MESH,EOL,SSO,CTO,ONTOTOXNUC,PR,VICO,TAXRANK,CHEMINF,TTO,GO-EXT,CHEBI,HEIO,SD3,EO,SSE,SURGICAL,DIAB,PDO_CAS,PP,HIVO004,ONTODM-CORE,HPIO,SCHEMA,ONTODM-KDD,DDO,HUGO,ORDO,RSA,INM-NPI,MERA,PEAO,CYTO,ADMIN,EDDA,NCCO,AURA,MCCL,ECP,WIKIPATHWAYS,GO-PLUS,GCO,PDO,STATO,MSV,MAMO,RETO,CARRE,APAONTO,GALEN,RADLEX,TRIAGE,GLYCORDF,FTC,DOCCC,PXO,ZFA,CHD,MEDEON,FMA,ONTOMA,PLIO,NEOMARK4,EMO,OFSMR,PIERO,UBERON,BDO,FLOPO,NCIT,BIOMO";
	private static final Logger log = LoggerFactory.getLogger(BiOSSConnector.class);

	/**
	 * Retrieves BiOSS recommendations from BioPortal based on the given
	 * keywords. Note: Currently, all parameters are fixed. However, this can be
	 * changed, if needed. Current parameter setting: input type: comma
	 * separated keywords output type: ranked list of ontology sets max number
	 * of elements per set: 3 coverage weight: 0.55 specialization weight: 0.15
	 * acceptance weight: 0.15 knowledge detail weight: 0.15
	 * 
	 * @param input
	 *            comma separated keywords
	 * @param key 
	 * @return a list of recommendation results
	 * @throws NoResultFromNCBORecommenderException
	 */
	public static ArrayList<RecommendationResult> getBiOSSRecommendations(String input, BiOSSParameters biossParams, String key)
			throws NoResultFromNCBORecommenderException {

		// prepare input string
		String keywords = input.replaceAll(" ", "+");
		keywords = EscapeUtils.escapeString(keywords);
		// get BiOSS recommendations from BioPortal
		// String request = REST_URL_BIOPORTAL + "/recommender?input=" +
		// keywords
		// + "&input_type=" + biossParams.input_type + "&output_type="
		// + biossParams.output_type + "&max_elements_set="
		// + biossParams.maximal_number_of_elements_per_set + "&wc="
		// + biossParams.coverage_weight + "&ws="
		// + biossParams.specialization_weight + "&wa="
		// + biossParams.acceptance_weight + "&wd="
		// + biossParams.knowledge_detail_weight + "&" + ontologies;
		// log.info("Sending post request to BiOSS: {}", request);
		// String jsonRecommendation = BioPortalUtil.getFromUrl(new URL(
		// request),
		// API_KEY);
		assert !StringUtils.isBlank(key) : "A valid BioPortal API key has to be delivered via the Java system property "
				+ JoyceSymbolConstants.BIOPORTAL_API_KEY + " or via this symbol in the configuration file.";
		String request = REST_URL_BIOPORTAL + "/recommender?apikey=" + key + "&input=" + keywords + "&input_type="
				+ biossParams.input_type + "&output_type=" + biossParams.output_type + "&max_elements_set="
				+ biossParams.maximal_number_of_elements_per_set + "&wc=" + biossParams.coverage_weight + "&ws="
				+ biossParams.specialization_weight + "&wa=" + biossParams.acceptance_weight + "&wd="
				+ biossParams.knowledge_detail_weight + "&" + ontologies;
		log.info("Sending post request to NCBO Recommender: {}", request);
		String jsonRecommendation = null;
		try {
			jsonRecommendation = BioPortalUtil.getFromUrl(new URL(request));
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		int numRetry = 1;
		while (jsonRecommendation == null) {
			if (numRetry == 4)
				throw new NoResultFromNCBORecommenderException(
						"Tried 3 times to get a result from the NCBO Recommender but failed to get one. Giving up.");
			log.info(
					"Could not get a response from the NCBO Recommender in the {}. try. Waiting for 30 seconds and then trying again.",
					numRetry++);
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				jsonRecommendation = BioPortalUtil.getFromUrl(new URL(request));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		log.trace("Receiving response from NCBO Recommender: {}", jsonRecommendation);

		// parse recommendations from json
		ArrayList<RecommendationResult> recommendations = new ArrayList<RecommendationResult>();
		JSONArray jsonArray = new JSONArray(jsonRecommendation);
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssX").create();

		for (Object jsonRec : jsonArray) {
			RecommendationResult rec = gson.fromJson(((JSONObject) jsonRec).toCompactString(),
					RecommendationResult.class);
			recommendations.add(rec);
		}

		return recommendations;

	}

	// // Create a new instance of the html unit driver
	// // Notice that the remainder of the code relies on the interface,
	// // not the implementation.
	// WebDriver driver = new HtmlUnitDriver();
	//
	// // And now use this to visit Google
	// driver.get("http://bioss.ontologyselection.com/Default.aspx");
	//
	// // find the input term box by its id
	// WebElement inputbox = driver.findElement(By.id("MainContent_TextBox1"));
	// // TODO: set keywords
	//
	// // find the checkbox for combined output and check it
	// WebElement combinedCheckbox =
	// driver.findElement(By.id("MainContent_RadioButtonList1_1"));
	// combinedCheckbox.click();
	//
	// // click Go! button
	// WebElement goButton = driver.findElement(By.id("MainContent_Button1"));
	// goButton.click();
	//
	// // sleep until the results are visible (maximal 30 seconds)
	// long end = System.currentTimeMillis() + 30000;
	// while (System.currentTimeMillis() < end) {
	//
	// WebElement resultsDiv =
	// driver.findElement(By.id("MainContent_UpdatePanel2"));
	//
	// System.out.println(resultsDiv.isDisplayed());
	//
	// // if the results are displayed go on
	// if (resultsDiv.isDisplayed()) {
	// break;
	// }
	// }
	//
	// System.out.println(driver.getPageSource());
	//
	// // JavascriptExecutor js = null;
	// // if (driver instanceof JavascriptExecutor) {
	// // js = (JavascriptExecutor)driver;
	// // }
	// //
	// js.executeScript("return
	// document.getElementById('MainContent_Button1');");
	//
	//
	// // // get the result rows
	// // WebElement resultsDiv =
	// driver.findElement(By.id("MainContent_UpdatePanel2"));
	// // System.out.println(resultsDiv.toString());
	// // List<WebElement> resultRows =
	// resultsDiv.findElements(By.tagName("tr"));
	// //
	// // for (WebElement row : resultRows) {
	// // System.out.println("row");
	// // WebElement colResultId = row.findElement(By.tagName("td"));
	// // System.out.println(colResultId.getText());
	// // }
	//
	//
	// // List<WebElement> allSuggestions =
	// driver.findElements(By.xpath("//td[@class='gssb_a gbqfsf']"));
	//
	// // // Enter something to search for
	// // System.out.println(element.getText());
	//
	// driver.quit();

}