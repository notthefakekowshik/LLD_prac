package com.lldprep.systems.orderbook.model;

/**
 * Immutable record of a single fill event produced by the matching engine.
 *
 * <p>One {@code MatchResult} is emitted per match iteration. If an incoming order
 * is partially filled against multiple resting orders, multiple results are fired.
 *
 * <p><b>WARNING: Using {@code double} for monetary values is problematic</b>
 * <ul>
 *   <li>Floating-point cannot represent decimals exactly (e.g., 0.1 + 0.2 = 0.30000000000000004)</li>
 *   <li>Accumulated rounding errors in averaging/summation</li>
 *   <li>Equality checks fail unexpectedly (0.10 might not equal 0.1)</li>
 * </ul>
 *
 * <p><b>Better alternatives for production:</b>
 * <table border="1">
 *   <tr><th>Approach</th><th>Precision</th><th>Performance</th><th>Use case</th></tr>
 *   <tr><td>{@code long} (cents)</td><td>Exact</td><td>Fastest</td><td>Fixed precision (stocks)</td></tr>
 *   <tr><td>{@link java.math.BigDecimal}</td><td>Exact</td><td>Slowest</td><td>Variable precision (FX)</td></tr>
 * </table>
 *
 * <p>Example: $150.50 stored as {@code long price = 15050L;} // cents
 */
public class MatchResult {

    private final String symbol;
    private final String buyOrderId;
    private final String sellOrderId;
    private final long matchedPrice;     // price in cents (e.g., $150.50 = 15050L) for exact precision
    private final int    matchedQty;

    public MatchResult(String symbol, String buyOrderId, String sellOrderId,
                       long matchedPrice, int matchedQty) {
        this.symbol       = symbol;
        this.buyOrderId   = buyOrderId;
        this.sellOrderId  = sellOrderId;
        this.matchedPrice = matchedPrice;
        this.matchedQty   = matchedQty;
    }

    public String getSymbol()       { return symbol; }
    public String getBuyOrderId()   { return buyOrderId; }
    public String getSellOrderId()  { return sellOrderId; }
    public long getMatchedPrice() { return matchedPrice; }

    /** Returns price as dollars for display (e.g., 15050L → 150.50) */
    public double getMatchedPriceDollars() { return matchedPrice / 100.0; }
    public int    getMatchedQty()   { return matchedQty; }

    @Override
    public String toString() {
        return String.format("TRADE  symbol=%-6s  price=%-8.2f  qty=%-5d  buyId=%.8s  sellId=%.8s",
                symbol, getMatchedPriceDollars(), matchedQty, buyOrderId, sellOrderId);
    }
}
