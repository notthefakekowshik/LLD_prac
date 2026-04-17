package com.lldprep.orderbook.model;

/**
 * Immutable record of a single fill event produced by the matching engine.
 *
 * <p>One {@code MatchResult} is emitted per match iteration. If an incoming order
 * is partially filled against multiple resting orders, multiple results are fired.
 */
public class MatchResult {

    private final String symbol;
    private final String buyOrderId;
    private final String sellOrderId;
    private final double matchedPrice;   // price of the resting (passive) order
    private final int    matchedQty;

    public MatchResult(String symbol, String buyOrderId, String sellOrderId,
                       double matchedPrice, int matchedQty) {
        this.symbol       = symbol;
        this.buyOrderId   = buyOrderId;
        this.sellOrderId  = sellOrderId;
        this.matchedPrice = matchedPrice;
        this.matchedQty   = matchedQty;
    }

    public String getSymbol()       { return symbol; }
    public String getBuyOrderId()   { return buyOrderId; }
    public String getSellOrderId()  { return sellOrderId; }
    public double getMatchedPrice() { return matchedPrice; }
    public int    getMatchedQty()   { return matchedQty; }

    @Override
    public String toString() {
        return String.format("TRADE  symbol=%-6s  price=%-8.2f  qty=%-5d  buyId=%.8s  sellId=%.8s",
                symbol, matchedPrice, matchedQty, buyOrderId, sellOrderId);
    }
}
