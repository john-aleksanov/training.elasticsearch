package dev.marvel.elastic.apiclient;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class Employee {

    private String id;
    private String name;

    @JsonProperty("dob")
    private LocalDate dateOfBirth;
    private Address address;
    private String email;
    private Set<String> skills;
    private int experience;
    private double rating;
    private String description;

    @JsonProperty("verified")
    private boolean isVerified;
    private int salary;

    @Data
    private static class Address {
        private String country;
        private String town;
    }
}
