package com.aspicereporting.entity;

import com.aspicereporting.entity.enums.Mode;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.exception.InvalidDataException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@JsonView(View.Simple.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ScoreRange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "range_id")
    private Long id;

    private Double n;
    private Double pminus;
    private Double pplus;
    private Double lminus;
    private Double lplus;

    private Double p;
    private Double l;
    @Column(length = 50, name = "mode")
    @Enumerated(EnumType.STRING)
    private Mode mode;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", unique = true)
    private Source source;

    public void initialize() {
        mode= Mode.SIMPLE;
        n = 0.15D;
        p = 0.50D;
        l = 0.85D;
        pminus = null;
        pplus = null;
        lminus = null;
        lplus = null;
    }

    private void normalize() {
        if(mode.equals(Mode.SIMPLE)) {
            this.n = n/100;
            this.p = p/100;
            this.l = l/100;
        }
        else {
            this.n = n/100;
            this.pminus = pminus /100;
            this.pplus = pplus /100;
            this.lminus = lminus /100;
            this.lplus = lplus /100;
        }
    }

    public ScoreRange createPercentagesObject() {
        ScoreRange percentageRange = new ScoreRange();
        if(this.mode.equals(Mode.SIMPLE)) {
            percentageRange.setMode(Mode.SIMPLE);
            percentageRange.setN(new BigDecimal(this.n*100).setScale(2, RoundingMode.HALF_UP).doubleValue());
            percentageRange.setP(new BigDecimal(this.p*100).setScale(2, RoundingMode.HALF_UP).doubleValue());
            percentageRange.setL(new BigDecimal(this.l*100).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }
        else {
            new BigDecimal(this.n*100).setScale(2, RoundingMode.HALF_UP).doubleValue();

            percentageRange.setMode(Mode.EXTENDED);
            percentageRange.setN(new BigDecimal(this.n*100).setScale(2, RoundingMode.HALF_UP).doubleValue());
            percentageRange.setPminus(new BigDecimal(this.pminus *100).setScale(2, RoundingMode.HALF_UP).doubleValue());
            percentageRange.setPplus(new BigDecimal(this.pplus *100).setScale(2, RoundingMode.HALF_UP).doubleValue());
            percentageRange.setLminus(new BigDecimal(this.lminus *100).setScale(2, RoundingMode.HALF_UP).doubleValue());
            percentageRange.setLplus(new BigDecimal(this.lplus *100).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }
        return percentageRange;
    }

    public void updateRanges(ScoreRange scoreRange) {
        if(scoreRange.mode.equals(Mode.SIMPLE)) {
            this.pminus = null;
            this.pplus = null;
            this.lminus = null;
            this.lplus = null;

            this.mode= Mode.SIMPLE;
            this.n = new BigDecimal(scoreRange.getN()).setScale(2, RoundingMode.HALF_UP).doubleValue();
            this.p = new BigDecimal(scoreRange.getP()).setScale(2, RoundingMode.HALF_UP).doubleValue();
            this.l = new BigDecimal(scoreRange.getL()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        else {
            this.p = null;
            this.l = null;

            this.mode= Mode.EXTENDED;
            this.n = new BigDecimal(scoreRange.getN()).setScale(2, RoundingMode.HALF_UP).doubleValue();
            this.pminus = new BigDecimal(scoreRange.getPminus()).setScale(2, RoundingMode.HALF_UP).doubleValue();
            this.pplus = new BigDecimal(scoreRange.getPplus()).setScale(2, RoundingMode.HALF_UP).doubleValue();
            this.lminus = new BigDecimal(scoreRange.getLminus()).setScale(2, RoundingMode.HALF_UP).doubleValue();
            this.lplus = new BigDecimal(scoreRange.getLplus()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        this.normalize();
    }

    public void validate() {
        if(mode == null) {
            throw new InvalidDataException("Score ranges must be SIMPLE or EXTENDED. Provided: " + mode);
        }
        if (mode.equals(Mode.SIMPLE)) {
            if (n == null) {
                throw new InvalidDataException("N score range value not defined.");
            }
            if (p == null) {
                throw new InvalidDataException("P score range value not defined.");
            }
            if (l == null) {
                throw new InvalidDataException("L score range value not defined.");
            }
            if (n > 99.99) {
                throw new InvalidDataException("N score range value must be smaller than 100.");
            }
            if (p > 99.99) {
                throw new InvalidDataException("P score range value must be smaller than 100.");
            }
            if (l > 99.99) {
                throw new InvalidDataException("L score range value must be smaller than 100.");
            }
            if (n > p) {
                throw new InvalidDataException("P score range value (" + p + ") must be bigger than upper N value (" + n + ")");
            }
            if (p > l) {
                throw new InvalidDataException("L score range value (" + l + ") must be bigger than upper P value (" + p + ")");
            }
        }
        else if (mode.equals(Mode.EXTENDED)) {
            if (n == null) {
                throw new InvalidDataException("N score range value not defined.");
            }
            if (pminus == null) {
                throw new InvalidDataException("P- score range value not defined.");
            }
            if (pplus == null) {
                throw new InvalidDataException("P+ score range value not defined.");
            }
            if (lminus == null) {
                throw new InvalidDataException("L- score range value not defined.");
            }
            if (lplus == null) {
                throw new InvalidDataException("L+ score range value not defined.");
            }
            if (n > 99.99) {
                throw new InvalidDataException("N score range value must be smaller than 100.");
            }
            if (pminus > 99.99) {
                throw new InvalidDataException("P- score range value must be smaller than 100.");
            }
            if (pplus > 99.99) {
                throw new InvalidDataException("P+ score range value must be smaller than 100.");
            }
            if (lminus > 99.99) {
                throw new InvalidDataException("L- score range value must be smaller than 100.");
            }
            if (lplus > 99.99) {
                throw new InvalidDataException("L+ score range value must be smaller than 100.");
            }
            if (n > pminus) {
                throw new InvalidDataException("P- score range value (" + pminus + ") must be bigger than upper N value (" + n + ")");
            }
            if (pminus > pplus) {
                throw new InvalidDataException("P+ score range value (" + pplus + ") must be bigger than upper P- value (" + pminus + ")");
            }
            if (pplus > lminus) {
                throw new InvalidDataException("L- score range value (" + lminus + ") must be bigger than upper P+ value (" + pplus + ")");
            }
            if (lminus > lplus) {
                throw new InvalidDataException("L+ score range value (" + lplus + ") must be bigger than upper L- value (" + lminus + ")");
            }
        } else {
            throw new InvalidDataException("Score ranges must be SIMPLE or EXTENDED. Provided: " + mode);
        }
    }
}
