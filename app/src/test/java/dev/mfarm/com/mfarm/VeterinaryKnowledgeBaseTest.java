package dev.mfarm.com.mfarm;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VeterinaryKnowledgeBaseTest {

    @Test
    public void testKnowledgeBaseIsAvailableOffline() {
        List<VeterinaryKnowledgeBase.DiseaseGuide> diseases = VeterinaryKnowledgeBase.getAllDiseases();
        assertNotNull("Diseases list should not be null", diseases);
        assertTrue("Diseases list should be pre-populated for offline use", diseases.size() >= 26);
    }

    @Test
    public void testStableSpeciesTargeting() {
        VeterinaryKnowledgeBase.DiseaseGuide ecf = VeterinaryKnowledgeBase.getById("ecf"); // Cattle
        assertNotNull(ecf);
        assertTrue(ecf.targets("CATTLE"));
        assertTrue(ecf.targets("ALL"));
        assertFalse(ecf.targets("POULTRY"));
        assertFalse(ecf.targets("CROP"));

        VeterinaryKnowledgeBase.DiseaseGuide faw = VeterinaryKnowledgeBase.getById("faw"); // Crop
        assertNotNull(faw);
        assertTrue(faw.targets("CROP"));
        assertTrue(faw.targets("ALL"));
        assertFalse(faw.targets("CATTLE"));

        VeterinaryKnowledgeBase.DiseaseGuide nd = VeterinaryKnowledgeBase.getById("newcastle"); // Poultry
        assertNotNull(nd);
        assertTrue(nd.targets("POULTRY"));
        assertTrue(nd.targets("ALL"));
        assertFalse(nd.targets("RABBIT"));
    }

    @Test
    public void testSymptomAssessmentLogic() {
        Set<String> symptoms = new HashSet<>();
        symptoms.add("Fever");
        symptoms.add("Lameness");
        symptoms.add("Mouth/foot blisters");
        
        List<VeterinaryKnowledgeBase.SymptomMatch> matches = VeterinaryKnowledgeBase.evaluateSymptoms(symptoms);
        assertFalse("Should return matches for FMD", matches.isEmpty());
        
        boolean foundFmd = false;
        for (VeterinaryKnowledgeBase.SymptomMatch match : matches) {
            if ("fmd".equals(match.disease.id)) {
                foundFmd = true;
                assertEquals(3, match.matchedCount);
            }
        }
        assertTrue("FMD should be a top match for these symptoms", foundFmd);
    }
    
    @Test
    public void testEmptyAndInvalidSearchResults() {
        Set<String> emptySymptoms = new HashSet<>();
        List<VeterinaryKnowledgeBase.SymptomMatch> matches = VeterinaryKnowledgeBase.evaluateSymptoms(emptySymptoms);
        assertTrue("Empty symptoms should return empty results", matches.isEmpty());
        
        Set<String> invalidSymptoms = new HashSet<>();
        invalidSymptoms.add("NonExistentSymptom123");
        List<VeterinaryKnowledgeBase.SymptomMatch> invalidMatches = VeterinaryKnowledgeBase.evaluateSymptoms(invalidSymptoms);
        assertTrue("Invalid symptoms should yield zero matches", invalidMatches.isEmpty());
    }

    @Test
    public void testCropAndAnimalSeparation() {
        VeterinaryKnowledgeBase.DiseaseGuide cwd = VeterinaryKnowledgeBase.getById("cwd");
        assertNotNull(cwd);
        assertTrue(cwd.isCrop());
        
        VeterinaryKnowledgeBase.DiseaseGuide faw = VeterinaryKnowledgeBase.getById("faw");
        assertNotNull(faw);
        assertTrue(faw.isCrop());

        VeterinaryKnowledgeBase.DiseaseGuide mastitis = VeterinaryKnowledgeBase.getById("mastitis");
        assertNotNull(mastitis);
        assertFalse(mastitis.isCrop());
    }
}
