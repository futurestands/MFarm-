package dev.mfarm.com.mfarm.intelligence;

public class Insight implements Comparable<Insight> {

    public enum Type {
        VACCINATION,
        MILK,
        FEED,
        CALVING,
        HEALTH,
        FINANCIAL,
        DATA_QUALITY
    }

    public enum Priority {
        CRITICAL(1),
        ATTENTION(2),
        INFO(3),
        POSITIVE(4);

        private final int level;

        Priority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    private final Type type;
    private final Priority priority;
    private final String title;
    private final String message;
    private final String relatedAnimalId;
    private final String relatedAnimalName;
    private final String actionTarget;

    public Insight(Type type, Priority priority, String title, String message,
                   String relatedAnimalId, String relatedAnimalName, String actionTarget) {
        this.type = type;
        this.priority = priority;
        this.title = title;
        this.message = message;
        this.relatedAnimalId = relatedAnimalId;
        this.relatedAnimalName = relatedAnimalName;
        this.actionTarget = actionTarget;
    }

    public Type getType() {
        return type;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getRelatedAnimalId() {
        return relatedAnimalId;
    }

    public String getRelatedAnimalName() {
        return relatedAnimalName;
    }

    public String getActionTarget() {
        return actionTarget;
    }

    @Override
    public int compareTo(Insight other) {
        if (other == null) return -1;
        return Integer.compare(this.priority.getLevel(), other.priority.getLevel());
    }
}
