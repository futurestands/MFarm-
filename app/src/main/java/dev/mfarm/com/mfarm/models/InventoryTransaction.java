package dev.mfarm.com.mfarm.models;

public class InventoryTransaction {
    private int id;
    private int itemId;
    private String type; // Stock In / Stock Out
    private double quantity;
    private String date;
    private String remarks;

    public InventoryTransaction() {
    }

    public InventoryTransaction(int id, int itemId, String type, double quantity, String date, String remarks) {
        this.id = id;
        this.itemId = itemId;
        this.type = type;
        this.quantity = quantity;
        this.date = date;
        this.remarks = remarks;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
