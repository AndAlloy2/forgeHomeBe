package com.forge.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "home")
    private Home home;
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

}
