package ug.ac.vu.g13.fees;

import java.time.LocalDate;

import ug.ac.vu.g13.core.BaseRecord;
import ug.ac.vu.g13.core.Fields;
import ug.ac.vu.g13.core.Validate;

/**
 * One payment made towards an invoice.
 *
 * @author Darwin
 */
public class Payment extends BaseRecord {
    public enum Method { CASH, MOBILE_MONEY, BANK }

    private final Invoice invoice;
    private final double amount;
    private final LocalDate paidOn;
    private final Method method;
    private final String reference;

    public Payment(String id, Invoice invoice, double amount, LocalDate paidOn, Method method, String reference) {
        super(id);
        this.invoice = Validate.notNull(invoice, "Invoice");
        this.amount = Validate.doubleRange(amount, "Payment amount (UGX)", 500, 10_000_000);
        this.paidOn = Validate.date(paidOn, "Payment date", LocalDate.of(2000, 1, 1), LocalDate.now().plusDays(1));
        this.method = Validate.notNull(method, "Payment method");
        this.reference = reference == null ? "" : Validate.text(reference, "Reference", 0, 30);
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public Method getMethod() {
        return method;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public String describe() {
        return String.format("%s | Payment | %s | %s | %s | %s%s", getId(), invoice.getId(),
                invoice.getEnrollment().getStudent().getFullName(), Validate.money(amount), paidOn,
                " | " + method + (reference.isEmpty() ? "" : " ref " + reference));
    }

    @Override
    public String toFileLine() {
        return Fields.join(getId(), invoice.getId(), amount, paidOn, method.name(), reference);
    }
}
