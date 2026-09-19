package com.aerialfishingtracker;

public enum TenchScope
{
    SESSION("This session"),
    LIFETIME("Lifetime catches");

    private final String label;

    TenchScope(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
