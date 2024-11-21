package com.forge.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Data
@Entity
public class Home {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @OneToMany(mappedBy = "home", cascade = CascadeType.ALL)
    private Set<User> users;
    @OneToMany(mappedBy = "home", cascade = CascadeType.ALL)
    private Set<Device> devices;
}
