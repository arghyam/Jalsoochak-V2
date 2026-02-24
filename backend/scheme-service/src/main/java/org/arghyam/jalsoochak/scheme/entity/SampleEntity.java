package org.arghyam.jalsoochak.scheme.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scheme_master_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_name")
    private String schemeName;

    @Column(name = "scheme_code")
    private String schemeCode;

    @Column(name = "channel")
    private Integer channel;
}
