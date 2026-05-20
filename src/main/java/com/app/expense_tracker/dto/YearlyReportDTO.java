package com.app.expense_tracker.dto;

import lombok.*;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YearlyReportDTO {
    private int year;
    private Double totalExpense;
    private Map<String,Double> monthlySummary;
    private Map<String,Double> categorySummary;

}
