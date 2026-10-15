package ug.ac.vu.g13.fees;

import java.time.LocalDate;
import java.util.Comparator;

import ug.ac.vu.g13.core.BaseService;
import ug.ac.vu.g13.core.Fields;

/**
 * Manages payments and keeps each invoice's paid amount up to date.
 *
 * @author Darwin
 */
public class PaymentService extends BaseService<Payment> {
    public static final Comparator<Payment> BY_DATE = Comparator.comparing(Payment::getPaidOn).thenComparing(Payment::getId);
    public static final Comparator<Payment> BY_AMOUNT_DESC = Comparator.comparingDouble(Payment::getAmount).reversed();

    private final InvoiceService invoices;

    public PaymentService(InvoiceService invoices) {
        super("payments.txt", "Payment");
        this.invoices = invoices;
        load();
        for (Payment p : getAll()) {
            p.getInvoice().addPaid(p.getAmount());
        }
    }

    @Override
    protected Payment fromFileLine(String line) {
        String[] f = Fields.split(line, 6);
        return new Payment(f[0], invoices.require(f[1]), Double.parseDouble(f[2]), LocalDate.parse(f[3]),
                Payment.Method.valueOf(f[4]), f[5]);
    }

    public Payment record(Invoice invoice, double amount, Payment.Method method, String reference)
            throws OverpaymentException {
        if (amount > invoice.getBalance() + 0.0001) {
            throw new OverpaymentException(invoice, amount);
        }
        Payment p = new Payment(nextId('P'), invoice, amount, LocalDate.now(), method, reference);
        add(p);
        invoice.addPaid(amount);
        return p;
    }

    /** Removing (reversing) a payment gives the money back to the invoice balance. */
    @Override
    public Payment remove(String id) {
        Payment p = super.remove(id);
        p.getInvoice().reducePaid(p.getAmount());
        return p;
    }

    public double totalCollected() {
        double total = 0;
        for (Payment p : getAll()) {
            total += p.getAmount();
        }
        return total;
    }
}
