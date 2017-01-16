import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import de.aquadiva.ontologyselection.evaluation.bioss.BiOSSConnector;
import de.aquadiva.ontologyselection.evaluation.bioss.data.bioportal.RecommendationResult;
import de.aquadiva.ontologyselection.evaluation.data.BiOSSParameters;


public class BiOSSConnectorTest {
	static final String testInput = "Backpain, White blood cell, Carcinoma, Cavity of stomach, Ductal Carcinoma in Situ, Adjuvant chemotherapy, Axillary lymph node staging, Mastectomy, tamoxifen, serotonin reuptake inhibitors, Invasive Breast Cancer, hormone receptor positive breast cancer, ovarian ablation, premenopausal women, surgical management, biopsy of breast tumor, Fine needle aspiration, entinel lymph node, breast preservation, adjuvant radiation therapy, prechemotherapy, Inflammatory Breast Cancer, ovarian failure, Bone scan, lumpectomy, brain metastases, pericardial effusion, aromatase inhibitor, postmenopausal, Palliative care, Guidelines, Stage IV breast cancer disease, Trastuzumab, Breast MRI examination";
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetBiOSSRecommendations() {
		BiOSSParameters biossParams = new BiOSSParameters();
		biossParams.acceptance_weight = 0.15;
		biossParams.coverage_weight = 0.55;
		biossParams.input_type = 2;
		biossParams.knowledge_detail_weight = 0.15;
		biossParams.maximal_number_of_elements_per_set = 4;
		biossParams.output_type = 2;
		biossParams.specialization_weight = 0.15;
		
		ArrayList<RecommendationResult> recommendations = BiOSSConnector.getBiOSSRecommendations(testInput, biossParams);
		assertTrue( recommendations.size()==25 );
	}

}
