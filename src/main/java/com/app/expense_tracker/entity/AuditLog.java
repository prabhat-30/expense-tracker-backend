package com.app.expense_tracker.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter

@Entity
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String action;

    private String details;

    private LocalDateTime timestamp;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    public AuditLog() {
    }

    public AuditLog(String username,
                    String action,
                    String details) {

        this.username = username;
        this.action = action;
        this.details = details;
    }
    @PrePersist
    public void setTimestamp(){
        this.timestamp=LocalDateTime.now();
    }
}