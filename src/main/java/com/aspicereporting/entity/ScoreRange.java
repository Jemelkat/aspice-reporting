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
    private Double pMinus;
    private Double pPlus;
    private Double lMinus;
    private Double lPlus;

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
        pMinus = null;
        pPlus = null;
        lMinus = null;
        lPlus = null;
    }

    private void normalize() {
        if(mode.equals(Mode.SIMPLE)) {
            this.n = n/100;
            this.p = p/100;
            this.l = l/100;
        }
        else {
            this.n = n/100;
            this.pMinus = pMinus/100;
            this.pPlus = pPlus/100;
            this.lMinus = lMinus/100;
            this.lPlus = lPlus/100;
        }
    }

    public ScoreRange createPercentagesObject() {
        ScoreRange percentageRange = new ScoreRange();
        if(this.mode.equals(Mode.SIMPLE)) {
            percentageRange.setMode(Mode.SIMPLE);
            percentageRange.setN(this.n*100D);
            percentageRange.setP(this.p*100D);
            percentageRange.setL(this.l*100D);
        }
        else {
            percentageRange.setMode(Mode.EXTENDED);
            percentageRange.setN(this.n*100D);
            percentageRange.setPMinus(this.pMinus*100D);
            percentageRange.setPPlus(this.pPlus*100D);
            percentageRange.setLMinus(this.lMinus*100D);
            percentageRange.setLPlus(this.lPlus*100D);
        }
        return percentageRange;
    }

    public void updateRanges(ScoreRange scoreRange) {
        scoreRange.normalize();
        if(scoreRange.mode.equals(Mode.SIMPLE)) {
            this.pMinus = null;
            this.pPlus = null;
            this.lMinus = null;
            this.lPlus = null;

            this.mode= Mode.SIMPLE;
            this.n = scoreRange.getN();
            this.p = scoreRange.getP();
            this.l = scoreRange.getL();
        }
        else {
            this.p = null;
            this.l = null;

            this.mode= Mode.EXTENDED;
            this.n = scoreRange.getN();
            this.pMinus = scoreRange.getPMinus();
            this.pPlus = scoreRange.getPPlus();
            this.lMinus = scoreRange.getLMinus();
            this.lPlus = scoreRange.getLPlus();
        }
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
            if (n >= 100D) {
                throw new InvalidDataException("N score range value bigger than 100.");
            }
            if (p >= 100D) {
                throw new InvalidDataException("P score range value bigger than 100.");
            }
            if (l >= 100D) {
                throw new InvalidDataException("L score range value bigger than 100.");
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
            if (pMinus == null) {
                throw new InvalidDataException("P- score range value not defined.");
            }
            if (pPlus == null) {
                throw new InvalidDataException("P+ score range value not defined.");
            }
            if (lMinus == null) {
                throw new InvalidDataException("L- score range value not defined.");
            }
            if (lPlus == null) {
                throw new InvalidDataException("L+ score range value not defined.");
            }
            if (n >= 100D) {
                throw new InvalidDataException("N score range value bigger than 100.");
            }
            if (pMinus >= 100D) {
                throw new InvalidDataException("P- score range value bigger than 100.");
            }
            if (pPlus >= 100D) {
                throw new InvalidDataException("P+ score range value bigger than 100.");
            }
            if (lMinus >= 100D) {
                throw new InvalidDataException("L- score range value bigger than 100.");
            }
            if (n > pMinus) {
                throw new InvalidDataException("P- score range value (" + pMinus + ") must be bigger than upper N value (" + n + ")");
            }
            if (pMinus > pPlus) {
                throw new InvalidDataException("P+ score range value (" + pPlus + ") must be bigger than upper P- value (" + pMinus + ")");
            }
            if (pPlus > lMinus) {
                throw new InvalidDataException("L- score range value (" + lMinus + ") must be bigger than upper P+ value (" + pPlus + ")");
            }
            if (lMinus > lPlus) {
                throw new InvalidDataException("L+ score range value (" + lPlus + ") must be bigger than upper L- value (" + lMinus + ")");
            }
        } else {
            throw new InvalidDataException("Score ranges must be SIMPLE or EXTENDED. Provided: " + mode);
        }
    }
}
