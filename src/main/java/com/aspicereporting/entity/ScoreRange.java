package com.aspicereporting.entity;

import com.aspicereporting.entity.items.TextItem;
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

    private Float n;
    private Float pMinus;
    private Float pPlus;
    private Float lMinus;
    private Float lPlus;

    private Float p;
    private Float l;
    @Column(length = 50, name = "mode", updatable = false)
    @Enumerated(EnumType.STRING)
    private EMode mode;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", unique = true)
    private Source source;

    private enum EMode {
        SIMPLE, EXTENDED
    }

    public void initialize() {
        mode= EMode.EXTENDED;
        n = 15F;
        pMinus = 32.5F;
        pPlus = 50F;
        lMinus = 67.5F;
        lPlus = 85F;
    }

    public void updateRanges(ScoreRange scoreRange) {
        if(scoreRange.mode.equals(EMode.SIMPLE)) {
            this.pMinus = null;
            this.pPlus = null;
            this.lMinus = null;
            this.lPlus = null;

            this.mode= EMode.SIMPLE;
            this.n = scoreRange.getN();
            this.p = scoreRange.getP();
            this.l = scoreRange.getL();
        }
        else {
            this.p = null;
            this.l = null;

            this.mode= EMode.EXTENDED;
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
        if (mode.equals(EMode.SIMPLE)) {
            if (n == null) {
                throw new InvalidDataException("N score range value not defined.");
            }
            if (p == null) {
                throw new InvalidDataException("P score range value not defined.");
            }
            if (l == null) {
                throw new InvalidDataException("L score range value not defined.");
            }
            if (n >= 100) {
                throw new InvalidDataException("N score range value bigger than 100.");
            }
            if (p >= 100) {
                throw new InvalidDataException("P score range value bigger than 100.");
            }
            if (l >= 100) {
                throw new InvalidDataException("L score range value bigger than 100.");
            }
            if (n > p) {
                throw new InvalidDataException("P score range value (" + p + ") must be bigger than upper N value (" + n + ")");
            }
            if (p > l) {
                throw new InvalidDataException("L score range value (" + l + ") must be bigger than upper P value (" + p + ")");
            }
        }
        else if (mode.equals(EMode.EXTENDED)) {
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
            if (n >= 100) {
                throw new InvalidDataException("N score range value bigger than 100.");
            }
            if (pMinus >= 100) {
                throw new InvalidDataException("P- score range value bigger than 100.");
            }
            if (pPlus >= 100) {
                throw new InvalidDataException("P+ score range value bigger than 100.");
            }
            if (lMinus >= 100) {
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
