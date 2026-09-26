package dev.mfarm.com.mfarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VeterinaryKnowledgeBase {

    public static class DiseaseGuide {
        public final String id;
        public final String name;
        public final String aliases;
        public final String species;
        public final String category;
        public final String urgency; // CRITICAL, HIGH, MEDIUM
        public final boolean notifiable;
        public final boolean zoonotic;
        public final String cause;
        public final String transmission;
        public final String riskFactors;
        public final String incubation;
        public final List<String> earlySigns;
        public final List<String> clinicalSigns;
        public final List<String> emergencySigns;
        public final List<String> farmerChecks;
        public final List<String> immediateActions;
        public final String veterinaryManagement;
        public final String diagnostics;
        public final String prevention;
        public final String vaccination;
        public final String biosecurity;
        public final String treatmentWarnings;
        public final String withdrawalWarning;
        public final String sources;
        public final String lastReviewed;
        public final Set<String> matchedSymptoms;

        public DiseaseGuide(String id, String name, String aliases, String species, String category,
                            String urgency, boolean notifiable, boolean zoonotic, String cause,
                            String transmission, String riskFactors, String incubation,
                            List<String> earlySigns, List<String> clinicalSigns, List<String> emergencySigns,
                            List<String> farmerChecks, List<String> immediateActions,
                            String veterinaryManagement, String diagnostics, String prevention,
                            String vaccination, String biosecurity, String treatmentWarnings,
                            String withdrawalWarning, String sources, String lastReviewed,
                            Set<String> matchedSymptoms) {
            this.id = id;
            this.name = name;
            this.aliases = aliases;
            this.species = species;
            this.category = category;
            this.urgency = urgency;
            this.notifiable = notifiable;
            this.zoonotic = zoonotic;
            this.cause = cause;
            this.transmission = transmission;
            this.riskFactors = riskFactors;
            this.incubation = incubation;
            this.earlySigns = earlySigns;
            this.clinicalSigns = clinicalSigns;
            this.emergencySigns = emergencySigns;
            this.farmerChecks = farmerChecks;
            this.immediateActions = immediateActions;
            this.veterinaryManagement = veterinaryManagement;
            this.diagnostics = diagnostics;
            this.prevention = prevention;
            this.vaccination = vaccination;
            this.biosecurity = biosecurity;
            this.treatmentWarnings = treatmentWarnings;
            this.withdrawalWarning = withdrawalWarning;
            this.sources = sources;
            this.lastReviewed = lastReviewed;
            this.matchedSymptoms = matchedSymptoms;
        }

        public String symptomsString() {
            StringBuilder sb = new StringBuilder();
            if (earlySigns != null) for (String s : earlySigns) sb.append(s).append(" ");
            if (clinicalSigns != null) for (String s : clinicalSigns) sb.append(s).append(" ");
            if (emergencySigns != null) for (String s : emergencySigns) sb.append(s).append(" ");
            return sb.toString();
        }
    }

    public static class SymptomMatch {
        public final DiseaseGuide disease;
        public final int matchedCount;
        public final List<String> matchingSigns;

        public SymptomMatch(DiseaseGuide disease, int matchedCount, List<String> matchingSigns) {
            this.disease = disease;
            this.matchedCount = matchedCount;
            this.matchingSigns = matchingSigns;
        }
    }

    private static final List<DiseaseGuide> DISEASES = new ArrayList<>();

    static {
        // 1. East Coast Fever (ECF)
        DISEASES.add(new DiseaseGuide(
                "ecf", "East Coast Fever (ECF)", "Theileriosis, Amakebe", "Cattle", "Tick-Borne Disease",
                "HIGH", false, false, "Theileria parva (protozoan parasite)",
                "Transmitted by Rhipicephalus appendiculatus (brown ear tick).",
                "Presence of brown ear ticks, un-dipped cattle, introduction of non-immune cattle.",
                "8 to 18 days.",
                Arrays.asList("Slight fever", "Reduced feed intake", "Dullness"),
                Arrays.asList("High fever (40–41.5°C)", "Swollen lymph nodes (especially below ear & shoulder)", "Frothy nasal discharge", "Watery eyes", "Difficulty breathing", "Anemia & muscle wasting"),
                Arrays.asList("Severe gasping for air", "Inability to stand", "Froth pouring from nostrils"),
                Arrays.asList("Check temperature with thermometer", "Palpate lymph nodes under ear and shoulder for swelling", "Check ears for ticks"),
                Arrays.asList("Isolate sick animal in a cool shaded area", "Provide fresh clean water and soft green feed", "Call veterinarian immediately"),
                "Veterinary treatment may include antiprotozoal agents (such as Buparvaquone) administered early in the disease course, along with anti-inflammatory support.",
                "Blood smear / lymph node aspirate microscopy, PCR.",
                "Strict tick control via regular dipping or spraying; strategic pasture management.",
                "ECF Infection and Treatment Method (ITM) immunization where available.",
                "Isolate newly purchased stock for 30 days and dip before joining herd.",
                "Antimicrobials & antiprotozoals must be prescribed by a qualified vet. Administer early for best survival.",
                "Observe mandatory milk (typically 48–72h) and meat withdrawal periods as specified on product label.",
                "Uganda MAAIF Veterinary Guidelines; WOAH Terrestrial Manual; MSD Veterinary Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Loss of appetite", "Swollen lymph nodes", "Cough", "Nasal discharge", "Difficult breathing", "Severe weakness"))
        ));

        // 2. Anaplasmosis
        DISEASES.add(new DiseaseGuide(
                "anaplasmosis", "Anaplasmosis", "Gall Sickness", "Cattle, Sheep, Goats", "Tick-Borne Disease",
                "HIGH", false, false, "Anaplasma marginale (rickettsial organism)",
                "Transmitted by ticks, biting flies (tabanids), and unsterilized needles/needles.",
                "High tick population, biting fly season, reusing needles between animals.",
                "3 to 6 weeks.",
                Arrays.asList("Mild fever", "Slight drop in milk yield", "Sluggish movement"),
                Arrays.asList("High fever", "Pale mucous membranes (pale gums and eyes)", "Yellowish eyes/skin (jaundice)", "Constipation followed by hard dry dung with mucus", "Rapid breathing when driven"),
                Arrays.asList("Extreme weakness", "Collapse when walked", "Severe respiratory distress"),
                Arrays.asList("Examine gums and inner eyelids for paleness or yellowing", "Observe dung consistency", "Check rectal temperature"),
                Arrays.asList("Rest animal in shade; DO NOT drive or stress the animal", "Provide fresh water and palatable feed"),
                "Veterinary treatment may include long-acting Tetracyclines or Imidocarb dipropionate, supported by blood transfusion in critical anemia cases.",
                "Stained blood smear showing Anaplasma marginale inclusion bodies.",
                "Regular tick control; control biting flies; use sterile disposable needles for each animal.",
                "Vaccination available in some regions.",
                "Disinfect surgical instruments and change needles between animals.",
                "Tetracycline antibiotics must be administered under veterinary guidance.",
                "Follow product label milk and meat withdrawal times.",
                "Uganda NDA Antimicrobial Guidelines; FAO Animal Health Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Loss of appetite", "Pale mucous membranes", "Yellow mucous membranes", "Reduced milk", "Severe weakness"))
        ));

        // 3. Babesiosis
        DISEASES.add(new DiseaseGuide(
                "babesiosis", "Babesiosis", "Redwater, Cattle Tick Fever", "Cattle", "Tick-Borne Disease",
                "HIGH", false, false, "Babesia bigemina / Babesia bovis",
                "Transmitted by blue ticks (Rhipicephalus decoloratus / microplus).",
                "Un-dipped exotic or crossbred cattle, moving cattle into tick-infested areas.",
                "1 to 3 weeks.",
                Arrays.asList("Fever", "Inappetence", "Isolation from herd"),
                Arrays.asList("High fever (40–42°C)", "Dark red to coffee-colored urine (hemoglobinuria)", "Pale or yellowish mucous membranes", "Rapid heart rate", "Muscle tremors"),
                Arrays.asList("Deep red urine", "Staggering gait", "Nervous signs (circling/aggression with B. bovis)", "Collapse"),
                Arrays.asList("Observe urine color during urination", "Check eye membranes for jaundice/paleness", "Take temperature"),
                Arrays.asList("Keep animal calm in shade", "Provide abundant water", "Contact veterinarian urgently"),
                "Veterinary treatment may include Imidocarb dipropionate or Diminazene aceturate, along with supportive fluid therapy.",
                "Blood smear examination stained with Giemsa.",
                "Strategic tick control; maintain tick immunity in indigenous breeds.",
                "Live attenuated vaccines available in selected countries.",
                "Dip or spray new animals before introduction.",
                "Diminazene and Imidocarb are potent drugs; precise veterinary dosing is critical to prevent toxicity.",
                "Strict adherence to meat and milk withdrawal periods is required.",
                "Uganda MAAIF Animal Health Division; WOAH Reference Guide",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Loss of appetite", "Pale mucous membranes", "Yellow mucous membranes", "Dark/red urine", "Severe weakness"))
        ));

        // 4. Trypanosomiasis
        DISEASES.add(new DiseaseGuide(
                "trypanosomiasis", "Trypanosomiasis", "Nagana, Fly Sickness", "Cattle, Goats, Sheep", "Vector-Borne Disease",
                "HIGH", false, true, "Trypanosoma congolense / vivax / brucei",
                "Transmitted by tsetse flies (Glossina spp.) and mechanically by biting flies.",
                "Grazing near tsetse fly belts, game reserves, or forested river banks.",
                "1 to 3 weeks.",
                Arrays.asList("Intermittent fever", "Gradual dullness", "Watery eyes"),
                Arrays.asList("Progressive severe weight loss (wasting)", "Swollen lymph nodes", "Anemia (pale gums/eyes)", "Rough coat & hair loss", "Decreased milk production & infertility"),
                Arrays.asList("Extreme emaciation", "Inability to rise", "Severe anemia"),
                Arrays.asList("Examine body condition score over time", "Check eye membranes for anemia", "Palpate prescapular lymph nodes"),
                Arrays.asList("Provide high-energy supplementary feed", "Avoid driving animal long distances"),
                "Veterinary management may involve trypanocidal drugs such as Diminazene aceturate or Isometamidium chloride, chosen based on local resistance patterns.",
                "Wet blood mount / buffy coat examination / blood smear.",
                "Deploy tsetse fly traps (e.g. Tiny Targets), apply pour-on insecticides, brush clearing around pastures.",
                "No commercial vaccine available.",
                "Apply prophylactic trypanocides (e.g. Isometamidium) when moving herds through tsetse areas under vet guidance.",
                "Overuse of trypanocides leads to drug resistance. Always consult a veterinarian for diagnostic confirmation.",
                "Observe withdrawal periods for meat and milk following trypanocide administration.",
                "Uganda COCTU (Cabinet Secretariat for Tsetse & Trypanosomiasis Control); FAO",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Loss of appetite", "Swollen lymph nodes", "Pale mucous membranes", "Reduced milk", "Severe weakness"))
        ));

        // 5. Mastitis
        DISEASES.add(new DiseaseGuide(
                "mastitis", "Mastitis", "Garget, Udder Infection", "Cattle, Goats, Sheep", "Bacterial Infection",
                "MEDIUM", false, false, "Staphylococcus aureus, Streptococcus agalactiae, E. coli, etc.",
                "Environmental contamination or cow-to-cow transmission during milking via hands or cloths.",
                "Poor milking hygiene, wet muddy bedding, teat injuries, incomplete milking.",
                "Variable (hours to days).",
                Arrays.asList("Slight flakes or clots in first streams of milk", "Mild teat sensitivity"),
                Arrays.asList("Swollen, hot, hard, or tender udder quarter", "Clotty, stringy, flaky, or watery milk", "Drop in milk yield", "Fever and dullness in acute toxic cases"),
                Arrays.asList("Cold blueish udder quarter (gangrenous mastitis)", "Recumbency and severe systemic shock"),
                Arrays.asList("Perform California Mastitis Test (CMT) or strip cup check on every teat", "Palpate udder quarters after milking for hardness"),
                Arrays.asList("Strip out infected quarter frequently into a container (disinfect afterwards)", "Apply warm compresses to udder", "Keep cow standing on clean dry straw"),
                "Veterinary management may include intramammary antibiotic tubes following sensitivity testing, systemic antibiotics in febrile cases, and anti-inflammatory therapy.",
                "California Mastitis Test (CMT), milk bacterial culture & sensitivity.",
                "Maintain strict milking hygiene: clean dry teats, post-milking teat dip, dry cow therapy at drying off.",
                "No universal vaccine; good management is primary prevention.",
                "Milk affected cows LAST. Sanitize milking equipment and wash hands between cows.",
                "Never sell or consume milk from teats treated with intramammary antibiotics until withdrawal period expires.",
                "Discard milk during treatment and observe mandatory milk withdrawal period (typically 3–7 days post-treatment).",
                "Uganda NDA Guidelines on Veterinary Antimicrobials; IDF Mastitis Control Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Reduced milk", "Abnormal milk", "Hot/swollen udder"))
        ));

        // 6. Calf Diarrhoea
        DISEASES.add(new DiseaseGuide(
                "scours", "Calf Diarrhoea", "Calf Scours", "Calves", "Gastrointestinal Disease",
                "HIGH", false, true, "Rotavirus, Coronavirus, Cryptosporidium, E. coli, Salmonella",
                "Ingestion of pathogens from contaminated environment, dirty calf pens, or infected dams.",
                "Failure of colostrum intake within first 6 hours, dirty calf housing, cold wet pens.",
                "12 hours to 5 days.",
                Arrays.asList("Slight listlessness", "Pasty tail", "Reluctance to suckle"),
                Arrays.asList("Profuse watery yellow, white, or greenish diarrhea", "Sunken eyes (dehydration)", "Cold ears and legs", "Severe weakness & depression", "Raw skin around tail"),
                Arrays.asList("Inability to rise", "Gasping or cool mouth", "Coma due to severe dehydration/acidosis"),
                Arrays.asList("Pinch skin on neck to test dehydration (skin tenting)", "Check mouth temperature and suckle reflex"),
                Arrays.asList("ISOLATE calf immediately", "Administer Oral Rehydration Electrolyte Solution urgently", "Keep calf warm, dry and clean", "DO NOT withhold milk completely for >24h"),
                "Veterinary management includes parenteral fluids for collapsed calves, targeted antimicrobial or antiprotozoal therapy, and gut mucosal protectants.",
                "Fecal pathogen testing / antigen rapid test kits.",
                "Ensure calf receives 2–4 Litres of quality colostrum within first 6 hours of birth; maintain spotless dry maternity & calf pens.",
                "Vaccinate pregnant dams against Rotavirus, Coronavirus, and E. coli prior to calving.",
                "Disinfect calf housing between batches. Use separate feeding buckets per calf.",
                "Dehydration kills calves rapidly. Rehydration therapy is more critical than immediate antibiotic use.",
                "Observe withdrawal times if systemic antibiotics are prescribed by vet.",
                "Uganda MAAIF Livestock Health Series; MSD Vet Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Loss of appetite", "Diarrhoea", "Dehydration", "Severe weakness"))
        ));

        // 7. Pneumonia / BRD
        DISEASES.add(new DiseaseGuide(
                "pneumonia", "Pneumonia / Bovine Respiratory Disease", "BRD, Respiratory Infection", "Cattle, Calves, Goats", "Respiratory Infection",
                "MEDIUM", false, false, "Pasteurella multocida, Mannheimia haemolytica, Mycoplasma, BRSV, IBR",
                "Inhalation of airborne droplets; exacerbated by stress and poor ventilation.",
                "Overcrowding, poor housing ventilation, damp floor, stress from transport or weaning.",
                "2 to 14 days.",
                Arrays.asList("Mild nasal discharge", "Slight dry cough", "Elevated respiratory rate"),
                Arrays.asList("High fever (40–41°C)", "Moist deep cough", "Pus-like or cloudy nasal discharge", "Labored breathing with flared nostrils", "Decreased appetite and drooping ears"),
                Arrays.asList("Open-mouth breathing with tongue protruded", "Severe grunting on expiration", "Cyanotic (blueish) tongue"),
                Arrays.asList("Listen to chest for raspy/crackling lung sounds", "Check breathing rate per minute", "Measure body temperature"),
                Arrays.asList("Move animal to dry, warm, well-ventilated draft-free housing", "Provide clean fresh water and palatable feed"),
                "Veterinary treatment typically involves prescription antimicrobials (such as Oxytetracycline, Florfenicol, or Macrolides) and non-steroidal anti-inflammatory drugs (NSAIDs).",
                "Thoracic auscultation, nasal swab culture, lung ultrasonography.",
                "Proper housing ventilation without cold drafts; avoid overcrowding; reduce weaning/transport stress.",
                "Vaccines available for specific respiratory viral/bacterial pathogens.",
                "Isolate sick animals to prevent airborne spread to pen mates.",
                "Early treatment is essential before permanent lung tissue scarring occurs.",
                "Observe meat and milk withdrawal periods specified for prescription antimicrobials.",
                "Uganda MAAIF Extension Manual; AABP Respiratory Guidelines",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Loss of appetite", "Cough", "Nasal discharge", "Difficult breathing"))
        ));

        // 8. Foot Rot
        DISEASES.add(new DiseaseGuide(
                "footrot", "Foot Rot", "Foul in the Foot, Interdigital Phlegmon", "Cattle, Sheep, Goats", "Bacterial Foot Disease",
                "MEDIUM", false, false, "Fusobacterium necrophorum, Porphyromonas levii",
                "Bacterial entry through injured skin between claws in wet, muddy conditions.",
                "Muddy corrals, sharp stones/stubble, coarse concrete, wet rainy season.",
                "3 to 5 days.",
                Arrays.asList("Slight stiffness in leg", "Slight redness between claw toes"),
                Arrays.asList("Sudden onset of severe lameness", "Swelling around the coronet band and interdigital space", "Foul-smelling pus or discharge between claws", "Spreading of claw toes due to swelling", "Fever and weight loss"),
                Arrays.asList("Inability to walk to pasture", "Deep tissue necrosis or joint involvement"),
                Arrays.asList("Lift leg and clean foot thoroughly with water to inspect between claws", "Check for foreign objects (wire, thorns, stones)"),
                Arrays.asList("Clean foot with clean water and mild antiseptic", "Keep animal on dry clean ground", "Apply topical copper sulfate or oxytetracycline spray"),
                "Veterinary management may include systemic antimicrobial therapy (such as Penicillin or Oxytetracycline) and local wound debridement.",
                "Visual claw inspection and clinical signs.",
                "Keep gateways and barn floors dry; walk stock through 5–10% Copper Sulfate or Zinc Sulfate footbaths regularly.",
                "Vaccines against F. necrophorum available in some countries.",
                "Isolate affected stock on dry ground to prevent pasture contamination.",
                "Do not mistake Foot Rot for Foot and Mouth Disease (FMD). FMD involves vesicles/blisters on mouth and hooves.",
                "Observe withdrawal periods for systemic antimicrobial treatments.",
                "Uganda MAAIF Livestock Improvement Guide; MSD Vet Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Lameness"))
        ));

        // 9. Foot and Mouth Disease (FMD)
        DISEASES.add(new DiseaseGuide(
                "fmd", "Foot and Mouth Disease (FMD)", "FMD, Aphtose Fever", "Cattle, Sheep, Goats, Pigs", "Viral Disease (Notifiable)",
                "HIGH", true, false, "Foot and Mouth Disease Virus (Aphthovirus - Serotypes O, A, SAT1, SAT2, SAT3)",
                "Highly contagious via direct contact, airborne droplets, contaminated vehicles, milk, or fodder.",
                "Unregulated livestock movement, shared communal watering points, open livestock markets.",
                "2 to 14 days.",
                Arrays.asList("High fever", "Sudden drop in milk yield", "Dullness & shivering"),
                Arrays.asList("Fluid-filled blisters (vesicles) on tongue, lips, gums, nostrils, and interdigital skin of feet", "Excessive smacking of lips and profuse ropy saliva drooling", "Severe lameness & kicking feet", "Sores on teats"),
                Arrays.asList("Sloughing of hoof wall ('shedding hooves')", "High calf mortality due to myocarditis ('tiger heart')", "Permanent emaciation"),
                Arrays.asList("Inspect mouth, tongue, and teats for open sores or blisters", "Check feet for interdigital blisters"),
                Arrays.asList("ISOLATE AFFECTED HERD IMMEDIATELY", "DO NOT MOVE ANY ANIMALS, MEAT OR MILK OFF FARM", "NOTIFY DISTRICT VETERINARY OFFICER (DVO) URGENTLY"),
                "No curative antiviral treatment. Veterinary care focuses on supportive therapy, soft green feed, disinfectant foot/mouth washes, and antibiotics for secondary bacterial infections.",
                "Epithelial tissue flap or vesicular fluid ELISA / RT-PCR test.",
                "Strict biosecurity; routine vaccination with quadrivalent/inactivated FMD vaccines as directed by MAAIF.",
                "Inactivated FMD vaccines administered bi-annually in endemic/control zones.",
                "Enforce quarantine. Disinfect vehicles entering farm with 4% Sodium Carbonate or citric acid.",
                "FMD IS A NOTIFIABLE DISEASE IN UGANDA. Illegal animal movement spreads outbreaks.",
                "Milk from affected cows must not be sold; boil or destroy milk. Follow vet quarantine instructions.",
                "Uganda MAAIF Animal Health Act; WOAH Terrestrial Code",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Reduced milk", "Lameness", "Mouth/foot blisters", "Excessive salivation"))
        ));

        // 10. Lumpy Skin Disease (LSD)
        DISEASES.add(new DiseaseGuide(
                "lsd", "Lumpy Skin Disease (LSD)", "LSD, Lumpy Skin", "Cattle", "Viral Skin Disease",
                "MEDIUM", true, false, "Lumpy Skin Disease Virus (Capripoxvirus)",
                "Mechanically transmitted by biting insects (mosquitoes, stable flies, ticks).",
                "Rainy season with high insect activity, wet marshy pastures.",
                "1 to 4 weeks.",
                Arrays.asList("High fever", "Watery eye and nasal discharge", "Reluctance to move"),
                Arrays.asList("Multiple firm, raised, round skin nodules/lumps (1–5 cm) all over body, head, neck, & udder", "Swollen legs and brisket edema", "Enlarged superficial lymph nodes", "Sores/ulcers in mouth & nose", "Severe milk drop"),
                Arrays.asList("Skin nodules becoming necrotic ('sit-fasts') leaving open deep wounds", "Severe pneumonia & emaciation"),
                Arrays.asList("Palpate skin over neck, back, and udder for firm raised lumps", "Check legs for swelling"),
                Arrays.asList("Isolate affected cattle in fly-screened or clean housing", "Apply antiseptic insect-repellent sprays to skin wounds", "Provide soft nutritious feed"),
                "Veterinary support includes antimicrobials to prevent secondary bacterial skin infections, anti-inflammatory drugs, and wound dressings.",
                "Clinical signs, skin lesion biopsy / PCR.",
                "Vector control using insecticides and repellents; annual vaccination before wet season.",
                "Attenuated Capripoxvirus vaccine (e.g. Neethling strain) provides strong protection.",
                "Isolate sick stock; restrict insect movement where feasible.",
                "Damaged hides cause severe economic loss. Vaccination is the primary control measure.",
                "Observe withdrawal periods for supportive medications prescribed.",
                "Uganda MAAIF Disease Control Guidelines; WOAH LSD Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Swollen lymph nodes", "Reduced milk", "Skin nodules"))
        ));

        // 11. Contagious Bovine Pleuropneumonia (CBPP)
        DISEASES.add(new DiseaseGuide(
                "cbpp", "Contagious Bovine Pleuropneumonia (CBPP)", "Lung Plague", "Cattle", "Bacterial Lung Disease (Notifiable)",
                "HIGH", true, false, "Mycoplasma mycoides small colony (MmmSC)",
                "Direct contact via inhaled infective respiratory droplets from coughing animals.",
                "Communal grazing, cattle trade routes, uncontrolled herd movements.",
                "3 to 8 weeks (up to 6 months).",
                Arrays.asList("Fever", "Loss of appetite", "Slight shallow cough"),
                Arrays.asList("Severe painful, labored breathing", "Extended neck and lowered head", "Elbows turned outwards", "Painful grunt on expiration or when ribs are pressed", "Violent coughing when forced to move"),
                Arrays.asList("Severe emaciation", "Fluid accumulation in brisket ('dewlap edema')", "Death from asphyxiation"),
                Arrays.asList("Observe posture (extended neck, elbows turned out)", "Press lightly between ribs to check for pain reaction"),
                Arrays.asList("ISOLATE AFFECTED CATTLE IMMEDIATELY", "REPORT TO DISTRICT VETERINARY OFFICER (DVO)", "Avoid driving or stressing animals"),
                "Treatment is officially restricted in some eradication zones because antibiotics may create chronic carrier animals ('sequestra'). Veterinary assessment is mandatory.",
                "Pleural fluid examination, CFT test, post-mortem lung marbling findings.",
                "Movement controls and quarantine; annual vaccination with T1/44 or T1-SR strain vaccines.",
                "T1/44 live attenuated vaccine administered annually in endemic zones.",
                "Strict quarantine of new animals; avoid contact with nomadic herds.",
                "CBPP IS A NOTIFIABLE DISEASE. Do not move lung-plague suspected stock.",
                "Follow official MAAIF disease control directives.",
                "Uganda MAAIF Veterinary Public Health; WOAH Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Loss of appetite", "Cough", "Nasal discharge", "Difficult breathing", "Severe weakness"))
        ));

        // 12. Blackleg
        DISEASES.add(new DiseaseGuide(
                "blackleg", "Blackleg", "Quarter Ill, Blackquarter", "Cattle, Sheep", "Clostridial Infection",
                "HIGH", false, false, "Clostridium chauvoei (spore-forming bacterium)",
                "Ingestion of bacterial spores from soil; spores lie dormant in muscle until triggered by trauma.",
                "Soil disturbance (digging, flooding), fast-growing young cattle (6–24 months) on high nutrition.",
                "1 to 5 days.",
                Arrays.asList("Sudden onset of fever", "Stiffness in one leg", "Lethargy"),
                Arrays.asList("Severe acute lameness", "Hot, painful, crepitant (crackling) swelling over heavy muscle groups (hip, shoulder, thigh)", "Swelling turns cold, painless, and dry", "High fever (41°C)"),
                Arrays.asList("Prostration", "Tremors", "Sudden death within 12–36 hours"),
                Arrays.asList("Press fingers over swollen thigh/shoulder muscle to feel for crackling gas under skin ('crepitus')"),
                Arrays.asList("Keep animal completely calm", "Contact veterinarian immediately", "Do not drag or move carcass across pasture"),
                "Veterinary treatment requires urgent high-dose Penicillin or Oxytetracycline early in the disease course, alongside surgical drainage of muscle lesions.",
                "Muscle tissue impression smear showing C. chauvoei rods; fluorescent antibody test.",
                "Annual vaccination of all young cattle aged 4–24 months before rainy season.",
                "Polyvalent Clostridial vaccines (Blackleg + Malignant Edema) highly effective.",
                "Burn or deeply bury carcasses with quicklime. Do not cut open carcass in open pasture.",
                "Blackleg spores survive in soil for decades. Burning carcasses prevents pasture contamination.",
                "Observe withdrawal periods if penicillin or tetracyclines are administered.",
                "Uganda MAAIF Field Guide; MSD Veterinary Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Lameness", "Severe weakness", "Sudden death"))
        ));

        // 13. Brucellosis
        DISEASES.add(new DiseaseGuide(
                "brucellosis", "Brucellosis", "Contagious Abortion, Bang's Disease", "Cattle, Goats, Sheep, Humans", "Bacterial Zoonosis (Notifiable)",
                "HIGH", true, true, "Brucella abortus (Cattle) / Brucella melitensis (Goats)",
                "Ingestion of bacteria from aborted fetuses, placenta, uterine discharge, or unpasteurized milk.",
                "Unscreened breeding stock, contact with aborted membranes, communal pastures.",
                "2 weeks to several months.",
                Arrays.asList("Often asymptomatic until first abortion", "Irregular estrus cycles"),
                Arrays.asList("Late-term abortion (typically between 6th and 9th month of pregnancy)", "Retained placenta and uterine infection (metritis)", "Reduced milk production", "Swollen joints (hygroma)", "Swollen testicles in bulls"),
                Arrays.asList("Repeated abortions in herd ('abortion storm')", "Permanent infertility"),
                Arrays.asList("Monitor herd abortion records", "Check aborted fetuses and placenta for leathery yellow appearance"),
                Arrays.asList("ZOONOTIC WARNING: USE RUBBER GLOVES WHEN HANDLING ABORTED FETUSES / PLACENTA", "Isolate aborting cow from herd for 30 days", "Bury aborted fetus and placenta with quicklime"),
                "No curative veterinary treatment permitted in livestock due to risk of creating carrier animals and human zoonotic exposure. Test and removal is standard policy.",
                "Rose Bengal Test (RBT), Milk Ring Test (MRT), ELISA.",
                "Vaccination of female calves; test-and-slaughter policy; strict biosecurity.",
                "S19 or RB51 live vaccine for female calves aged 4–8 months (NEVER VACCINATE ADULT COWS OR MALES).",
                "DO NOT DRINK UNPASTEURIZED MILK. Brucellosis causes Undulant Fever (Malta fever) in humans.",
                "HUMAN HEALTH RISK: Brucellosis causes chronic debilitating fever, joint pain, and sweating in humans. Always boil or pasteurize milk.",
                "Always observe local regulatory culling and disposal protocols.",
                "Uganda MAAIF & Ministry of Health Zoonotic Disease Directives; WOAH",
                "May 2026",
                new HashSet<>(Arrays.asList("Reduced milk", "Abortion", "Retained placenta"))
        ));

        // 14. Ketosis
        DISEASES.add(new DiseaseGuide(
                "ketosis", "Ketosis", "Acetonemia, Slow Fever", "High-yielding Dairy Cows", "Metabolic Disorder",
                "MEDIUM", false, false, "Negative energy balance in early lactation (glucose deficit)",
                "Inadequate energy intake during early peak lactation.",
                "High-yielding dairy cows, poor quality forage, overly fat cows at calving.",
                "1 to 4 weeks post-calving.",
                Arrays.asList("Gradual decline in appetite", "Preference for dry forage over concentrates"),
                Arrays.asList("Sweet, fruity, acetone smell on cow's breath, milk, and urine", "Rapid weight loss & hollow flanks", "Firm dry dung", "Drop in milk production", "Nervous ketosis (licking walls, chewing gate, unsteadiness)"),
                Arrays.asList("Severe emaciation", "Inability to rise", "Aggressive delirium in nervous ketosis"),
                Arrays.asList("Check breath smell for sweet acetone odor", "Use urine/milk ketone test strips"),
                Arrays.asList("Provide high-energy oral drench (Propylene glycol or Glycerol)", "Offer high-quality palatable green feed and concentrates"),
                "Veterinary management includes IV Dextrose (50%) administration, glucocorticoids, and oral glucose precursors.",
                "Urine or milk dipstick test for acetoacetate / beta-hydroxybutyrate (BHB).",
                "Maintain optimal Body Condition Score (BCS 3.0–3.5) at calving; step-up concentrate feeding post-calving.",
                "N/A (Metabolic disorder).",
                "Ensure transition diets contain adequate energy and bypass protein.",
                "Do not confuse ketosis with hardware disease or displacement of abomasum.",
                "Follow product label if veterinary glucocorticoids are administered.",
                "Dairy Cattle Metabolic Disorders Manual; MSD Vet Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Loss of appetite", "Reduced milk", "Severe weakness"))
        ));

        // 15. Milk Fever
        DISEASES.add(new DiseaseGuide(
                "milkfever", "Milk Fever", "Hypocalcaemia, Parturient Paresis", "High-yielding Dairy Cows", "Metabolic Disorder",
                "HIGH", false, false, "Acute deficiency of blood calcium at or near calving.",
                "Rapid calcium shift into colostrum faster than body bone calcium mobilization.",
                "High-producing older cows (3rd lactation+), Jersey breed, high calcium diets during dry period.",
                "Within 48 hours before or after calving.",
                Arrays.asList("Hypersensitivity", "Muscle tremors of flanks and muzzle", "Unsteady wobbly gait"),
                Arrays.asList("Cow unable to stand ('downer cow')", "Head turned back resting on flank ('S-bend' posture)", "Cold ears, skin, and teats", "Dry muzzle & glazed eyes", "Bloat and stopped rumen movements"),
                Arrays.asList("Coma", "Severe bloat", "Hypothermia and death within 12–24 hours"),
                Arrays.asList("Feel ears for coldness", "Check posture (cow sitting on sternum with head turned to flank)"),
                Arrays.asList("DO NOT DRENCH ANIMAL ORALLY WITH FLUIDS (swallowing reflex is paralyzed; risk of aspiration pneumonia)", "Keep cow upright sitting on sternum with straw bales", "Call veterinarian URGENTLY"),
                "Veterinary treatment requires slow intravenous infusion of Calcium Borogluconate (400–500 mL of 23–40% solution), monitoring heart rate carefully during administration.",
                "Clinical posture, blood serum calcium measurement (<1.5 mmol/L).",
                "Restrict calcium intake during dry period (low-calcium dry cow feed); administer oral calcium boluses at calving.",
                "N/A (Metabolic disorder).",
                "Ensure deep bedding under downer cows to prevent pressure nerve damage.",
                "IV Calcium must be infused SLOWLY over 10–15 minutes. Rapid injection can cause fatal heart arrest.",
                "N/A (Calcium Borogluconate has zero withdrawal, but verify secondary drugs).",
                "Veterinary Clinical Nutrition Manual; MSD Vet Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Loss of appetite", "Reduced milk", "Severe weakness", "Bloat"))
        ));

        // 16. Worm / Helminth Infection
        DISEASES.add(new DiseaseGuide(
                "worms", "Worm / Helminth Infection", "Internal Parasites, Gastrointestinal Nematodes", "Cattle, Sheep, Goats", "Parasitic Infection",
                "MEDIUM", false, false, "Haemonchus, Ostertagia, Cooperia, Fasciola hepatica (Liver fluke)",
                "Ingestion of infective parasite larvae from wet, contaminated pastures.",
                "Communal grazing, marshy wet pastures, overstocking, rainy season.",
                "2 to 4 weeks.",
                Arrays.asList("Slight dullness", "Rough unthrifty coat", "Slow growth rate"),
                Arrays.asList("Soft watery diarrhea / scours", "Progressive weight loss and emaciation", "'Bottle jaw' (soft fluid swelling under lower jaw)", "Pale eyes/gums (anemia with Haemonchus)", "Pot belly in young calves"),
                Arrays.asList("Extreme weakness", "Severe recumbency", "Death from severe blood loss / anemia"),
                Arrays.asList("Press lower jaw to check for fluid swelling ('bottle jaw')", "Check mucous membranes of eye for paleness"),
                Arrays.asList("Move herd to clean dry pasture if possible", "Provide high-protein feed supplements"),
                "Veterinary management involves anthelmintic deworming (Albendazole, Levamisole, Ivermectin, or Triclabendazole for fluke) selected based on targeted parasite species.",
                "Fecal egg count (FEC) / McMaster technique, liver fluke egg sedimentation.",
                "Pasture rotation; strategic deworming before and after wet season; avoid grazing marshy areas.",
                "N/A.",
                "Quarantine new stock and deworm before adding to main herd.",
                "Over-deworming creates resistant super-worms. Use targeted anthelmintic treatment based on fecal egg counts.",
                "Observe meat and milk withdrawal times specified on the anthelmintic product label.",
                "Uganda MAAIF Parasitology Guide; FAO Helminth Control Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Loss of appetite", "Diarrhoea", "Dehydration", "Pale mucous membranes", "Reduced milk", "Severe weakness"))
        ));

        // 17. Metritis
        DISEASES.add(new DiseaseGuide(
                "metritis", "Metritis / Uterine Infection", "Toxic Metritis, Endometritis", "Cattle", "Reproductive Infection",
                "MEDIUM", false, false, "Trueperella pyogenes, E. coli, Fusobacterium necrophorum",
                "Bacterial invasion of uterus following calving, retained placenta, or assisted difficult birth.",
                "Unclean calving environment, manual placenta removal, dystocia (hard calving), retained twin placenta.",
                "1 to 2 weeks post-calving.",
                Arrays.asList("Slight foul discharge from vulva", "Mild fever"),
                Arrays.asList("Foul-smelling, reddish-brown, watery discharge from vulva", "High fever (40–41°C)", "Arching back and tail raised", "Severe drop in milk production", "Loss of appetite and depression"),
                Arrays.asList("Septicemia / toxemia", "Inability to stand", "Permanent uterine scarring / sterility"),
                Arrays.asList("Inspect under tail for vulvar discharge color and odor", "Check body temperature"),
                Arrays.asList("Keep hindquarters clean", "Provide clean bedding", "Call vet for intrauterine and systemic therapy"),
                "Veterinary management includes systemic antimicrobials (e.g. Ceftiofur, Oxytetracycline), NSAIDs for fever, and gentle intrauterine antiseptic flushes where appropriate.",
                "Vaginal speculum examination, transrectal uterine palpation.",
                "Sanitary calving pens; hygienic calving assistance (disinfect hands & ropes); DO NOT pull retained placenta forcibly.",
                "N/A.",
                "Keep maternity pens thoroughly disinfected with lime/antiseptic between calvings.",
                "Forcible removal of retained placenta damages uterine lining. Allow placenta to drop naturally or seek vet care.",
                "Observe milk and meat withdrawal periods for systemic antimicrobials.",
                "Theriogenology Manual; MSD Vet Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Loss of appetite", "Reduced milk", "Retained placenta"))
        ));

        // 18. Bloat
        DISEASES.add(new DiseaseGuide(
                "bloat", "Bloat", "Ruminal Tympany", "Cattle, Sheep, Goats", "Digestive Emergency",
                "HIGH", false, false, "Frothy bloat (legumes/young clover) or Free-gas bloat (esophageal obstruction)",
                "Ingestion of lush young leguminous pastures (clover/lucerne) or choking on root crops (sweet potato, avocado).",
                "Turned out to lush wet pasture when hungry, high-concentrate low-roughage diets, choke.",
                "15 minutes to 4 hours post-feeding.",
                Arrays.asList("Slight distension of left flank", "Frequent shifting of weight"),
                Arrays.asList("Severe bulging distension of LEFT flank (higher than spine)", "Kicking at belly", "Frequent urination and defecation", "Frequent grunting and bellowing", "Rapid shallow breathing"),
                Arrays.asList("Severe gasping with open mouth and blueish tongue", "Collapse and suffocation due to pressure on lungs"),
                Arrays.asList("Observe left flank behind ribs for distension above spine line", "Press left flank to test tight drum-like tension"),
                Arrays.asList("Keep animal walking slowly; DO NOT ALLOW ANIMAL TO LIE DOWN", "Drench orally with 250–500 mL vegetable oil or anti-bloat poloxalene agent", "Pass esophageal stomach tube if gas is free", "IN CRITICAL SUFFOCATION: Emergency trocarisation in left flank by trained personnel"),
                "Veterinary intervention includes stomach tube decompression, administration of anti-frothing surfactant agents, or surgical rumenotomy in extreme choke.",
                "Left flank palpation, stomach tube passage.",
                "Feed dry hay/straw BEFORE turning cattle onto lush legume pastures; avoid grazing when dew is wet.",
                "N/A.",
                "Introduce lush green pastures gradually over 7–10 days.",
                "Emergency trocar incision into the left flank is a last-resort life-saving procedure when suffocation is imminent.",
                "N/A for vegetable oil/surfactants; check withdrawal if antibiotics administered.",
                "Uganda MAAIF Extension Service; MSD Vet Manual",
                "May 2026",
                new HashSet<>(Arrays.asList("Loss of appetite", "Difficult breathing", "Severe weakness", "Bloat"))
        ));

        // 19. Anthrax
        DISEASES.add(new DiseaseGuide(
                "anthrax", "Anthrax", "Splenic Fever, Charbon", "Cattle, Sheep, Goats, Wildlife, Humans", "Bacterial Zoonosis (Notifiable)",
                "CRITICAL", true, true, "Bacillus anthracis (spore-forming bacterium)",
                "Ingestion or inhalation of bacterial spores from contaminated soil, water, or bone meal.",
                "Flooding, drought, alkaline soil disturbance, un-vaccinated herds.",
                "1 to 14 days.",
                Arrays.asList("Often ZERO warning; sudden death is primary sign", "Fever & muscle tremors"),
                Arrays.asList("Sudden unexplained death in previously healthy animal", "Dark, tarry blood oozing from mouth, nose, anus, and vulva that DOES NOT CLOT", "Rapid decomposition of carcass", "Absence of rigor mortis (stiffening)"),
                Arrays.asList("Sudden death of multiple animals in same pasture within hours"),
                Arrays.asList("Look at carcass from distance: Check for dark blood oozing from nostrils/anus", "Check if carcass fails to stiffen"),
                Arrays.asList("DO NOT OPEN OR PERFORM POST-MORTEM ON CARCASS (Opening carcass causes bacteria to form resistant spores that contaminate soil for 50+ years)", "DO NOT CONSUME OR SELL MEAT", "Cover carcass with heavy plastic tarpaulin", "CONTACT DISTRICT VETERINARY OFFICER (DVO) IMMEDIATELY"),
                "Veterinary authorities handle anthrax outbreaks. In live suspected contacts, high-dose Penicillin or Oxytetracycline is administered under strict quarantine.",
                "Blood smear from ear vein stained with Polychrome Methylene Blue (McFadyean reaction).",
                "Annual vaccination in endemic areas; proper deep burial of carcasses with quicklime or complete incineration.",
                "Sterne strain live spore vaccine administered annually.",
                "Strict quarantine. Disinfect contaminated ground with 10% Formalin or 5% Sodium Hydroxide.",
                "ZOONOTIC & PUBLIC HEALTH WARNING: Anthrax causes fatal cutaneous, gastrointestinal, or pulmonary infection in humans. NEVER butcher a suddenly dead animal.",
                "Milk and meat from infected herds are strictly condemned under animal health regulations.",
                "Uganda MAAIF & Ministry of Health One Health Directives; WOAH",
                "May 2026",
                new HashSet<>(Arrays.asList("Fever", "Severe weakness", "Sudden death", "Bloat"))
        ));

        // 20. Rabies
        DISEASES.add(new DiseaseGuide(
                "rabies", "Rabies", "Hydrophobia, Mad Cow Disease (Lissavirus)", "Cattle, Dogs, Cats, Wildlife, Humans", "Viral Zoonosis (Notifiable)",
                "CRITICAL", true, true, "Rabies Lyssavirus",
                "Inoculation of virus via saliva from bite of infected animal (rabid dog, jackal, fox, bat).",
                "Unvaccinated stray dogs on farm, wildlife contact near forest reserves.",
                "2 weeks to several months.",
                Arrays.asList("Change in behavior", "Isolation or unusual alertness", "Slight unsteadiness"),
                Arrays.asList("Furious form: Extreme aggression, attacking inanimate objects, loud altered bellowing, biting", "Dumb form: Profuse ropy saliva drooling, inability to swallow, sagging lower jaw", "Knuckling over of hind fetlocks & progressive paralysis"),
                Arrays.asList("Coma and death within 3–7 days after onset of clinical signs"),
                Arrays.asList("Observe animal safely behind barrier: Check for drooling saliva, jaw paralysis, and abnormal bellowing"),
                Arrays.asList("DO NOT PUT HANDS INSIDE COW'S MOUTH (frequently mistaken for choke / object stuck in throat)", "ISOLATE ANIMAL IN SECURE ENCLOSURE", "Contact District Veterinary Officer & Medical Doctor immediately if human contact occurred"),
                "No curative veterinary treatment once clinical signs appear. Euthanasia is recommended under official supervision.",
                "Brain tissue Fluorescent Antibody Test (FAT) post-mortem.",
                "Mass vaccination of all farm dogs and cats; fence farm perimeter.",
                "Inactivated cell-culture rabies vaccine for livestock and domestic dogs.",
                "Quarantine animal securely. Disinfect contaminated areas with bleach / quaternary ammonium.",
                "HUMAN LIFE THREAT: Rabies is 100% fatal in humans once symptoms develop. Any human bitten or exposed to saliva must receive immediate Post-Exposure Prophylaxis (PEP) rabies vaccine.",
                "Milk and meat from rabid animals strictly condemned.",
                "Uganda Ministry of Health & MAAIF Zoonotic Guidelines; WOAH",
                "May 2026",
                new HashSet<>(Arrays.asList("Excessive salivation", "Severe weakness", "Sudden death"))
        ));
    }

    public static List<DiseaseGuide> getAllDiseases() {
        return Collections.unmodifiableList(DISEASES);
    }

    public static List<DiseaseGuide> getEmergencyDiseases() {
        List<DiseaseGuide> list = new ArrayList<>();
        for (DiseaseGuide d : DISEASES) {
            if ("CRITICAL".equalsIgnoreCase(d.urgency) || d.notifiable) {
                list.add(d);
            }
        }
        return list;
    }

    public static DiseaseGuide getById(String id) {
        if (id == null) return null;
        for (DiseaseGuide d : DISEASES) {
            if (d.id.equalsIgnoreCase(id) || d.name.toLowerCase().contains(id.toLowerCase())) {
                return d;
            }
        }
        return null;
    }

    public static List<SymptomMatch> evaluateSymptoms(Set<String> selectedSymptoms) {
        List<SymptomMatch> matches = new ArrayList<>();
        if (selectedSymptoms == null || selectedSymptoms.isEmpty()) {
            return matches;
        }

        for (DiseaseGuide d : DISEASES) {
            List<String> matching = new ArrayList<>();
            for (String symptom : selectedSymptoms) {
                if (d.matchedSymptoms.contains(symptom)) {
                    matching.add(symptom);
                }
            }
            if (!matching.isEmpty()) {
                matches.add(new SymptomMatch(d, matching.size(), matching));
            }
        }

        // Sort descending by number of matching symptoms
        Collections.sort(matches, (a, b) -> Integer.compare(b.matchedCount, a.matchedCount));
        return matches;
    }

    public static String[] ALL_FARMER_SYMPTOMS = new String[]{
            "Fever",
            "Loss of appetite",
            "Swollen lymph nodes",
            "Cough",
            "Nasal discharge",
            "Difficult breathing",
            "Diarrhoea",
            "Dehydration",
            "Pale mucous membranes",
            "Yellow mucous membranes",
            "Dark/red urine",
            "Reduced milk",
            "Abnormal milk",
            "Hot/swollen udder",
            "Lameness",
            "Mouth/foot blisters",
            "Excessive salivation",
            "Skin nodules",
            "Abortion",
            "Retained placenta",
            "Severe weakness",
            "Sudden death",
            "Bloat"
    };

    private VeterinaryKnowledgeBase() {}
}
