package com.lldprep.systems.vendingmachine.exception;

import com.lldprep.systems.vendingmachine.model.enums.ProductCode;

/**
 * Thrown when selected product slot is empty.
 */
public class ProductOutOfStockException extends VendingMachineException {
    private final ProductCode productCode;

    public ProductOutOfStockException(ProductCode productCode) {
        super(String.format("Product at slot %s is out of stock", productCode));
        this.productCode = productCode;
    }

    public ProductCode getProductCode() {
        return productCode;
    }
}
