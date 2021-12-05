package com.aspicereporting.entity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("TABLE")
public class TableItem extends ReportItem{
}
