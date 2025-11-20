package com.domu.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "units", uniqueConstraints = {
        @UniqueConstraint(name = "uix_building_unit_number", columnNames = {"building_id", "number"})
})
@Getter
@Setter
@JsonIgnoreProperties({"building", "residents", "expenseStatements", "parkingSpaces"})
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(nullable = false, length = 50)
    private String number;

    @Column(length = 50)
    private String floor;

    private BigDecimal areaM2;

    @Column(nullable = false, length = 50)
    private String status = "occupied";

    @OneToMany(mappedBy = "unit")
    private List<Resident> residents = new ArrayList<>();

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseStatement> expenseStatements = new ArrayList<>();

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParkingSpace> parkingSpaces = new ArrayList<>();
}
