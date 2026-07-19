package com.woojung.jerusalemmix.model;

public final class EqBand {
    public int frequencyTenthHz;
    public int gainHundredthDb;
    public int qThousandths;
    public boolean bypassed;
    public String type = "Bell";

    public EqBand(int frequencyTenthHz, int gainHundredthDb, int qThousandths) {
        this.frequencyTenthHz = frequencyTenthHz;
        this.gainHundredthDb = gainHundredthDb;
        this.qThousandths = qThousandths;
    }
}
