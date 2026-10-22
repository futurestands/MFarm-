package dev.mfarm.com.mfarm.sync;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class TemplateManager {

    private static final String TAG = "TemplateManager";

    private TemplateManager() {}

    public static File getTemplateDirectory(Context context) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "MFarm_Templates");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static Map<String, File> generateAllTemplates(Context context) {
        Map<String, File> files = new HashMap<>();
        File dir = getTemplateDirectory(context);

        files.put("Individual Livestock", generateFile(dir, "1_Individual_Livestock_Template.csv", getIndividualLivestockCsv()));
        files.put("Livestock Batches", generateFile(dir, "2_Livestock_Batches_Template.csv", getLivestockBatchesCsv()));
        files.put("Breeding & Births", generateFile(dir, "3_Breeding_and_Births_Template.csv", getBreedingAndBirthsCsv()));
        files.put("Vaccination & Health", generateFile(dir, "4_Vaccination_and_Health_Template.csv", getVaccinationAndHealthCsv()));
        files.put("Offtake & Disposals", generateFile(dir, "5_Offtake_and_Disposals_Template.csv", getOfftakeAndDisposalsCsv()));
        files.put("Crops & Harvests", generateFile(dir, "6_Crops_and_Harvests_Template.csv", getCropsAndHarvestsCsv()));
        files.put("Finances", generateFile(dir, "7_Finances_Income_Expenses_Template.csv", getFinancesCsv()));
        files.put("Egg Production", generateFile(dir, "8_Egg_Production_Template.csv", getEggProductionCsv()));

        return files;
    }

    private static File generateFile(File dir, String filename, String content) {
        File f = new File(dir, filename);
        try (FileOutputStream fos = new FileOutputStream(f);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            osw.write(content);
            osw.flush();
        } catch (Exception e) {
            Log.e(TAG, "Error writing template file " + filename, e);
        }
        return f;
    }

    private static String getIndividualLivestockCsv() {
        return "# MFarm Template: Individual Livestock Records (Cattle, Goats, Sheep, Rabbits)\n"
                + "# Version: 1.2 | Required: tag_no, species, gender\n"
                + "tag_no,species,breed,gender,dob,dam_id,sire_id,origin,color,location_name,notes\n"
                + "GT-323-2,Goat,Kalahari Red,Female,12-07-2021,8,3,Born in this herd,Brown and white,Kicwamba,Large frame\n"
                + "CW-101,Cattle,Friesian,Female,10-05-2020,CW-080,CW-002,Bought from Levi,Black and white,Main Farm,Good milk yield\n";
    }

    private static String getLivestockBatchesCsv() {
        return "# MFarm Template: Group & Batch Livestock (Poultry Chicks, Broilers, Layers, Rabbit Batches)\n"
                + "# Version: 1.2 | Required: batch_id, species, subtype, initial_quantity, acquisition_date\n"
                + "batch_id,species,subtype,breed_or_strain,purpose,initial_quantity,acquisition_date,age_weeks,supplier,location_name,purchase_cost,project_code,notes\n"
                + "BATCH-20260929-001,Poultry,Day-Old Chicks,Kuroiler,Layers,500,29-09-2026,1,Biyinzika Hatchery,Main Farm,1500000,PL4,Coop 1 flock\n"
                + "BATCH-20260929-002,Rabbit,Growers,New Zealand White,Meat,30,29-09-2026,8,Local Farm,Kicwamba,300000,RB5,Hutch B group\n";
    }

    private static String getBreedingAndBirthsCsv() {
        return "# MFarm Template: Breeding & Birth Records (Gestation/Incubation)\n"
                + "# Version: 1.2 | Required: female_tag, male_tag, mating_date\n"
                + "female_tag,male_tag,mating_date,expected_birth_date,weight_at_mating,pregnancy_confirmed,notes\n"
                + "GT-323-2,3,04-06-2026,04-11-2026,45.0,Yes,Expected 150-day goat kidding\n";
    }

    private static String getVaccinationAndHealthCsv() {
        return "# MFarm Template: Vaccination & Healthcare Protocols\n"
                + "# Version: 1.2 | Required: vaccine_name, scheduled_date\n"
                + "animal_tag_or_batch,disease_name,vaccine_name,scheduled_date,dosage,frequency,remarks\n"
                + "Herd-Level,PPR,PPR Vaccine,01-09-2026,2ml,Annually,Caprine annual vaccination\n";
    }

    private static String getOfftakeAndDisposalsCsv() {
        return "# MFarm Template: Animal Offtake & Disposals (Sales, Mortality, Culling)\n"
                + "# Version: 1.2 | Required: tag_no_or_batch, disposal_date, method\n"
                + "tag_no_or_batch,disposal_date,sex,method,cause_or_reason,price,buyer,notes\n"
                + "GT-608-9,15-06-2026,Female,Death,Dog attack,0,N/A,Mortality log\n"
                + "GT-359-6,10-10-2026,Female,Sold,Market sale,250000,Local Buyer,Live goat sale\n";
    }

    private static String getCropsAndHarvestsCsv() {
        return "# MFarm Template: Crop & Enterprise Activity Logs (Coffee, Groundnuts, Maize)\n"
                + "# Version: 1.2 | Required: crop_type, activity_type, activity_date\n"
                + "plot_name,crop_type,activity_type,activity_date,inputs_used,quantity,cost,yield_harvested,notes\n"
                + "Kicwamba Block A,Coffee,Spraying,13-06-2026,Striker Spray,2,45000,0,CF3 Coffee Enterprise\n"
                + "Main Field B,Coffee,Harvesting,05-07-2026,Labor,0,0,120,Coffee harvest 120kg\n";
    }

    private static String getFinancesCsv() {
        return "# MFarm Template: Income & Expense Financial Ledger\n"
                + "# Version: 1.2 | Required: type (Income/Expense), date, amount, project_code\n"
                + "type,date,project_code,description,amount,payment_method,buyer_or_payee,notes\n"
                + "Expense,13-06-2026,GT2,PPR Vaccine Purchase,15000,Cash,Vet Shop,Goat enterprise vaccine\n"
                + "Income,05-07-2026,CF3,Coffee Sales,120000,Mobile Money,Trader,Coffee sale 120kg\n";
    }

    private static String getEggProductionCsv() {
        return "# MFarm Template: Daily Egg Production Logs\n"
                + "# Version: 1.2 | Required: batch_id, log_date, eggs_collected\n"
                + "batch_id,log_date,eggs_collected,broken_eggs,notes\n"
                + "BATCH-20260929-001,29-09-2026,85,2,Morning collection\n";
    }
}
