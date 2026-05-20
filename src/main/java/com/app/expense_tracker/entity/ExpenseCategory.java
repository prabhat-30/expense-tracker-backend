package com.app.expense_tracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "expense_categories")
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name", unique = true, nullable = false)
    private String name;

    @Column(name = "display_icon")
    private String icon; // Stores emojis like 🍔, 🚗, 🛒

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // Constructors
    public ExpenseCategory() {}

    public ExpenseCategory(String name, String icon, boolean active) {
        this.name = name;
        this.icon = icon;
        this.active = active;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}