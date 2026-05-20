package com.app.expense_tracker.dto.analytics;

public class YearlyExpenseDTO {
    private Integer year;
    private Double total;

    public YearlyExpenseDTO(Integer year,Double total){

        this.year=year;
        this.total=total;
    }

    public Integer getYear() {
        return year;
    }

    public Double getTotal() {
        return total;
    }
}
