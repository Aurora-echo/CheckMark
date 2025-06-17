package com.example.checkmark.main_list;

public class CheckItem {
    private int id;
    private String text;
    private boolean isCompleted;

    public CheckItem(int id,String text, boolean isCompleted) {
        this.id = id;
        this.text = text;
        this.isCompleted = isCompleted;
    }

    public int getId() {return id;}

    public String getText() {
        return text;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
