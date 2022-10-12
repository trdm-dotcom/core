package com.homer.core.model.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Table(name = "t_districts")
@NoArgsConstructor
@AllArgsConstructor
public class District {
    @Id
    private Long code;
    private String name;
    private String codeName;
    private String cityCode;
}
