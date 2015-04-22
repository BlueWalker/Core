package walker.blue.core.lib.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import walker.blue.path.lib.node.GridNode;

/**
 * Table designed to store GridNode objects representing destinations
 */
public class DestinationTable {

    /**
     * Nested maps holding the gata
     */
    private Map<DestinationType, Object> table;

    /**
     * Contructor. Initializes entries for all the DestinationTypes
     */
    public DestinationTable() {
        this.table = new HashMap<>();
        for (final DestinationType type : DestinationType.values()) {
            if (type.isGeneric()) {
                this.table.put(type, new HashSet<GridNode>());
            } else {
                this.table.put(type, new HashMap<String, GridNode>());
            }
        }
    }

    /**
     * Get a non generic value using the given type and key
     *
     * @param type Type of the value
     * @param key Key for the value
     * @return valu corresponding to the given type and key
     */
    public GridNode getNonGeneric(final DestinationType type, final String key) {
        if (type.isGeneric()) {
            return null;
        } else {
            return (GridNode) this.getValue(type, key);
        }
    }

    /**
     * Get all values for the given non-generic type
     *
     * @param type Type of the values
     * @return All values for the given non-generic type
     */
    public Set<Map.Entry<String, GridNode>> getAllNonGeneric(final DestinationType type) {
        if (type.isGeneric()) {
            return null;
        } else {
            return ((HashMap<String, GridNode>) this.getValue(type, null)).entrySet();
        }
    }

    /**
     * Get all generic value of given type
     *
     * @param type Type of the values
     * @return All generic value correspoinging to the given type
     */
    public Set<GridNode> getGeneric(final DestinationType type) {
        if (type.isGeneric()) {
            return (Set<GridNode>) this.getValue(type, null);
        } else {
            return null;
        }
    }

    /**
     * Get raw object for the given type
     *
     * @param type Type of the value
     * @return Raw object corresponding tot he given type
     */
    public Object getImmediateValue(final DestinationType type) {
        return this.getValue(type, null);
    }

    /**
     * Add value with the given type and key
     *
     * @param type Type of the value being added
     * @param key Key for the value being added
     * @param newVal Value being added
     */
    public void addValue(final DestinationType type, final String key, final GridNode newVal) {
        if (newVal != null && type.isGeneric()) {
            ((Set<GridNode>) this.table.get(type)).add(newVal);
        } else if (newVal != null && key != null) {
            ((Map<String, GridNode>) this.table.get(type)).put(key, newVal);
        }
    }

    /**
     * Check whether the table is empty
     *
     * @return boolean indicating whether the table is empty
     */
    public boolean isEmpty() {
        return this.table.isEmpty();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DestinationTable that = (DestinationTable) o;

        if (table != null ? !table.equals(that.table) : that.table != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return table != null ? table.hashCode() : 0;

    }

    /**
     * Get value with the given Type and key
     *
     * @param type Type of the value
     * @param key Key for the value
     * @return Object corresponding to the given tpy and key
     */
    private Object getValue(final DestinationType type, final String key) {
        final Object value = this.table.get(type);
        if (type.isGeneric() || key == null) {
            return value;
        } else {
            return ((HashMap<String, GridNode>) value).get(key);
        }
    }
}
