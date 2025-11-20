package com.domu.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "staff")
@Getter
@Setter
@JsonIgnoreProperties({"community", "tasks", "shifts"})
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(nullable = false, length = 120)
    private String firstName;

    @Column(nullable = false, length = 120)
    private String lastName;

    @Column(nullable = false, unique = true, length = 20)
    private String rut;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false, length = 120)
    private String position;

    private boolean active = true;

    @OneToMany(mappedBy = "assignee")
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "staff")
    private List<Shift> shifts = new ArrayList<>();
}
