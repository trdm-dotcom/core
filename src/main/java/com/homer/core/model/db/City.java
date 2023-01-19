package com.homer.core.model.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

@Entity
@Getter
@Setter
@Table(name = "t_city")
@NoArgsConstructor
@AllArgsConstructor
public class City {
    @Id
    @JsonProperty
    private Long id;
    @JsonProperty
    private String name;
    @JsonProperty
    private String codeName;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "city")
    @JsonIgnore
    private Collection<Post> posts = new ArrayList<>();
}
