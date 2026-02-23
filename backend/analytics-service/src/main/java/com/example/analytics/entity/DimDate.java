package com.example.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "dim_date_table", schema = "analytics_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimDate {

    @Id
    @Column(name = "date_key")
    private Integer dateKey;

    @Column(name = "full_date", nullable = false)
    private LocalDate fullDate;

    private Integer day;
    private Integer month;

    @Column(name = "month_name")
    private String monthName;

    private Integer quarter;
    private Integer year;
    private Integer week;

    @Column(name = "is_weekend")
    private Boolean isWeekend;

    @Column(name = "fiscal_year")
    private Integer fiscalYear;
}
