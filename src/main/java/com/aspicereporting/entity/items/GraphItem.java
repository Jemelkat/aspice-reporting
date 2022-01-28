package com.aspicereporting.entity.items;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("GRAPH")
public class GraphItem extends ReportItem {
}
