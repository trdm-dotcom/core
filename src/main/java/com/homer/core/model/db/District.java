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
@Table(name = "t_district")
@NoArgsConstructor
@AllArgsConstructor
public class District {
    @Id
    @JsonProperty
    private Long id;
    @JsonProperty
    private String name;
    @JsonProperty
    private String codeName;
    @JsonProperty
    private Long cityId;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "district")
    @JsonIgnore
    private Collection<Post> posts = new ArrayList<>();
}
