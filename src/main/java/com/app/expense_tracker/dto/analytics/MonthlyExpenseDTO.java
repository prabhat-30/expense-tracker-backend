package com.app.expense_tracker.dto.analytics;

public class MonthlyExpenseDTO {
    private Integer month;
    private Double total;

    public MonthlyExpenseDTO(Integer month,Double total){

        this.month=month;
        this.total=total;
    }

    public Integer getMonth() {
        return month;
    }

    public Double getTotal() {
        return total;
    }
}
