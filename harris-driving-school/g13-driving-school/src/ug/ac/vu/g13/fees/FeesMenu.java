package ug.ac.vu.g13.fees;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ug.ac.vu.g13.core.AbstractModuleMenu;
import ug.ac.vu.g13.core.InputHelper;
import ug.ac.vu.g13.core.Menu;
import ug.ac.vu.g13.core.Payable;
import ug.ac.vu.g13.core.Reportable;
import ug.ac.vu.g13.core.Validate;
import ug.ac.vu.g13.courses.Enrollment;
import ug.ac.vu.g13.courses.EnrollmentService;

/**
 * Submenu for the Fees module.
 *
 * @author Darwin 
 */
public class FeesMenu extends AbstractModuleMenu {
    private final InvoiceService invoices;
    private final PaymentService payments;
    private final EnrollmentService enrollments;

    public FeesMenu(InvoiceService invoices, PaymentService payments, EnrollmentService enrollments) {
        this.invoices = invoices;
        this.payments = payments;
        this.enrollments = enrollments;
    }

    @Override
    public String getName() {
        return "Fees and Payments";
    }

    @Override
    protected String[] options() {
        return new String[] {
            "Issue an invoice for an enrolment", "List invoices", "Change an invoice due date",
            "Remove an invoice", "Record a payment", "List payments", "Reverse (remove) a payment",
            "Report: fees (sorted / filtered)", "Overview of invoices and payments"
        };
    }

    @Override
    protected void handle(int choice) throws Exception {
        switch (choice) {
            case 1: issue(); break;
            case 2: Menu.printList("Invoices", invoices.sorted(InvoiceService.BY_DUE_DATE)); break;
            case 3: dueDate(); break;
            case 4: removeInvoice(); break;
            case 5: pay(); break;
            case 6: Menu.printList("Payments", payments.sorted(PaymentService.BY_DATE)); break;
            case 7: reverse(); break;
            case 8: report(); break;
            case 9: overview(); break;
            default: break;
        }
    }

    private void issue() {
        Enrollment e = enrollments.require(InputHelper.readRequired("Enrolment ID (e.g. G13-E001): "));
        LocalDate due = InputHelper.readDate("Due date");
        Invoice inv = invoices.issue(e, due);
        Menu.ok("Invoice " + inv.getId() + " for " + Validate.money(inv.getTotalDue()) + " issued to "
                + e.getStudent().getFullName() + ".");
    }

    private void dueDate() {
        String id = InputHelper.readRequired("Invoice ID (e.g. G13-N001): ");
        invoices.require(id);
        LocalDate d = InputHelper.readDate("New due date");
        invoices.update(id, i -> i.setDueDate(d));
        Menu.ok("Due date changed.");
    }

    private void removeInvoice() {
        String id = InputHelper.readRequired("Invoice ID to remove: ");
        invoices.require(id);
        if (InputHelper.readYesNo("Remove invoice " + id + "?")) {
            invoices.remove(id);
            Menu.ok("Invoice removed.");
        }
    }

    private void pay() throws OverpaymentException {
        Invoice inv = invoices.require(InputHelper.readRequired("Invoice ID: "));
        System.out.println("  Balance: " + Validate.money(inv.getBalance()));
        double amount = InputHelper.readDouble("Amount paid in UGX: ", 500, 10_000_000);
        Payment.Method m = InputHelper.readEnum("Payment method: ", Payment.Method.values());
        String ref = InputHelper.readLine("Reference / receipt number (optional, invented): ");
        Payment p = payments.record(inv, amount, m, ref);
        Menu.ok("Payment " + p.getId() + " recorded. New balance: " + Validate.money(inv.getBalance()));
    }

    private void reverse() {
        String id = InputHelper.readRequired("Payment ID (e.g. G13-P001): ");
        payments.require(id);
        if (InputHelper.readYesNo("Reverse payment " + id + "?")) {
            payments.remove(id);
            Menu.ok("Payment reversed.");
        }
    }

    private void report() {
        int how = InputHelper.readChoice("Report: ", "Biggest unpaid balances first", "Overdue invoices",
                "Payments by amount (largest first)", "Payments by one method", "Total collected and outstanding");
        if (how == 0) {
            List<Invoice> list = invoices.filter(i -> !i.isSettled());
            list.sort(InvoiceService.BY_BALANCE_DESC);
            Menu.printList("Unpaid balances", list);
        } else if (how == 1) {
            List<Invoice> list = invoices.filter(Invoice::isOverdue);
            list.sort(InvoiceService.BY_DUE_DATE);
            Menu.printList("Overdue invoices", list);
        } else if (how == 2) {
            Menu.printList("Payments by amount", payments.sorted(PaymentService.BY_AMOUNT_DESC));
        } else if (how == 3) {
            Payment.Method m = InputHelper.readEnum("Method: ", Payment.Method.values());
            List<Payment> list = payments.filter(p -> p.getMethod() == m);
            list.sort(PaymentService.BY_DATE);
            Menu.printList("Payments by " + m, list);
        } else {
            double outstanding = 0;
            for (Payable p : invoices.getAll()) {
                outstanding += p.getBalance();
            }
            System.out.println("  Total collected:   " + Validate.money(payments.totalCollected()));
            System.out.println("  Total outstanding: " + Validate.money(outstanding));
        }
    }

    /** Polymorphism: one loop over invoices and payments through Reportable. */
    private void overview() {
        List<Reportable> mixed = new ArrayList<>(invoices.sorted(InvoiceService.BY_DUE_DATE));
        mixed.addAll(payments.sorted(PaymentService.BY_DATE));
        System.out.println("--- Invoices and payments (" + mixed.size() + ") ---");
        for (Reportable r : mixed) {
            System.out.println("  " + r.reportLine());
        }
    }
}
