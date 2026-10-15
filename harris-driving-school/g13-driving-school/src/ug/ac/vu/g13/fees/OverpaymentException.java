package ug.ac.vu.g13.fees;

import ug.ac.vu.g13.core.Validate;

/**
 * Thrown when a payment is larger than the balance still owed on the invoice.
 *
 * @author Darwin
 */
public class OverpaymentException extends Exception {
    public OverpaymentException(Invoice invoice, double amount) {
        super("Payment of " + Validate.money(amount) + " is more than the balance of "
                + Validate.money(invoice.getBalance()) + " on invoice " + invoice.getId() + ".");
    }
}
