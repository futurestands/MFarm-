package dev.mfarm.com.mfarm.intelligence;

import java.util.ArrayList;
import java.util.List;

public class FarmBrief {
    private final String greeting;
    private final List<String> attentionItems;
    private final List<String> goodNewsItems;
    private final List<String> comingUpItems;

    public FarmBrief(String greeting, List<String> attentionItems, List<String> goodNewsItems, List<String> comingUpItems) {
        this.greeting = greeting != null ? greeting : "Hello 👋";
        this.attentionItems = attentionItems != null ? attentionItems : new ArrayList<String>();
        this.goodNewsItems = goodNewsItems != null ? goodNewsItems : new ArrayList<String>();
        this.comingUpItems = comingUpItems != null ? comingUpItems : new ArrayList<String>();
    }

    public String getGreeting() {
        return greeting;
    }

    public List<String> getAttentionItems() {
        return attentionItems;
    }

    public List<String> getGoodNewsItems() {
        return goodNewsItems;
    }

    public List<String> getComingUpItems() {
        return comingUpItems;
    }

    public boolean isEmpty() {
        return attentionItems.isEmpty() && goodNewsItems.isEmpty() && comingUpItems.isEmpty();
    }
}
