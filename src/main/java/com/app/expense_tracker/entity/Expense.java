package com.app.expense_tracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor // Automatically creates an empty constructor
@AllArgsConstructor // Automatically includes 'type' in the constructor
@Entity
@Table(name="expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate date;
    private String title;
    private Double amount;
    private String category;
    // Add this field to your Expense.java entity
    @Column(nullable = false)
    private String type; // values: "INCOME" or "EXPENSE"

    @JsonProperty("recurring")
    private boolean recurring;

    @JsonProperty("frequency")
    private String frequency;

    @JsonProperty("nextDate")
    private LocalDate nextDate;

    @JsonProperty("includeSat")
    private boolean includeSat;

    @JsonProperty("includeSun")
    private boolean includeSun;

}