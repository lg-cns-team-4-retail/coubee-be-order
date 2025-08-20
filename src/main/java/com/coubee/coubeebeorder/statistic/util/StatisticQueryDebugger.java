package com.coubee.coubeebeorder.statistic.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;

/**
 * Utility class for debugging PostgreSQL type mismatch issues in statistics queries
 */
@Slf4j
@Component
public class StatisticQueryDebugger {

    private final JdbcTemplate jdbcTemplate;

    public StatisticQueryDebugger(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Debug method to test parameter types and SQL execution
     */
    public void debugParameterTypes(LocalDate startDate, LocalDate endDate, Long storeId) {
        log.info("=== PostgreSQL Parameter Type Debug ===");
        
        // Convert dates to UNIX timestamps
        long startUnix = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = endDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);
        
        log.info("Input Parameters:");
        log.info("  startDate: {} (LocalDate)", startDate);
        log.info("  endDate: {} (LocalDate)", endDate);
        log.info("  storeId: {} ({})", storeId, storeId != null ? storeId.getClass().getSimpleName() : "null");
        
        log.info("Converted Parameters:");
        log.info("  startUnix: {} (long)", startUnix);
        log.info("  endUnix: {} (long)", endUnix);
        
        // Test basic order query
        testOrderQuery(startUnix, endUnix, storeId);
        
        // Test item count query
        testItemCountQuery(startUnix, endUnix, storeId);
        
        log.info("=== Debug Complete ===");
    }

    private void testOrderQuery(long startUnix, long endUnix, Long storeId) {
        log.info("Testing Order Query...");
        
        String sql = """
            SELECT
                COALESCE(SUM(o.total_amount), 0) as total_sales_amount,
                COUNT(o.order_id) as total_order_count,
                COUNT(DISTINCT o.user_id) as unique_customer_count
            FROM orders o
            WHERE o.status = ?
            AND o.paid_at_unix BETWEEN ? AND ?
            """ + (storeId != null ? "AND o.store_id = ? " : "");
        
        Object[] params = storeId != null 
            ? new Object[]{"RECEIVED", startUnix, endUnix, storeId.longValue()}
            : new Object[]{"RECEIVED", startUnix, endUnix};
        
        log.info("SQL: {}", sql);
        log.info("Parameters: {}", Arrays.toString(params));
        
        try {
            jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                long totalSales = rs.getLong("total_sales_amount");
                int totalOrders = rs.getInt("total_order_count");
                int uniqueCustomers = rs.getInt("unique_customer_count");
                
                log.info("✅ Order Query Success - Sales: {}, Orders: {}, Customers: {}", 
                        totalSales, totalOrders, uniqueCustomers);
                return null;
            }, params);
        } catch (Exception e) {
            log.error("❌ Order Query Failed: {}", e.getMessage());
        }
    }

    private void testItemCountQuery(long startUnix, long endUnix, Long storeId) {
        log.info("Testing Item Count Query...");
        
        String sql = """
            SELECT COALESCE(SUM(oi.quantity), 0) as total_item_count
            FROM order_items oi
            WHERE EXISTS (
                SELECT 1 FROM orders o
                WHERE o.order_id = oi.order_id
                AND o.status = ?
                AND o.paid_at_unix BETWEEN ? AND ?
            """ + (storeId != null ? "    AND o.store_id = ? " : "") + ")";
        
        Object[] params = storeId != null 
            ? new Object[]{"RECEIVED", startUnix, endUnix, storeId.longValue()}
            : new Object[]{"RECEIVED", startUnix, endUnix};
        
        log.info("SQL: {}", sql);
        log.info("Parameters: {}", Arrays.toString(params));
        
        try {
            Integer result = jdbcTemplate.queryForObject(sql, Integer.class, params);
            log.info("✅ Item Count Query Success - Items: {}", result);
        } catch (Exception e) {
            log.error("❌ Item Count Query Failed: {}", e.getMessage());
            
            // Additional debugging for type mismatch
            if (e.getMessage().contains("operator does not exist")) {
                log.error("🔍 Type Mismatch Detected!");
                log.error("   This usually means parameter types don't match column types");
                log.error("   Check: orders.order_id (varchar) vs order_items.order_id (varchar)");
                log.error("   Check: orders.store_id (bigint) vs parameter type");
                log.error("   Check: orders.status (varchar) vs parameter type");
            }
        }
    }

    /**
     * Verify database schema types
     */
    public void verifySchemaTypes() {
        log.info("=== Database Schema Verification ===");
        
        try {
            // Check orders table schema
            String ordersSql = """
                SELECT column_name, data_type, character_maximum_length, numeric_precision
                FROM information_schema.columns 
                WHERE table_schema = 'coubee_order' 
                AND table_name = 'orders'
                AND column_name IN ('order_id', 'store_id', 'status', 'paid_at_unix')
                ORDER BY column_name
                """;
            
            log.info("Orders table columns:");
            jdbcTemplate.query(ordersSql, (rs, rowNum) -> {
                String columnName = rs.getString("column_name");
                String dataType = rs.getString("data_type");
                Integer maxLength = rs.getObject("character_maximum_length", Integer.class);
                Integer precision = rs.getObject("numeric_precision", Integer.class);
                
                log.info("  {}: {} {} {}", columnName, dataType, 
                        maxLength != null ? "(" + maxLength + ")" : "",
                        precision != null ? "precision=" + precision : "");
                return null;
            });
            
            // Check order_items table schema
            String itemsSql = """
                SELECT column_name, data_type, character_maximum_length, numeric_precision
                FROM information_schema.columns 
                WHERE table_schema = 'coubee_order' 
                AND table_name = 'order_items'
                AND column_name IN ('order_id', 'product_id', 'quantity')
                ORDER BY column_name
                """;
            
            log.info("Order_items table columns:");
            jdbcTemplate.query(itemsSql, (rs, rowNum) -> {
                String columnName = rs.getString("column_name");
                String dataType = rs.getString("data_type");
                Integer maxLength = rs.getObject("character_maximum_length", Integer.class);
                Integer precision = rs.getObject("numeric_precision", Integer.class);
                
                log.info("  {}: {} {} {}", columnName, dataType, 
                        maxLength != null ? "(" + maxLength + ")" : "",
                        precision != null ? "precision=" + precision : "");
                return null;
            });
            
        } catch (Exception e) {
            log.error("Schema verification failed: {}", e.getMessage());
        }
        
        log.info("=== Schema Verification Complete ===");
    }

    /**
     * Test with sample data to ensure queries work
     */
    public void testWithSampleData() {
        log.info("=== Sample Data Test ===");
        
        LocalDate testDate = LocalDate.now();
        Long testStoreId = 1L;
        
        debugParameterTypes(testDate, testDate, testStoreId);
        debugParameterTypes(testDate, testDate, null);
        
        log.info("=== Sample Data Test Complete ===");
    }
}
