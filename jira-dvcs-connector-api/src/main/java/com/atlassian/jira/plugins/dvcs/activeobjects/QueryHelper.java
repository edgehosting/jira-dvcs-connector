package com.atlassian.jira.plugins.dvcs.activeobjects;

/**
 * Contains helper utilities, useful for query building.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface QueryHelper
{

    /**
     * Represents single element of order clause.
     * 
     * @see QueryHelper#getOrder(OrderClause...)
     * @author Stanislav Dvorscak
     * 
     */
    public class OrderClause
    {

        /**
         * Type of order - Ascending vs. Descending.
         * 
         * @author Stanislav Dvorscak
         * 
         */
        public enum Order
        {
            ASC, DESC
        }

        /**
         * @see #getColumn()
         */
        private final String column;

        /**
         * @see #getOrder()
         */
        private final Order order;

        /**
         * Constructor.
         * 
         * @param column
         *            {@link #getColumn()}
         * @param order
         *            {@link #getOrder()}
         */
        public OrderClause(String column, Order order)
        {
            this.column = column;
            this.order = order;
        }

        /**
         * @return AO name of column.
         */
        public String getColumn()
        {
            return column;
        }

        /**
         * @return Type of order - ascending/descending.
         */
        public Order getOrder()
        {
            return order;
        }
    }

    /**
     * @param plainTableName
     * @return transforms plain table name into full SQL table name - escaped and extended by schema prefix
     */
    String getSqlTableName(String plainTableName);

    /**
     * @param plainColumnName
     * @return transforms plain column name into full SQL column name - escaped, ...
     */
    String getSqlColumnName(String plainColumnName);

    /**
     * @param orderClause
     *            collection of order parts
     * @return order
     */
    String getOrder(OrderClause... orderClause);

}
