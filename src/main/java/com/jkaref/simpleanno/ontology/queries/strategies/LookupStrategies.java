package com.jkaref.simpleanno.ontology.queries.strategies;

/**
 * @author Matthias Muenzner <matthias.muenzner@jkaref.com>
 */
public enum LookupStrategies {

    UNKNOWN(0),
    CHILD(1),
    PARENT(2),
    GRANDPARENT(3),
    UNCLE(4);

    LookupStrategies(int value) {
        this.value = value;
    }

    public static LookupStrategies fromValue(int val) {
        for (LookupStrategies v : values())
            if (v.getValue() == val)
                return v;
        return UNKNOWN;
    }

    public static LookupStrategies fromValue(String val) {
        int v;

        try {
            v = Integer.parseInt(val);
        } catch (NumberFormatException nfe) {
            v = UNKNOWN.value;
        }

        return fromValue(v);
    }

    public int getValue() {
        return value;
    }

    private int value;
}
