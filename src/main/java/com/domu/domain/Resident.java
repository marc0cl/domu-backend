package com.domu.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "residents", uniqueConstraints = {
        @UniqueConstraint(name = "uix_resident_rut", columnNames = {"rut"})
})
@Getter
@Setter
@JsonIgnoreProperties({"community", "unit", "visits", "deliveries", "reservations", "votes", "threads",
        "posts", "tickets", "ticketUpdates", "payments", "notifications", "parkingPermits", "roles"})
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(nullable = false, length = 120)
    private String firstName;

    @Column(nullable = false, length = 120)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false, length = 20)
    private String rut;

    private boolean owner;

    private boolean active = true;

    @OneToMany(mappedBy = "resident")
    private List<Visit> visits = new ArrayList<>();

    @OneToMany(mappedBy = "resident")
    private List<Delivery> deliveries = new ArrayList<>();

    @OneToMany(mappedBy = "resident")
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "resident")
    private List<Vote> votes = new ArrayList<>();

    @OneToMany(mappedBy = "author")
    private List<ForumThread> threads = new ArrayList<>();

    @OneToMany(mappedBy = "author")
    private List<ForumPost> posts = new ArrayList<>();

    @OneToMany(mappedBy = "reporter")
    private List<Ticket> tickets = new ArrayList<>();

    @OneToMany(mappedBy = "author")
    private List<TicketUpdate> ticketUpdates = new ArrayList<>();

    @OneToMany(mappedBy = "resident")
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "recipient")
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "resident")
    private List<ParkingPermit> parkingPermits = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "resident_roles",
            joinColumns = @JoinColumn(name = "resident_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
}
