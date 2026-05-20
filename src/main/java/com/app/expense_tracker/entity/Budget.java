package com.app.expense_tracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;
    private Double limitAmount;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Getter and Setter
    @Column(name = "alert_state", length = 30)
    private String alertState;

}