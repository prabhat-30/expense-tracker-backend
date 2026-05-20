package com.app.expense_tracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

@Getter
@Setter
public class ExpenseDTO {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    @NotBlank(message = "Category is required")
    private String category;

    //@NotNull(message = "Date is required so today's date is set")
    private LocalDate date;

    // Add this field to your ExpenseDTO
    @NotBlank(message = "Type is required")
    private String type;

    //@NotNull(message = "Id is required")
    //private int id;
    @JsonProperty("recurring")
    private boolean recurring; // Removed the "is" prefix entirely

    @JsonProperty("frequency")
    private String frequency;

    @JsonProperty("nextDate")
    private LocalDate nextDate;

    @JsonProperty("includeSat")
    private boolean includeSat;

    @JsonProperty("includeSun")
    private boolean includeSun;

    @JsonProperty("isUndo")
    private boolean isUndo;
}