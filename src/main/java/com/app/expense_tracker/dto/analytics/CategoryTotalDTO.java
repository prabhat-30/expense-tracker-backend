package com.app.expense_tracker.dto.analytics;

public class CategoryTotalDTO {
    private String category;
    private Double total;

    public CategoryTotalDTO(String category,Double total){
        this.category=category;
        this.total=total;
    }
    public String getCategory(){
        return category;
    }
    public Double getTotal(){
        return total;
    }
}
