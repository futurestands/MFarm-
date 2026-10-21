package dev.mfarm.com.mfarm.dao;

import android.database.Cursor;

public final class SpeciesCatalog {

    public static class SpeciesInfo {
        public final String name;
        public final int durationDays;
        public final boolean isIncubation; // true for egg-laying poultry
        public final String birthTerm;     // Calving, Kidding, Lambing, Kindling, Hatching
        public final String offspringTerm; // Calf, Kid, Lamb, Kit, Chick
        public final String femaleTerm;    // Cow, Doe, Ewe, Hen
        public final String maleTerm;      // Bull, Buck, Ram, Rooster

        public SpeciesInfo(String name, int durationDays, boolean isIncubation, String birthTerm, String offspringTerm, String femaleTerm, String maleTerm) {
            this.name = name;
            this.durationDays = durationDays;
            this.isIncubation = isIncubation;
            this.birthTerm = birthTerm;
            this.offspringTerm = offspringTerm;
            this.femaleTerm = femaleTerm;
            this.maleTerm = maleTerm;
        }
    }

    private SpeciesCatalog() {}

    public static SpeciesInfo getSpeciesInfo(DatabaseAdapter db, String speciesName) {
        if (speciesName == null || speciesName.trim().isEmpty()) {
            speciesName = "Cattle";
        }
        String clean = speciesName.trim();

        if (db != null) {
            Cursor c = db.query("SELECT gestation_days, birth_term, offspring_term, female_term, male_term FROM species_catalog WHERE LOWER(species_name) = LOWER(?)", new String[]{clean});
            if (c != null && c.moveToFirst()) {
                int days = c.getInt(0);
                String birth = c.getString(1);
                String offspring = c.getString(2);
                String female = c.getString(3);
                String male = c.getString(4);
                c.close();

                boolean isInc = "Poultry".equalsIgnoreCase(clean) || "Hatching".equalsIgnoreCase(birth);
                return new SpeciesInfo(clean, days, isInc, birth, offspring, female, male);
            }
            if (c != null) c.close();
        }

        // Fallback defaults
        if ("Goat".equalsIgnoreCase(clean)) return new SpeciesInfo("Goat", 150, false, "Kidding", "Kid", "Doe", "Buck");
        if ("Sheep".equalsIgnoreCase(clean)) return new SpeciesInfo("Sheep", 147, false, "Lambing", "Lamb", "Ewe", "Ram");
        if ("Rabbit".equalsIgnoreCase(clean)) return new SpeciesInfo("Rabbit", 31, false, "Kindling", "Kit", "Doe", "Buck");
        if ("Poultry".equalsIgnoreCase(clean)) return new SpeciesInfo("Poultry", 21, true, "Hatching", "Chick", "Hen", "Rooster");
        return new SpeciesInfo("Cattle", 283, false, "Calving", "Calf", "Cow", "Bull");
    }
}
