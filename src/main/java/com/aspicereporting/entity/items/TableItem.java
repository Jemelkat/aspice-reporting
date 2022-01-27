package com.aspicereporting.entity.items;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("TABLE")
public class TableItem extends ReportItem {
    @Override
    public void update() {

    }
}
